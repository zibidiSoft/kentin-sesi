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
import io.github.thwisse.kentinsesi.util.ValidationUtils

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

        // State restore
        savedInstanceState?.let { savedState ->
            savedState.getParcelable<Uri>("saved_image_uri")?.let {
                selectedImageUri = it
            }
            savedState.getDouble("saved_latitude", Double.NaN).takeIf { !it.isNaN() }?.let {
                currentLatitude = it
            }
            savedState.getDouble("saved_longitude", Double.NaN).takeIf { !it.isNaN() }?.let {
                currentLongitude = it
            }
            savedState.getString("saved_title")?.let {
                binding.etTitle.setText(it)
            }
            savedState.getString("saved_description")?.let {
                binding.etDescription.setText(it)
            }
            savedState.getString("saved_category")?.let {
                binding.actvCategory.setText(it)
            }
            savedState.getString("saved_district")?.let {
                binding.actvDistrict.setText(it)
            }
        }

        setupDistrictSpinner()
        setupCategorySpinner()

        // 1. Haritadan gelen sonucu dinle
        setupLocationResultListener()

        // 2. Tıklama olaylarını kur
        setupClickListeners()

        // 3. ViewModel'i dinle
        observeViewModel()

        // State restore: Eğer daha önce resim seçildiyse, ekran geri geldiğinde tekrar yükle
        selectedImageUri?.let {
            binding.ivPostImage.setImageURI(it)
            binding.ivPostImage.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        }
        
        // State restore: Konum bilgisi varsa göster
        if (currentLatitude != null && currentLongitude != null) {
            binding.tvLocationInfo.text = "Konum Seçildi: ${String.format("%.4f", currentLatitude)}, ${String.format("%.4f", currentLongitude)}"
            binding.tvLocationInfo.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Form verilerini kaydet
        selectedImageUri?.let {
            outState.putParcelable("saved_image_uri", it)
        }
        currentLatitude?.let {
            outState.putDouble("saved_latitude", it)
        }
        currentLongitude?.let {
            outState.putDouble("saved_longitude", it)
        }
        binding.etTitle.text.toString().takeIf { it.isNotEmpty() }?.let {
            outState.putString("saved_title", it)
        }
        binding.etDescription.text.toString().takeIf { it.isNotEmpty() }?.let {
            outState.putString("saved_description", it)
        }
        binding.actvCategory.text.toString().takeIf { it.isNotEmpty() }?.let {
            outState.putString("saved_category", it)
        }
        binding.actvDistrict.text.toString().takeIf { it.isNotEmpty() }?.let {
            outState.putString("saved_district", it)
        }
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

            // ValidationUtils kullanarak daha iyi hata mesajları gösteriyoruz
            var hasError = false

            // Başlık kontrolü
            val titleError = ValidationUtils.getValidationError("title", title)
            when {
                title.isEmpty() -> {
                    binding.etTitle.error = "Başlık gereklidir"
                    binding.etTitle.requestFocus()
                    hasError = true
                }
                titleError != null -> {
                    binding.etTitle.error = titleError
                    binding.etTitle.requestFocus()
                    hasError = true
                }
            }

            // Açıklama kontrolü
            val descriptionError = ValidationUtils.getValidationError("description", description)
            when {
                description.isEmpty() -> {
                    binding.etDescription.error = "Açıklama gereklidir"
                    if (!hasError) {
                        binding.etDescription.requestFocus()
                        hasError = true
                    }
                }
                descriptionError != null -> {
                    binding.etDescription.error = descriptionError
                    if (!hasError) {
                        binding.etDescription.requestFocus()
                        hasError = true
                    }
                }
            }

            // Kategori kontrolü
            if (category.isEmpty()) {
                binding.actvCategory.error = "Kategori seçilmelidir"
                if (!hasError) {
                    binding.actvCategory.requestFocus()
                    hasError = true
                }
            }

            // Fotoğraf kontrolü
            if (selectedImageUri == null) {
                Toast.makeText(requireContext(), "Lütfen bir fotoğraf seçiniz.", Toast.LENGTH_SHORT).show()
                hasError = true
            }

            // Konum kontrolü
            if (currentLatitude == null || currentLongitude == null) {
                Toast.makeText(requireContext(), "Lütfen haritadan bir konum seçiniz.", Toast.LENGTH_SHORT).show()
                hasError = true
            }

            // Hata varsa işlemi durdur
            if (hasError) {
                return@setOnClickListener
            }

            // Tüm validasyonlar geçti, hata mesajlarını temizle
            binding.etTitle.error = null
            binding.etDescription.error = null
            binding.actvCategory.error = null

            // Her şey tamamsa gönder
            viewModel.createPost(
                title = title,
                description = description,
                category = category,
                latitude = currentLatitude!!,
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