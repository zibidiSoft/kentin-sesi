package io.github.thwisse.kentinsesi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.thwisse.kentinsesi.databinding.DialogFilterOptionsBinding
import io.github.thwisse.kentinsesi.databinding.FragmentFilterBottomSheetBinding

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

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
            .create()
        
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.tvDialogTitle.text = title

        // Adapter oluştur - "Tümü" seçeneği yok, sadece normal seçenekler
        val filterItems = items.map { item -> FilterItem(item, item, selectedItems.contains(item)) }
        
        // Adapter için callback fonksiyonu - adapter oluşturulduktan sonra set edilecek
        var onItemSelectedCallback: ((FilterItem) -> Unit)? = null
        
        // Adapter'ı oluştur
        val adapter = FilterCheckboxAdapter { item ->
            onItemSelectedCallback?.invoke(item)
        }
        
        // "Tümünü Seç" butonu metnini güncelleme fonksiyonu
        fun updateSelectAllButton() {
            val selectedCount = adapter.getSelectedItems().size
            val allSelected = selectedCount == items.size
            dialogBinding.btnSelectAll.text = if (allSelected) "Tümünü Kaldır" else "Tümünü Seç"
        }
        
        // Callback'i set et
        onItemSelectedCallback = { item ->
            adapter.updateSelection(item)
            updateSelectAllButton()
        }
        
        dialogBinding.rvFilterOptions.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(requireContext())
            // RecyclerView'ın görünür olduğundan emin ol
            visibility = View.VISIBLE
        }
        
        // Adapter'a veriyi yükle
        adapter.submitList(filterItems)
        
        // İlk yüklemede buton metnini güncelle
        updateSelectAllButton()
        
        dialogBinding.btnSelectAll.setOnClickListener {
            val allSelected = adapter.getSelectedItems().size == items.size
            val updatedList = adapter.currentList.map { filterItem ->
                filterItem.copy(isSelected = !allSelected)
            }
            adapter.submitList(updatedList)
            updateSelectAllButton()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val selected = adapter.getSelectedItems()
                .mapNotNull { it.value }
                .toSet()
            // Eğer tümü seçiliyse boş set gönder (tümü = filtre yok)
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
        binding.tvDistrictSummary.text = when {
            selectedDistricts.isEmpty() -> "Tümü"
            selectedDistricts.size == districts.size -> "Tümü"
            selectedDistricts.size == 1 -> selectedDistricts.first()
            else -> "${selectedDistricts.size} seçenek"
        }

        // Kategori özeti
        binding.tvCategorySummary.text = when {
            selectedCategories.isEmpty() -> "Tümü"
            selectedCategories.size == categories.size -> "Tümü"
            selectedCategories.size == 1 -> selectedCategories.first()
            else -> "${selectedCategories.size} seçenek"
        }

        // Durum özeti
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