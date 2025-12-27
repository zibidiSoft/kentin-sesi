package io.github.thwisse.kentinsesi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.databinding.FragmentFilterPresetsBottomSheetBinding

@AndroidEntryPoint
class FilterPresetsBottomSheetFragment : BottomSheetDialogFragment() {

    private val statusLabels = mapOf(
        "new" to "Yeni",
        "in_progress" to "İşlemde",
        "resolved" to "Çözüldü"
    )

    private var _binding: FragmentFilterPresetsBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()

    private lateinit var adapter: FilterPresetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterPresetsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FilterPresetAdapter(
            onClick = { preset ->
                viewModel.applyPreset(preset.id)
                dismiss()
            },
            onLongClick = { preset ->
                val criteria = preset.criteria

                val statusDisplay = criteria.statuses
                    .map { statusLabels[it] ?: it }

                val message = buildString {
                    appendLine("İlçeler: ${criteria.districts.joinToString().ifBlank { "Tümü" }}")
                    appendLine("Kategoriler: ${criteria.categories.joinToString().ifBlank { "Tümü" }}")
                    appendLine("Durumlar: ${statusDisplay.joinToString().ifBlank { "Tümü" }}")
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(preset.name)
                    .setMessage(message.trim())
                    .setPositiveButton("Tamam", null)
                    .show()
            },
            onMenuSetDefault = { preset ->
                viewModel.setDefaultPreset(preset.id)
            },
            onMenuDelete = { preset ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Filtreyi sil")
                    .setMessage("'${preset.name}' filtresi silinsin mi?")
                    .setNegativeButton("İptal", null)
                    .setPositiveButton("Sil") { _, _ ->
                        viewModel.deletePreset(preset.id)
                    }
                    .show()
            }
        )

        binding.rvPresets.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPresets.adapter = adapter

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        viewModel.presets.observe(viewLifecycleOwner) { presets ->
            adapter.submitList(presets)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
