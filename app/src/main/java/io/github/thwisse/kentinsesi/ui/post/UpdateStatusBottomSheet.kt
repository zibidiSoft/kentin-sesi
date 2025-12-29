package io.github.thwisse.kentinsesi.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.model.PostStatus
import io.github.thwisse.kentinsesi.databinding.BottomSheetUpdateStatusBinding

class UpdateStatusBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetUpdateStatusBinding? = null
    private val binding get() = _binding!!

    private var currentStatus: PostStatus = PostStatus.NEW
    private var onStatusUpdateListener: ((PostStatus, String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val statusValue = it.getString(ARG_CURRENT_STATUS, "new")
            currentStatus = when (statusValue) {
                "new" -> PostStatus.NEW
                "in_progress" -> PostStatus.IN_PROGRESS
                "resolved" -> PostStatus.RESOLVED
                else -> PostStatus.NEW
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUpdateStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStatusSelection()
        setupNoteInput()
        setupButtons()
    }

    private fun setupStatusSelection() {
        // Mevcut durumu otomatik seç
        when (currentStatus) {
            PostStatus.NEW -> binding.radioNew.isChecked = true
            PostStatus.IN_PROGRESS -> binding.radioInProgress.isChecked = true
            PostStatus.RESOLVED -> binding.radioResolved.isChecked = true
            PostStatus.REJECTED -> {} // Kullanılmıyor
        }
    }

    private fun setupNoteInput() {
        // Not girişi değiştikçe kaydet butonunu aktif/pasif yap
        binding.etNote.doAfterTextChanged { text ->
            val noteValid = !text.isNullOrBlank()
            binding.btnSave.isEnabled = noteValid
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val selectedStatus = when (binding.radioGroupStatus.checkedRadioButtonId) {
                R.id.radioNew -> PostStatus.NEW
                R.id.radioInProgress -> PostStatus.IN_PROGRESS
                R.id.radioResolved -> PostStatus.RESOLVED
                else -> PostStatus.NEW
            }

            val note = binding.etNote.text.toString().trim()

            if (note.isNotBlank()) {
                onStatusUpdateListener?.invoke(selectedStatus, note)
                dismiss()
            }
        }
    }

    fun setOnStatusUpdateListener(listener: (PostStatus, String) -> Unit) {
        onStatusUpdateListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CURRENT_STATUS = "current_status"

        fun newInstance(currentStatus: PostStatus): UpdateStatusBottomSheet {
            return UpdateStatusBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_STATUS, currentStatus.value)
                }
            }
        }
    }
}
