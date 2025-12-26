package io.github.thwisse.kentinsesi.ui.map

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.databinding.FragmentMapBinding
import io.github.thwisse.kentinsesi.ui.home.HomeViewModel
import io.github.thwisse.kentinsesi.util.Resource

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()

    private var googleMap: GoogleMap? = null

    private var latestPosts: List<Post> = emptyList()

    private enum class MapDisplayScope {
        ALL,
        MINE
    }

    private var displayScope: MapDisplayScope = MapDisplayScope.ALL

    // Marker ile Post'u eşleştirmek için bir harita (Map) tutuyoruz
    private val markerPostMap = HashMap<Marker, Post>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapBinding.bind(view)

        setupMenu()
        setupFilterResultListener()

        // Paylaşılan post state'ini erken dinle (harita hazır olmasa bile listeyi cache'le)
        observePosts()

        // Haritayı Başlat
        val mapFragment = childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(R.menu.menu_map, menu)
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_post -> {
                        findNavController().navigate(R.id.action_nav_map_to_createPostFragment)
                        true
                    }
                    R.id.action_filter -> {
                        val bundle = Bundle().apply {
                            viewModel.lastDistricts?.let { putStringArrayList("districts", ArrayList(it)) }
                            viewModel.lastCategories?.let { putStringArrayList("categories", ArrayList(it)) }
                            viewModel.lastStatuses?.let { putStringArrayList("statuses", ArrayList(it)) }
                        }
                        findNavController().navigate(R.id.action_nav_map_to_filterBottomSheetFragment, bundle)
                        true
                    }
                    R.id.action_all_posts -> {
                        displayScope = MapDisplayScope.ALL
                        if (googleMap != null) {
                            addMarkers(latestPosts)
                        }
                        true
                    }
                    R.id.action_my_posts -> {
                        val userId = viewModel.currentUserId
                        if (userId.isBlank()) {
                            Toast.makeText(requireContext(), "Kullanıcı oturumu bulunamadı", Toast.LENGTH_SHORT).show()
                            return true
                        }

                        displayScope = MapDisplayScope.MINE
                        if (googleMap != null) {
                            addMarkers(latestPosts.filter { it.authorId == userId })
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupFilterResultListener() {
        setFragmentResultListener("filter_request") { _, bundle ->
            val districts = bundle.getStringArrayList("districts")
            val categories = bundle.getStringArrayList("categories")
            val statuses = bundle.getStringArrayList("statuses")

            viewModel.getPosts(
                districts = districts?.toList(),
                categories = categories?.toList(),
                statuses = statuses?.toList()
            )

            Toast.makeText(requireContext(), "Filtreler uygulandı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Baloncuk tıklamasını dinle
        map.setOnInfoWindowClickListener(this)

        // Başlangıç konumu (Örn: İskenderun Meydanı)
        val startLocation = LatLng(36.58, 36.17)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 12f))

        // Harita hazır olduğunda bulunduğumuz scope'a göre marker bas (filtreler her zaman uygulanmış olur)
        when (displayScope) {
            MapDisplayScope.ALL -> addMarkers(latestPosts)
            MapDisplayScope.MINE -> {
                val userId = viewModel.currentUserId
                addMarkers(latestPosts.filter { it.authorId == userId })
            }
        }
    }

    private fun observePosts() {
        viewModel.postsState.observe(viewLifecycleOwner) { resource ->
            when(resource) {
                is Resource.Success -> {
                    val posts = resource.data ?: emptyList()
                    latestPosts = posts
                    if (googleMap != null) {
                        when (displayScope) {
                            MapDisplayScope.ALL -> addMarkers(posts)
                            MapDisplayScope.MINE -> {
                                val userId = viewModel.currentUserId
                                addMarkers(posts.filter { it.authorId == userId })
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    // Loading gösterilebilir
                }
            }
        }
    }

    private fun addMarkers(posts: List<Post>) {
        googleMap?.clear() // Önce temizle
        markerPostMap.clear()

        for (post in posts) {
            if (post.location != null) {
                val position = LatLng(post.location.latitude, post.location.longitude)

                val marker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(post.title)
                        .snippet(post.category) // Altına kategori yazsın
                )

                // Marker ile Post nesnesini eşleştir
                if (marker != null) {
                    markerPostMap[marker] = post
                }
            }
        }
    }

    // Marker'ın üzerindeki balona tıklanınca çalışır
    override fun onInfoWindowClick(marker: Marker) {
        val post = markerPostMap[marker]
        if (post != null) {
            // Detay sayfasına git (sadece post ID gönder)
            val bundle = Bundle().apply { putString("postId", post.id) }
            try {
                findNavController().navigate(R.id.action_nav_map_to_postDetailFragment, bundle)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigasyon hatası", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}