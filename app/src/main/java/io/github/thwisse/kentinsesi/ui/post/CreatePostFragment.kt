package io.github.thwisse.kentinsesi.ui.post

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.databinding.FragmentCreatePostBinding
import io.github.thwisse.kentinsesi.util.Resource

@AndroidEntryPoint
class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreatePostViewModel by viewModels()

    // Konum servisi istemcisi
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Anlık konum bilgileri
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    private var currentDistrict: String? = null // Bulunan ilçe burada tutulacak

    // --- GALERİDEN FOTOĞRAF SEÇİCİ ---
    // Modern Android Photo Picker kullanıyoruz
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            binding.ivPostImage.setImageURI(uri)
            viewModel.selectedImageUri = uri // ViewModel'e kaydet
        } else {
            Toast.makeText(requireContext(), "Fotoğraf seçilmedi", Toast.LENGTH_SHORT).show()
        }
    }

    // --- KONUM İZNİ İSTEYİCİ ---
    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // İzin verildi, konumu al
            getCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Konum izni olmadan paylaşım yapılamaz.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Konum servisini başlat
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupDistrictSpinner()
        setupCategorySpinner()
        setupClickListeners()
        observeState()

        // Ekran açılır açılmaz konumu almaya çalış
        checkLocationPermissionAndGet()
    }

    private fun setupDistrictSpinner() {
        val districts = listOf(
            "Altınözü", "Antakya", "Arsuz", "Belen", "Defne", "Dörtyol",
            "Erzin", "Hassa", "İskenderun", "Kırıkhan", "Kumlu",
            "Payas", "Reyhanlı", "Samandağ", "Yayladağı"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, districts)
        binding.actvDistrict.setAdapter(adapter)
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Altyapı (Yol/Su)", "Temizlik/Çöp", "Park/Bahçe", "Aydınlatma", "Trafik", "Diğer")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        // Fotoğraf Seçimi
        binding.ivPostImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Paylaş Butonu
        binding.btnShare.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val category = binding.actvCategory.text.toString().trim()
            // YENİ: İlçeyi ekrandaki kutucuktan okuyoruz
            val selectedDistrict = binding.actvDistrict.text.toString().trim()

            // 1. Boş Alan Kontrolü
            if (title.isEmpty() || description.isEmpty() || category.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen başlık, açıklama ve kategori giriniz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. İlçe Seçimi Kontrolü
            if (selectedDistrict.isEmpty() || selectedDistrict == "İlçe Seçiniz") {
                Toast.makeText(requireContext(), "Lütfen geçerli bir ilçe seçiniz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Konum Kontrolü (GPS verisi gelmiş mi?)
            if (currentLatitude == null || currentLongitude == null) {
                Toast.makeText(requireContext(), "Konum bekleniyor... Lütfen bekleyin veya GPS'i kontrol edin.", Toast.LENGTH_SHORT).show()
                checkLocationPermissionAndGet() // Tekrar denemek için
                return@setOnClickListener
            }

            // 4. ViewModel'e Gönder (Manuel seçilen ilçe ile birlikte)
            viewModel.createPost(
                title = title,
                description = description,
                category = category,
                latitude = currentLatitude!!,
                longitude = currentLongitude!!,
                district = selectedDistrict // Kullanıcının seçtiği ilçe
            )
        }
    }
    private fun checkLocationPermissionAndGet() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            requestLocationPermission.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun getCurrentLocation() {
        binding.tvLocationInfo.text = "Konum alınıyor..."

        try {
            // Yüksek hassasiyetle anlık konumu iste
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        // Koordinatları değişkenlere al
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude

                        // Ekrana sadece koordinat bilgisi bas
                        binding.tvLocationInfo.text = "Koordinat alındı: ${location.latitude}, ${location.longitude}"
                    } else {
                        binding.tvLocationInfo.text = "Konum bulunamadı. GPS açık mı?"
                    }
                }
                .addOnFailureListener {
                    binding.tvLocationInfo.text = "Konum hatası: ${it.message}"
                }
        } catch (e: SecurityException) {
            // İzin verilmemişse buraya düşer
            binding.tvLocationInfo.text = "Konum izni yok."
        }
    }

    private fun observeState() {
        viewModel.createPostState.observe(viewLifecycleOwner) { resource ->
            binding.progressBar.isVisible = resource is Resource.Loading
            binding.btnShare.isEnabled = resource !is Resource.Loading

            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Bildirim başarıyla oluşturuldu!", Toast.LENGTH_LONG).show()
                    // Başarılı olunca bir önceki ekrana (Home) dön
                    findNavController().popBackStack()
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> { }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}