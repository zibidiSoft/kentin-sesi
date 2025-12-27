package io.github.thwisse.kentinsesi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.data.model.FilterCriteria
import io.github.thwisse.kentinsesi.databinding.DialogFilterOptionsBinding
import io.github.thwisse.kentinsesi.databinding.FragmentFilterBottomSheetBinding
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()

    // Seçilen filtreler
    private var selectedDistricts = mutableSetOf<String>()
    private var selectedCategories = mutableSetOf<String>()
    private var selectedStatuses = mutableSetOf<String>()

    // Filtre listeleri
    private val districts = listOf(
        "Altınözü", "Antakya", "Arsuz", "Belen", "Defne", "Dörtyol",
        "Erzin", "Hassa", "İskenderun", "Kırıkhan", "Kumlu", "Payas", 
        "Reyhanlı", "Samandağ", "Yayladağı"
    )
    
    private val categories = listOf(
        "Altyapı (Yol/Su)", "Temizlik/Çöp", "Park/Bahçe", "Aydınlatma", "Trafik", "Diğer"
    )
    
    private val statusLabels = mapOf(
        "new" to "Yeni",
        "in_progress" to "İşlemde",
        "resolved" to "Çözüldü"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restoreFilters()
        updateFilterSummaries()
        setupClickListeners()

        binding.btnApplyFilter.setOnClickListener {
            applyFilters()
        }

        binding.btnSavePreset.setOnClickListener {
            showSavePresetDialog()
        }
    }

    private fun showSavePresetDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Filtre adı"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtreyi kaydet")
            .setView(input)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Kaydet") { _, _ ->
                val name = input.text?.toString().orEmpty().trim()

                val criteria = FilterCriteria(
                    districts = selectedDistricts.toList(),
                    categories = selectedCategories.toList(),
                    statuses = selectedStatuses.toList()
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    when (val result = viewModel.savePresetNow(name, criteria)) {
                        is Resource.Success -> {
                            Toast.makeText(requireContext(), "Filtre kaydedildi", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                        is Resource.Error -> {
                            Toast.makeText(requireContext(), result.message ?: "Filtre kaydedilemedi", Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            // no-op
                        }
                    }
                }
            }
            .show()
    }

    private fun setupClickListeners() {
        // İlçe filtresine tıkla
        binding.cvDistrict.setOnClickListener {
            showFilterOptionsDialog(
                title = "İlçe Seç",
                items = districts,
                selectedItems = selectedDistricts,
                onSave = { selected ->
                    selectedDistricts = selected.toMutableSet()
                    updateFilterSummaries()
                }
            )
        }

        // Kategori filtresine tıkla
        binding.cvCategory.setOnClickListener {
            showFilterOptionsDialog(
                title = "Kategori Seç",
                items = categories,
                selectedItems = selectedCategories,
                onSave = { selected ->
                    selectedCategories = selected.toMutableSet()
                    updateFilterSummaries()
                }
            )
        }

        // Durum filtresine tıkla
        binding.cvStatus.setOnClickListener {
            val statusDisplayItems = statusLabels.values.toList()
            val selectedStatusDisplay: Set<String> = selectedStatuses.mapNotNull { statusKey ->
                statusLabels[statusKey]
            }.toSet()
            
            showFilterOptionsDialog(
                title = "Durum Seç",
                items = statusDisplayItems,
                selectedItems = selectedStatusDisplay,
                onSave = { selected ->
                    // Display name'den status key'e çevir
                    selectedStatuses = selected.mapNotNull { displayName ->
                        statusLabels.entries.firstOrNull { it.value == displayName }?.key
                    }.toMutableSet()
                    updateFilterSummaries()
                }
            )
        }
    }

    private fun showFilterOptionsDialog(
        title: String,
        items: List<String>,
        selectedItems: Set<String>,
        onSave: (Set<String>) -> Unit
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(io.github.thwisse.kentinsesi.R.layout.dialog_filter_options, null)
        val dialogBinding = DialogFilterOptionsBinding.bind(dialogView)
        
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Dialog genişliğini ayarla
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.tvDialogTitle.text = title

        // Başlangıç seçimlerini hazırla
        val initialSelectedItems = selectedItems.toMutableSet()
        
        // Adapter için filter items oluştur
        val filterItems = items.map { item -> 
            FilterItem(
                label = item,
                value = item,
                isSelected = initialSelectedItems.contains(item)
            )
        }
        
        // "Tümünü Seç" butonu metnini güncelleme fonksiyonu
        fun updateSelectAllButton(adapter: FilterCheckboxAdapter, binding: DialogFilterOptionsBinding, totalCount: Int) {
            val selectedCount = adapter.getSelectedItems().size
            val allSelected = selectedCount == totalCount
            binding.btnSelectAll.text = if (allSelected) "Tümünü Kaldır" else "Tümünü Seç"
        }
        
        // Adapter'ı lateinit var olarak tanımla (callback içinde kullanmak için)
        lateinit var adapter: FilterCheckboxAdapter
        
        // Adapter'ı oluştur
        adapter = FilterCheckboxAdapter { updatedItem ->
            // Seçim değiştiğinde adapter'ı güncelle
            val updatedList = adapter.currentList.map { filterItem ->
                if (filterItem.value == updatedItem.value) {
                    updatedItem
                } else {
                    filterItem
                }
            }
            adapter.submitList(updatedList)
            updateSelectAllButton(adapter, dialogBinding, items.size)
        }
        
        dialogBinding.rvFilterOptions.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(requireContext())
            // RecyclerView'ın görünür olduğundan emin ol
            visibility = View.VISIBLE
            // RecyclerView'ın boyutunu içeriğe göre ayarla
            setHasFixedSize(false)
        }
        
        // Adapter'a veriyi yükle
        adapter.submitList(filterItems)
        
        // Debug: Adapter'ın item sayısını kontrol et
        if (filterItems.isEmpty()) {
            android.util.Log.w("FilterBottomSheet", "Filter items listesi boş!")
        } else {
            android.util.Log.d("FilterBottomSheet", "Filter items sayısı: ${filterItems.size}")
        }
        
        // İlk yüklemede buton metnini güncelle
        updateSelectAllButton(adapter, dialogBinding, items.size)
        
        // "Tümünü Seç" butonu
        dialogBinding.btnSelectAll.setOnClickListener {
            val selectedCount = adapter.getSelectedItems().size
            val allSelected = selectedCount == items.size
            
            // Tümünü seç veya kaldır
            val updatedList = adapter.currentList.map { filterItem ->
                filterItem.copy(isSelected = !allSelected)
            }
            adapter.submitList(updatedList)
            updateSelectAllButton(adapter, dialogBinding, items.size)
        }

        // İptal butonu
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Kaydet butonu
        dialogBinding.btnSave.setOnClickListener {
            val selected = adapter.getSelectedItems()
                .mapNotNull { it.value }
                .toSet()
            
            // Eğer tümü seçiliyse boş set gönder (tümü = filtre yok)
            // Aksi halde seçilenleri gönder
            val finalSelected = if (selected.size == items.size) {
                emptySet<String>()
            } else {
                selected
            }
            
            onSave(finalSelected)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateFilterSummaries() {
        // İlçe özeti
        binding.tvDistrictSummary.text = getFilterSummary(
            selectedItems = selectedDistricts,
            totalItems = districts.size
        )

        // Kategori özeti
        binding.tvCategorySummary.text = getFilterSummary(
            selectedItems = selectedCategories,
            totalItems = categories.size
        )

        // Durum özeti - Önce key'leri display name'e çevir
        val selectedStatusDisplay: List<String> = selectedStatuses.mapNotNull { statusKey ->
            statusLabels[statusKey]
        }
        binding.tvStatusSummary.text = when {
            selectedStatuses.isEmpty() -> "Tümü"
            selectedStatuses.size == statusLabels.size -> "Tümü"
            selectedStatuses.size == 1 -> selectedStatusDisplay.firstOrNull() ?: "Tümü"
            else -> "${selectedStatuses.size} seçenek"
        }
    }
    
    /**
     * Filtre özeti metnini oluşturur
     * - Boş veya tümü seçiliyse: "Tümü"
     * - Tek seçim varsa: Seçilen item'ın adı
     * - Birden fazla seçim varsa: "X seçenek"
     */
    private fun getFilterSummary(selectedItems: Set<String>, totalItems: Int): String {
        return when {
            selectedItems.isEmpty() -> "Tümü"
            selectedItems.size == totalItems -> "Tümü"
            selectedItems.size == 1 -> selectedItems.first()
            else -> "${selectedItems.size} seçenek"
        }
    }

    private fun restoreFilters() {
        // Gelen verileri oku (List olarak veya eski tek string formatı)
        val districtsList = arguments?.getStringArrayList("districts") ?: 
            arguments?.getString("district")?.let { listOf(it) }?.takeIf { it.isNotEmpty() } ?: emptyList()
        val categoriesList = arguments?.getStringArrayList("categories") ?: 
            arguments?.getString("category")?.let { listOf(it) }?.takeIf { it.isNotEmpty() } ?: emptyList()
        val statusesList = arguments?.getStringArrayList("statuses") ?: 
            arguments?.getString("status")?.let { listOf(it) }?.takeIf { it.isNotEmpty() } ?: emptyList()

        selectedDistricts = districtsList.toMutableSet()
        selectedCategories = categoriesList.toMutableSet()
        selectedStatuses = statusesList.toMutableSet()
    }

    private fun applyFilters() {
        // Verileri HomeFragment'a paketle gönder
        setFragmentResult("filter_request", bundleOf(
            "districts" to ArrayList(selectedDistricts),
            "categories" to ArrayList(selectedCategories),
            "statuses" to ArrayList(selectedStatuses)
        ))

        dismiss() // Pencereyi kapat
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}