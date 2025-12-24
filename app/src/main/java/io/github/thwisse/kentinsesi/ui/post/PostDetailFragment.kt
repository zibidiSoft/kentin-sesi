package io.github.thwisse.kentinsesi.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.databinding.FragmentPostDetailBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class PostDetailFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private var postLocation: LatLng? = null // Konumu burada tutacağız

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gelen veriyi (Post nesnesini) al
        // "post" anahtarıyla gönderilen Parcelable nesneyi okuyoruz
        val post = arguments?.getParcelable<Post>("post")

        if (post != null) {
            setupViews(post)

            // Konum varsa haritayı hazırla
            if (post.location != null) {
                postLocation = LatLng(post.location.latitude, post.location.longitude)

                // XML'deki fragment'ı bul ve haritayı başlat
                val mapFragment = childFragmentManager.findFragmentById(io.github.thwisse.kentinsesi.R.id.mapFragment) as? SupportMapFragment
                mapFragment?.getMapAsync(this)
            } else {
                // Konum yoksa harita alanını gizle (Opsiyonel)
                // binding.mapContainer.isVisible = false gibi...
            }
        }

        // Geri butonu
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    // Harita hazır olduğunda burası çalışır
    override fun onMapReady(googleMap: GoogleMap) {
        postLocation?.let { location ->
            // İğne ekle
            googleMap.addMarker(MarkerOptions().position(location).title("Sorun Konumu"))

            // Kamerayı oraya odakla (Zoom seviyesi: 15f)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))

            // Haritaya tıklanınca Google Maps uygulamasını aç (Navigasyon için)
            googleMap.setOnMapClickListener {
                // İleride buraya Intent yazıp navigasyon başlatabiliriz
            }
        }
    }

    private fun setupViews(post: Post) {
        binding.apply {
            tvDetailTitle.text = post.title
            tvDetailDescription.text = post.description
            tvDetailCategory.text = post.category
            tvDetailDistrict.text = post.district ?: "İlçe Yok"

            ivDetailImage.load(post.imageUrl) {
                crossfade(true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}