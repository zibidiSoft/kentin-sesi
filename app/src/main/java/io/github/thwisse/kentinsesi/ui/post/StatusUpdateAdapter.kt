package io.github.thwisse.kentinsesi.ui.post

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.thwisse.kentinsesi.data.model.StatusUpdate
import io.github.thwisse.kentinsesi.databinding.ItemStatusUpdateBinding
import java.text.SimpleDateFormat
import java.util.Locale

class StatusUpdateAdapter : ListAdapter<StatusUpdate, StatusUpdateAdapter.ViewHolder>(StatusUpdateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatusUpdateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemStatusUpdateBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))

        fun bind(update: StatusUpdate) {
            // Durum badge'i
            val (badgeText, badgeColor) = when (update.status) {
                "new" -> "YENİ" to Color.parseColor("#2196F3") // Mavi
                "in_progress" -> "İŞLEMDE" to Color.parseColor("#FF9800") // Turuncu
                "resolved" -> "ÇÖZÜLDÜ" to Color.parseColor("#4CAF50") // Yeşil
                else -> "BİLİNMEYEN" to Color.parseColor("#9E9E9E") // Gri
            }
            
            binding.tvStatusBadge.text = badgeText
            binding.tvStatusBadge.setBackgroundColor(badgeColor)
            
            // Tarih
            binding.tvDate.text = update.createdAt?.let { dateFormat.format(it) } ?: "-"
            
            // Yazar
            val usernameText = if (update.authorUsername.isNotBlank()) 
                "@${update.authorUsername}" 
            else 
                ""
                
            binding.tvAuthor.text = if (usernameText.isNotBlank()) {
                "${update.authorFullName} ($usernameText)"
            } else {
                update.authorFullName
            }
            
            // Not
            binding.tvNote.text = update.note
        }
    }

    class StatusUpdateDiffCallback : DiffUtil.ItemCallback<StatusUpdate>() {
        override fun areItemsTheSame(oldItem: StatusUpdate, newItem: StatusUpdate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StatusUpdate, newItem: StatusUpdate): Boolean {
            return oldItem == newItem
        }
    }
}
