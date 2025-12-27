package io.github.thwisse.kentinsesi.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.thwisse.kentinsesi.data.model.FilterPreset
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.ItemFilterPresetBinding

class FilterPresetAdapter(
    private val onClick: (FilterPreset) -> Unit,
    private val onLongClick: (FilterPreset) -> Unit,
    private val onMenuSetDefault: (FilterPreset) -> Unit,
    private val onMenuDelete: (FilterPreset) -> Unit
) : ListAdapter<FilterPreset, FilterPresetAdapter.PresetViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val binding = ItemFilterPresetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PresetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PresetViewHolder(
        private val binding: ItemFilterPresetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FilterPreset) {
            binding.tvPresetName.text = item.name

            binding.tvPresetBadge.visibility = if (item.isDefault) View.VISIBLE else View.GONE

            val criteria = item.criteria
            val districts = criteria.districts.joinToString().ifBlank { "Tümü" }
            val categories = criteria.categories.joinToString().ifBlank { "Tümü" }
            val statuses = criteria.statuses.joinToString().ifBlank { "Tümü" }

            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }

            binding.btnPresetMenu.setOnClickListener { anchor ->
                val popupMenu = PopupMenu(anchor.context, anchor)
                popupMenu.menuInflater.inflate(R.menu.menu_filter_preset_item, popupMenu.menu)

                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_set_default -> {
                            onMenuSetDefault(item)
                            true
                        }
                        R.id.action_delete -> {
                            onMenuDelete(item)
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<FilterPreset>() {
        override fun areItemsTheSame(oldItem: FilterPreset, newItem: FilterPreset): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FilterPreset, newItem: FilterPreset): Boolean = oldItem == newItem
    }
}
