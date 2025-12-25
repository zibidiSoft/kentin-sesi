package io.github.thwisse.kentinsesi.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentLocationPickerBinding

class LocationPickerFragment : Fragment(R.layout.fragment_location_picker), OnMapReadyCallback {

    private var _binding: FragmentLocationPickerBinding? = null
    private val binding get() = _binding!!
    private var googleMap: GoogleMap? = null

    // Konum izni isteyicisi
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableMyLocation()
            } else {
                Toast.makeText(requireContext(), "Konumunuza gitmek için izin gerekli.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocationPickerBinding.bind(view)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnConfirmLocation.setOnClickListener {
            confirmSelection()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Varsayılan başlangıç (İskenderun)
        val startLocation = LatLng(36.58, 36.17)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 13f))

        // Konum butonunu açmaya çalış
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
            // Butonun konumunu biraz ayarlamak gerekebilir (Varsayılan olarak sağ üsttedir)
            googleMap?.uiSettings?.isMyLocationButtonEnabled = true
        } else {
            // İzin yoksa iste
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun confirmSelection() {
        googleMap?.let { map ->
            val target = map.cameraPosition.target

            setFragmentResult("requestKey_location", bundleOf(
                "latitude" to target.latitude,
                "longitude" to target.longitude
            ))

            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}