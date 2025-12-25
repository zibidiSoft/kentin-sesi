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
import io.github.thwisse.kentinsesi.databinding.FragmentPostDetailBinding
import io.github.thwisse.kentinsesi.util.Resource
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailBinding.bind(view)

        val post = arguments?.getParcelable<Post>("post")

        if (post != null) {
            currentPostId = post.id
            setupViews(post)
            setupMap(post)
            setupComments()

            // Yorumları Çek
            viewModel.getComments(post.id)

            // Yetki Kontrolü ve Menü
            if (viewModel.currentUserId == post.authorId) {
                setupOwnerMenu()
            }
        }

        // --- DEĞİŞİKLİK: binding.toolbar kodları SİLİNDİ ---
        // Geri butonu artık MainActivity'deki setSupportActionBar sayesinde otomatik çalışacak.

        observeOwnerActions()
    }

    private fun setupOwnerMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(io.github.thwisse.kentinsesi.R.menu.menu_post_detail, menu)
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
            if (text.isNotEmpty() && currentPostId != null) {
                viewModel.addComment(currentPostId!!, text)
                binding.etComment.text.clear()
            } else {
                Toast.makeText(requireContext(), "Yorum boş olamaz", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.commentsState.observe(viewLifecycleOwner) { resource ->
            if(resource is Resource.Success) commentAdapter.submitList(resource.data)
        }
        viewModel.addCommentState.observe(viewLifecycleOwner) {
            // Hata mesajı vs.
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