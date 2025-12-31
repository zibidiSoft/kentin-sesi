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
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.data.model.PostStatus
import io.github.thwisse.kentinsesi.databinding.FragmentPostDetailBinding
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.util.Resource
import io.github.thwisse.kentinsesi.util.ValidationUtils
import androidx.core.view.isVisible
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import android.content.Context
import android.view.inputmethod.InputMethodManager
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class PostDetailFragment : Fragment(io.github.thwisse.kentinsesi.R.layout.fragment_post_detail), OnMapReadyCallback {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    
    // ViewModel Tanımı
    private val viewModel: PostDetailViewModel by viewModels()

    private var postLocation: LatLng? = null
    private var currentPostId: String? = null
    private var currentPost: Post? = null

    private var lastRequestedStatus: PostStatus? = null
    private var statusUpdateConsumed = false

    private var isRefreshingPost: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailBinding.bind(view)

        binding.swipeRefreshLayout.setOnRefreshListener {
            val postId = currentPostId
            if (postId != null) {
                binding.swipeRefreshLayout.isRefreshing = true
                isRefreshingPost = true
                viewModel.loadPostById(postId)
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

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

        observePostLoadState()
        observeToggleUpvoteState()
        observeOwnerActions()
        setupStatusUpdates()

        viewModel.postAuthor.observe(viewLifecycleOwner) { author ->
            if (author == null) {
                binding.cardPostAuthor.isVisible = false
            } else {
                binding.cardPostAuthor.isVisible = true
                binding.tvPostAuthorFullName.text = author.fullName.ifBlank { "-" }

                val username = author.username.trim().takeIf { it.isNotBlank() }
                binding.tvPostAuthorUsername.isVisible = username != null
                binding.tvPostAuthorUsername.text = if (username != null) "@$username" else ""

                val location = listOf(author.city, author.district)
                    .filter { it.isNotBlank() }
                    .joinToString("/")
                val title = author.title
                binding.tvPostAuthorMeta.text = buildString {
                    if (location.isNotBlank()) append(location)
                    if (location.isNotBlank() && title.isNotBlank()) append(" • ")
                    if (title.isNotBlank()) append(title)
                }.ifBlank { " " }
            }
        }
    }

    // Comment functions moved to CommentsFragment
    // Comment functions moved to CommentsFragment
    
    private fun observePostLoadState() {
        viewModel.postLoadState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    if (isRefreshingPost) {
                        isRefreshingPost = false
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    if (isRefreshingPost) {
                        isRefreshingPost = false
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
                is Resource.Loading -> {
                    // no-op
                }
            }
        }
    }

    private fun observeToggleUpvoteState() {
        viewModel.toggleUpvoteState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    // Optimistic güncelleme yaptıysak server state'e dönmek için yeniden çek
                    currentPostId?.let { viewModel.loadPostById(it) }
                }
                else -> {
                    // no-op
                }
            }
        }
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

        binding.tvCommentsHeader.text = getString(R.string.comments_header, post.commentCount)
        
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
                    // Post zaten yüklü, sadece güncelle (status/commentCount değişikliği gibi)
                    currentPost = it
                    setupViews(it)
                }

                if (isRefreshingPost) {
                    isRefreshingPost = false
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
        
        // Post yükleme durumu için alternatif observer - her zaman UI'ı güncelle
        viewModel.postLoadState.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                resource.data?.let { post ->
                    currentPost = post
                    setupViews(post)
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
                menu.findItem(io.github.thwisse.kentinsesi.R.id.action_update_status)?.isVisible = false
                menu.findItem(io.github.thwisse.kentinsesi.R.id.action_delete)?.isVisible = false
                
                // Silme kontrolü - Post sahibi veya admin silebilir
                viewModel.canDeletePost.observe(viewLifecycleOwner) { canDelete ->
                    menu.findItem(io.github.thwisse.kentinsesi.R.id.action_delete)?.isVisible = canDelete
                }
                
                // Durumu güncelle - Yetkili kullanıcılar veya post sahibi yapabilir
                viewModel.canUpdateStatus.observe(viewLifecycleOwner) { canUpdate ->
                    menu.findItem(io.github.thwisse.kentinsesi.R.id.action_update_status)?.isVisible = canUpdate
                }
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                return when (menuItem.itemId) {
                    io.github.thwisse.kentinsesi.R.id.action_update_status -> {
                        showUpdateStatusDialog()
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
    
    /**
     * Post durumunu güncellemek için dialog göster
     */
    private fun setupStatusUpdates() {
        // Status count observer
        viewModel.statusUpdateCount.observe(viewLifecycleOwner) { count ->
            binding.tvStatusUpdates.text = getString(R.string.status_updates_count, count)
        }
        
        // Timeline'ı yükle
        currentPostId?.let { postId ->
            viewModel.loadStatusUpdates(postId)
        }
        
        // Güncellemeler butonuna tıklama
        binding.cardStatusUpdates.setOnClickListener {
            currentPostId?.let { postId ->
                val bundle = Bundle().apply {
                    putString("postId", postId)
                }
                findNavController().navigate(
                    R.id.action_postDetailFragment_to_statusUpdatesFragment,
                    bundle
                )
            }
        }
        
        // Status update sonucu observer
        viewModel.addStatusUpdateState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    // Sadece bir kez toast göster
                    if (!statusUpdateConsumed) {
                        Toast.makeText(requireContext(), R.string.status_update_success, Toast.LENGTH_SHORT).show()
                        statusUpdateConsumed = true
                        // Post detail'i yenile (status değişti)
                        currentPostId?.let { viewModel.loadPostById(it) }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message ?: getString(R.string.status_update_error), Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> { 
                    // Loading başladığında flag'i reset et
                    statusUpdateConsumed = false
                }
            }
        }
    }
    
    
    private fun showUpdateStatusDialog() {
        val currentPostStatus = currentPost?.statusEnum ?: viewModel.currentPost.value?.statusEnum ?: PostStatus.NEW
        
        val bottomSheet = UpdateStatusBottomSheet.newInstance(currentPostStatus)
        bottomSheet.setOnStatusUpdateListener { status, note ->
            viewModel.addStatusUpdate(status, note)
        }
        bottomSheet.show(childFragmentManager, "UpdateStatusBottomSheet")
    }

    private fun observeOwnerActions() {
        viewModel.deletePostState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.post_deleted_success), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is Resource.Error -> Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                is Resource.Loading -> { }
            }
        }

        viewModel.updateStatusState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val statusText = when (lastRequestedStatus) {
                        PostStatus.NEW -> getString(R.string.status_new)
                        PostStatus.IN_PROGRESS -> getString(R.string.status_in_progress)
                        PostStatus.RESOLVED -> getString(R.string.status_resolved)
                        PostStatus.REJECTED -> getString(R.string.status_rejected)
                        else -> getString(R.string.status_update_success)
                    }
                    Toast.makeText(requireContext(), "${getString(R.string.status_update_success)}: $statusText", Toast.LENGTH_SHORT).show()

                    // ViewModel optimistic update yaptı, ekranı da güncelle
                    viewModel.currentPost.value?.let { setupViews(it) }
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
            tvDetailDistrict.text = "Hatay, ${post.district ?: "-"}"

            tvDetailDate.text = post.createdAt?.toDate()?.let { date ->
                val format = SimpleDateFormat("EEEE, dd MMM yyyy, HH:mm", Locale.getDefault())
                format.format(date)
            } ?: ""

            // Durum metni ve rengi
            val statusColorRes = when (post.statusEnum) {
                PostStatus.NEW -> io.github.thwisse.kentinsesi.R.color.statusNew
                PostStatus.IN_PROGRESS -> io.github.thwisse.kentinsesi.R.color.statusInProgress
                PostStatus.RESOLVED -> io.github.thwisse.kentinsesi.R.color.statusResolved
                PostStatus.REJECTED -> io.github.thwisse.kentinsesi.R.color.statusRejected
            }
            tvDetailStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), statusColorRes))

            tvDetailStatus.text = when (post.statusEnum) {
                PostStatus.NEW -> getString(R.string.post_status_new)
                PostStatus.IN_PROGRESS -> getString(R.string.post_status_in_progress)
                PostStatus.RESOLVED -> getString(R.string.post_status_resolved)
                PostStatus.REJECTED -> "Reddedildi"
            }

            tvDetailUpvoteCount.text = "${post.upvoteCount} Destek"
            ivDetailImage.load(post.imageUrl) { crossfade(true) }
            
            // Yorum sayısını güncelle (CommentsFragment'tan döndükten sonra)
            tvCommentsHeader.text = getString(R.string.comments_header, post.commentCount)
            
            // Duration (Geçen süre) - resolved postlarda ve null timestamp'te gizli
            val durationText = io.github.thwisse.kentinsesi.util.TimeUtil.getRelativeTime(post.createdAt, requireContext())
            if (durationText.isNotEmpty() && post.status != "resolved") {
                tvDetailDuration.text = durationText
                tvDetailDuration.visibility = android.view.View.VISIBLE
            } else {
                tvDetailDuration.visibility = android.view.View.INVISIBLE
            }
        }

        binding.btnDetailUpvote.setOnClickListener {
            val postId = currentPostId ?: return@setOnClickListener
            val userId = viewModel.currentUserId ?: return@setOnClickListener

            // Optimistic UI: anında sayıyı değiştir ve buton metnini güncelle
            val latest = currentPost ?: post
            val alreadyUpvoted = latest.upvotedBy.contains(userId)

            val newCount = (latest.upvoteCount + if (alreadyUpvoted) -1 else 1).coerceAtLeast(0)
            val newUpvotedBy = if (alreadyUpvoted) {
                latest.upvotedBy.filterNot { it == userId }
            } else {
                latest.upvotedBy + userId
            }

            val optimisticPost = latest.copy(
                upvoteCount = newCount,
                upvotedBy = newUpvotedBy
            )

            currentPost = optimisticPost
            setupViews(optimisticPost)

            viewModel.toggleUpvote(postId)
        }

        // Buton metni: desteklediyse geri çek, değilse destekle
        val userId = viewModel.currentUserId
        val isUpvotedByMe = userId != null && post.upvotedBy.contains(userId)
        binding.btnDetailUpvote.text = if (isUpvotedByMe) "Desteği Geri Çek" else "Destekle"
    }

    private fun setupMap(post: Post) {
        if (post.location != null) {
            postLocation = LatLng(post.location.latitude, post.location.longitude)
            val mapFragment = childFragmentManager.findFragmentById(io.github.thwisse.kentinsesi.R.id.mapFragment) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
        }
    }

    private fun setupComments() {
        // Comment count observer - güncellemelerdeki gibi reactive
        viewModel.commentCount.observe(viewLifecycleOwner) { count ->
            binding.tvCommentsHeader.text = getString(R.string.comments_header, count)
        }
        
        // Comments card click - navigate to CommentsFragment
        binding.cardCommentsHeader.setOnClickListener {
            currentPostId?.let { postId ->
                val bundle = Bundle().apply {
                    putString("postId", postId)
                }
                findNavController().navigate(
                    R.id.action_postDetailFragment_to_commentsFragment,
                    bundle
            )
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
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        postLocation?.let { location ->
            googleMap.addMarker(MarkerOptions().position(location).title("Sorun Konumu"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Bottom navigation'ı gizle
        (activity as? io.github.thwisse.kentinsesi.ui.MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            io.github.thwisse.kentinsesi.R.id.bottom_nav_view
        )?.visibility = android.view.View.GONE
        
        // CommentsFragment veya StatusUpdatesFragment'tan döndükten sonra post'u yenile
        currentPostId?.let { postId ->
            viewModel.refreshPostForUI(postId)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Bottom navigation'ı geri göster
        (activity as? io.github.thwisse.kentinsesi.ui.MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            io.github.thwisse.kentinsesi.R.id.bottom_nav_view
        )?.visibility = android.view.View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}