package io.github.thwisse.kentinsesi.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.thwisse.kentinsesi.databinding.ItemFilterCheckboxBinding

data class FilterItem(
    val label: String,
    val value: String?,
    var isSelected: Boolean = false
)

class FilterCheckboxAdapter(
    private val onSelectionChanged: (FilterItem) -> Unit
) : ListAdapter<FilterItem, FilterCheckboxAdapter.FilterViewHolder>(FilterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = ItemFilterCheckboxBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FilterViewHolder(binding, onSelectionChanged)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateSelection(item: FilterItem) {
        val updatedList = currentList.map { filterItem ->
            if (filterItem.value == item.value) {
                item // Güncellenmiş item'i kullan
            } else {
                filterItem
            }
        }
        submitList(updatedList)
    }

    fun getSelectedItems(): List<FilterItem> {
        return currentList.filter { it.isSelected }
    }

    class FilterViewHolder(
        private val binding: ItemFilterCheckboxBinding,
        private val onSelectionChanged: (FilterItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FilterItem) {
            binding.tvFilterText.text = item.label
            // Listener'ı geçici olarak kaldır, sonra tekrar ekle
            binding.cbFilter.setOnCheckedChangeListener(null)
            binding.cbFilter.isChecked = item.isSelected

            binding.cbFilter.setOnCheckedChangeListener { _, isChecked ->
                if (item.isSelected != isChecked) {
                    val updatedItem = item.copy(isSelected = isChecked)
                    onSelectionChanged(updatedItem)
                }
            }

            binding.root.setOnClickListener {
                binding.cbFilter.toggle()
            }
        }
    }

    class FilterDiffCallback : DiffUtil.ItemCallback<FilterItem>() {
        override fun areItemsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean {
            return oldItem.value == newItem.value
        }

        override fun areContentsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean {
            return oldItem == newItem
        }
    }
}

