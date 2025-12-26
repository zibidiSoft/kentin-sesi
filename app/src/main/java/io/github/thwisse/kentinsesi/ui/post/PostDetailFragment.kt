package io.github.thwisse.kentinsesi.ui.post

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.data.model.PostStatus
import io.github.thwisse.kentinsesi.databinding.FragmentPostDetailBinding
import io.github.thwisse.kentinsesi.util.Resource
import io.github.thwisse.kentinsesi.util.ValidationUtils
import androidx.core.view.isVisible
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle

@AndroidEntryPoint
class PostDetailFragment : Fragment(io.github.thwisse.kentinsesi.R.layout.fragment_post_detail), OnMapReadyCallback {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    // ViewModel Tanımı
    private val viewModel: PostDetailViewModel by viewModels()
    private val commentAdapter = CommentAdapter()

    private var postLocation: LatLng? = null
    private var currentPostId: String? = null
    private var currentPost: Post? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailBinding.bind(view)

        // State restore stratejisi:
        // 1. ViewModel'den dene (eğer ViewModel hala varsa - process death olmadıysa)
        // 2. Arguments'tan postId al ve çek (ilk yükleme)
        // 3. savedInstanceState'tan post ID al ve tekrar çek (process death durumunda)
        val viewModelPost = viewModel.currentPost.value
        val argumentPostId = arguments?.getString("postId")
        val savedPostId = savedInstanceState?.getString("saved_post_id")
        
        val postIdToLoad = viewModelPost?.id ?: argumentPostId ?: savedPostId
        
        when {
            // En iyi durum: ViewModel'de post var (process death olmadı)
            viewModelPost != null -> {
                loadPost(viewModelPost)
            }
            // İkinci ve üçüncü durum: Post ID var, tekrar çekmemiz gerekiyor
            postIdToLoad != null -> {
                currentPostId = postIdToLoad
                // Post'u yükle
                viewModel.loadPostById(postIdToLoad)
                // ViewModel'den post'u observe et
                observePostFromViewModel()
            }
            else -> {
                // Hiçbir şey yok, geri git
                findNavController().navigateUp()
            }
        }

        observeOwnerActions()
    }
    
    /**
     * Post'u yükle ve UI'ı kur
     */
    private fun loadPost(post: Post) {
        currentPost = post
        currentPostId = post.id
        
        // ViewModel'e post bilgisini set et
        viewModel.setPost(post)
        
        setupViews(post)
        setupMap(post)
        setupComments()
        setupOfficialActions()
        
        // Menü kurulumu
        setupOwnerMenu()

        // Yorumları Çek (sadece ilk yüklemede)
        viewModel.getComments(post.id)
    }
    
    /**
     * ViewModel'den post'u observe et (process death sonrası restore için)
     */
    private fun observePostFromViewModel() {
        viewModel.currentPost.observe(viewLifecycleOwner) { post ->
            post?.let {
                if (currentPost == null) {
                    // İlk kez yükleniyor - Post restore edildi
                    loadPost(it)
                    // Yorumları da yükle
                    viewModel.getComments(it.id)
                } else {
                    // Post zaten yüklü, sadece güncelle (status değişikliği gibi)
                    currentPost = it
                    setupViews(it)
                }
            }
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Post'u direkt kaydetme (GeoPoint Parcelable değil, crash veriyor)
        // Sadece post ID'sini kaydet, restore ederken tekrar çekeceğiz
        currentPostId?.let {
            outState.putString("saved_post_id", it)
        }
    }

    private fun setupOwnerMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(io.github.thwisse.kentinsesi.R.menu.menu_post_detail, menu)
                
                // Başlangıçta menü öğelerini gizle, LiveData güncellendiğinde göster
                menu.findItem(io.github.thwisse.kentinsesi.R.id.action_resolve)?.isVisible = false
                menu.findItem(io.github.thwisse.kentinsesi.R.id.action_delete)?.isVisible = false
                
                // Silme kontrolü - Post sahibi veya admin silebilir
                viewModel.canDeletePost.observe(viewLifecycleOwner) { canDelete ->
                    menu.findItem(io.github.thwisse.kentinsesi.R.id.action_delete)?.isVisible = canDelete
                }
                
                // Çözüldü olarak işaretle - Yetkili kullanıcılar veya post sahibi yapabilir
                viewModel.canUpdateStatus.observe(viewLifecycleOwner) { canUpdate ->
                    menu.findItem(io.github.thwisse.kentinsesi.R.id.action_resolve)?.isVisible = canUpdate
                }
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                return when (menuItem.itemId) {
                    io.github.thwisse.kentinsesi.R.id.action_resolve -> {
                        currentPostId?.let { viewModel.markAsResolved(it) }
                        true
                    }
                    io.github.thwisse.kentinsesi.R.id.action_delete -> {
                        currentPostId?.let { viewModel.deletePost(it) }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeOwnerActions() {
        viewModel.deletePostState.observe(viewLifecycleOwner) { resource ->
            when(resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Bildirim silindi.", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is Resource.Error -> Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                is Resource.Loading -> { }
            }
        }

        viewModel.updateStatusState.observe(viewLifecycleOwner) { resource ->
            when(resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Durum güncellendi: Çözüldü!", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                is Resource.Loading -> { }
            }
        }
    }

    private fun setupViews(post: Post) {
        binding.apply {
            tvDetailTitle.text = post.title
            tvDetailDescription.text = post.description
            tvDetailCategory.text = post.category
            tvDetailDistrict.text = post.district ?: "İlçe Yok"
            ivDetailImage.load(post.imageUrl) { crossfade(true) }
        }
    }

    // ... setupMap, setupComments, onMapReady ve onDestroyView AYNI KALIYOR ...
    // Sadece yer kaplamasın diye tekrar yazmıyorum, o kısımlarda değişiklik yok.

    private fun setupMap(post: Post) {
        if (post.location != null) {
            postLocation = LatLng(post.location.latitude, post.location.longitude)
            val mapFragment = childFragmentManager.findFragmentById(io.github.thwisse.kentinsesi.R.id.mapFragment) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
        }
    }

    private fun setupComments() {
        binding.rvComments.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.btnSendComment.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            
            // ValidationUtils kullanarak yorum kontrolü
            val commentError = ValidationUtils.getValidationError("comment", text)
            
            when {
                text.isEmpty() -> {
                    binding.etComment.error = "Yorum boş olamaz"
                    binding.etComment.requestFocus()
                    return@setOnClickListener
                }
                commentError != null -> {
                    binding.etComment.error = commentError
                    binding.etComment.requestFocus()
                    return@setOnClickListener
                }
                currentPostId == null -> {
                    Toast.makeText(requireContext(), "Post ID bulunamadı", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            
            viewModel.addComment(currentPostId!!, text)
            binding.etComment.text.clear()
            binding.etComment.error = null
        }

        viewModel.commentsState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> commentAdapter.submitList(resource.data)
                is Resource.Error -> Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                is Resource.Loading -> { }
            }
        }
        
        viewModel.addCommentState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Error -> {
                    binding.etComment.error = resource.message
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Success -> {
                    // Yorum başarıyla eklendi, yorumlar otomatik yenilenecek
                }
                is Resource.Loading -> { }
            }
        }
    }
    
    /**
     * Yetkili kullanıcılar için butonları ayarla
     */
    private fun setupOfficialActions() {
        // Yetkili kontrolü için LiveData'ları observe et
        viewModel.canUpdateStatus.observe(viewLifecycleOwner) { canUpdate ->
            // Yetkili butonlarını göster/gizle
            // Bu butonlar layout'ta olmalı, şimdilik sadece menüde gösteriyoruz
        }
        
        // Post durumunu göster
        viewModel.currentPost.observe(viewLifecycleOwner) { post ->
            post?.let {
                // Post durumunu göster (opsiyonel)
                val statusText = when (it.statusEnum) {
                    PostStatus.NEW -> "Yeni"
                    PostStatus.IN_PROGRESS -> "İşleme Alındı"
                    PostStatus.RESOLVED -> "Çözüldü"
                    PostStatus.REJECTED -> "Reddedildi"
                }
                // Eğer layout'ta status gösterilecek bir TextView varsa:
                // binding.tvStatus.text = statusText
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        postLocation?.let { location ->
            googleMap.addMarker(MarkerOptions().position(location).title("Sorun Konumu"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}