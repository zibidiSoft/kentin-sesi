package io.github.thwisse.kentinsesi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.thwisse.kentinsesi.databinding.FragmentFilterBottomSheetBinding

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()
        restoreFilters() // YENİ: Eski seçimleri yükle

        binding.btnApplyFilter.setOnClickListener {
            // Seçilen değerleri al
            val district = binding.actvDistrict.text.toString()
            val category = binding.actvCategory.text.toString()
            val statusMap = mapOf("Tümü" to null, "Yeni" to "new", "İşlemde" to "in_progress", "Çözüldü" to "resolved")
            val selectedStatusLabel = binding.actvStatus.text.toString()
            val status = statusMap[selectedStatusLabel]

            // "Tümü" seçildiyse null gönderelim ki filtreleme yapılmasın
            val finalDistrict = if (district == "Tümü") null else district
            val finalCategory = if (category == "Tümü") null else category

            // Verileri HomeFragment'a paketle gönder
            setFragmentResult("filter_request", bundleOf(
                "district" to finalDistrict,
                "category" to finalCategory,
                "status" to status
            ))

            dismiss() // Pencreyi kapat
        }
    }

    private fun restoreFilters() {
        // Gelen verileri oku
        val district = arguments?.getString("district")
        val category = arguments?.getString("category")
        val status = arguments?.getString("status")

        // 1. İlçe Ayarı
        if (!district.isNullOrEmpty()) {
            // setText(value, false) kullanıyoruz ki filtreleme tetiklenip liste bozulmasın
            binding.actvDistrict.setText(district, false)
        } else {
            binding.actvDistrict.setText("Tümü", false)
        }

        // 2. Kategori Ayarı
        if (!category.isNullOrEmpty()) {
            binding.actvCategory.setText(category, false)
        } else {
            binding.actvCategory.setText("Tümü", false)
        }

        // 3. Durum Ayarı (Backend kodu -> Ekranda görünen isim dönüşümü)
        // Backend "new" tutuyor ama ekranda "Yeni" yazıyor. Bunu çevirmeliyiz.
        if (!status.isNullOrEmpty()) {
            val displayStatus = when(status) {
                "new" -> "Yeni"
                "in_progress" -> "İşlemde"
                "resolved" -> "Çözüldü"
                else -> "Tümü"
            }
            binding.actvStatus.setText(displayStatus, false)
        } else {
            binding.actvStatus.setText("Tümü", false)
        }
    }

    private fun setupDropdowns() {
        // İlçeler
        val districts = listOf("Tümü", "Altınözü", "Antakya", "Arsuz", "Belen", "Defne", "Dörtyol",
            "Erzin", "Hassa", "İskenderun", "Kırıkhan", "Kumlu", "Payas", "Reyhanlı", "Samandağ", "Yayladağı")
        binding.actvDistrict.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, districts))

        // Kategoriler
        val categories = listOf("Tümü", "Altyapı (Yol/Su)", "Temizlik/Çöp", "Park/Bahçe", "Aydınlatma", "Trafik", "Diğer")
        binding.actvCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories))

        // Durumlar
        val statuses = listOf("Tümü", "Yeni", "İşlemde", "Çözüldü")
        binding.actvStatus.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statuses))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}