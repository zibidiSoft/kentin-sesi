package io.github.thwisse.kentinsesi.ui.post

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentCreatePostBinding
import io.github.thwisse.kentinsesi.util.Resource

@AndroidEntryPoint
class CreatePostFragment : Fragment(R.layout.fragment_create_post) {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreatePostViewModel by viewModels()

    private var selectedImageUri: Uri? = null

    // Varsayılan değerler null olsun, kullanıcı seçmeden gönderemesin
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    // Fotoğraf seçici
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPostImage.setImageURI(uri)
            binding.ivPostImage.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreatePostBinding.bind(view)

        setupDistrictSpinner()
        setupCategorySpinner()

        // 1. Haritadan gelen sonucu dinle
        setupLocationResultListener()

        // 2. Tıklama olaylarını kur
        setupClickListeners()

        // 3. ViewModel'i dinle
        observeViewModel()

        // NOT: Artık getCurrentLocation() çağırmıyoruz!

        // --- EKLENECEK KISIM (STATE RESTORATION) ---
        // Eğer daha önce resim seçildiyse, ekran geri geldiğinde tekrar yükle
        if (selectedImageUri != null) {
            binding.ivPostImage.setImageURI(selectedImageUri)
            binding.ivPostImage.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        }
        // --------------------------------------------
    }

    private fun setupLocationResultListener() {
        setFragmentResultListener("requestKey_location") { _, bundle ->
            val lat = bundle.getDouble("latitude")
            val lng = bundle.getDouble("longitude")

            // Değişkenleri güncelle
            currentLatitude = lat
            currentLongitude = lng

            // UI güncelle
            binding.tvLocationInfo.text = "Konum Seçildi: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
            // İkonun rengini değiştirebiliriz ki seçildiği belli olsun (Opsiyonel)
            binding.tvLocationInfo.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        }
    }

    private fun setupClickListeners() {
        binding.ivPostImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSelectLocation.setOnClickListener {
            findNavController().navigate(R.id.action_createPostFragment_to_locationPickerFragment)
        }

        binding.btnShare.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val category = binding.actvCategory.text.toString().trim()
            val selectedDistrict = binding.actvDistrict.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || category.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen başlık, açıklama ve kategori giriniz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri == null) {
                Toast.makeText(requireContext(), "Lütfen bir fotoğraf seçiniz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // KONUM KONTROLÜ: Kullanıcı haritadan seçmek ZORUNDA
            if (currentLatitude == null || currentLongitude == null) {
                Toast.makeText(requireContext(), "Lütfen haritadan bir konum seçiniz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Her şey tamamsa gönder
            viewModel.createPost(
                title = title,
                description = description,
                category = category,
                latitude = currentLatitude!!, // null olmadığına eminiz
                longitude = currentLongitude!!,
                district = selectedDistrict,
                imageUri = selectedImageUri!!
            )
        }
    }

    private fun setupDistrictSpinner() {
        val districts = listOf("Altınözü", "Antakya", "Arsuz", "Belen", "Defne", "Dörtyol", "Erzin", "Hassa", "İskenderun", "Kırıkhan", "Kumlu", "Payas", "Reyhanlı", "Samandağ", "Yayladağı")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, districts)
        binding.actvDistrict.setAdapter(adapter)
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Altyapı", "Ulaşım", "Çevre", "Aydınlatma", "Park/Bahçe", "Sokak Hayvanları", "Diğer")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun observeViewModel() {
        viewModel.createPostState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnShare.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnShare.isEnabled = true
                    Toast.makeText(requireContext(), "Paylaşım başarılı!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnShare.isEnabled = true
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}