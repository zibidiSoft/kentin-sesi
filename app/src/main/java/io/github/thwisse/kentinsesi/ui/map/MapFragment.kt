package io.github.thwisse.kentinsesi.ui.map

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import io.github.thwisse.kentinsesi.util.Resource

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()
    private var googleMap: GoogleMap? = null

    // Marker ile Post'u eşleştirmek için bir harita (Map) tutuyoruz
    private val markerPostMap = HashMap<Marker, Post>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapBinding.bind(view)

        // Haritayı Başlat
        val mapFragment = childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Baloncuk tıklamasını dinle
        map.setOnInfoWindowClickListener(this)

        // Başlangıç konumu (Örn: İskenderun Meydanı)
        val startLocation = LatLng(36.58, 36.17)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 12f))

        // Harita hazır olunca verileri çekmeye başla (veya observe et)
        observePosts()
    }

    private fun observePosts() {
        viewModel.postsState.observe(viewLifecycleOwner) { resource ->
            when(resource) {
                is Resource.Success -> {
                    val posts = resource.data ?: emptyList()
                    addMarkers(posts)
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
            // Detay sayfasına git
            val bundle = Bundle().apply { putParcelable("post", post) }
            // ÖNEMLİ: Nav Graph'ta Map -> Detail okunu (action) oluşturmalıyız.
            // ID'sini kontrol et: action_nav_map_to_postDetailFragment
            try {
                findNavController().navigate(R.id.action_nav_map_to_postDetailFragment, bundle)
            } catch (e: Exception) {
                // Eğer action yoksa loga yaz veya toast göster
                Toast.makeText(requireContext(), "Navigasyon hatası", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}