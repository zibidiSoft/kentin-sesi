package io.github.thwisse.kentinsesi.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.ItemNotificationBinding

class MockNotificationsAdapter : ListAdapter<MockNotification, MockNotificationsAdapter.ViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: MockNotification) {
            binding.tvNotificationTitle.text = notification.title
            binding.tvNotificationMessage.text = notification.message
            binding.tvNotificationTime.text = notification.time
            
            // Unread indicator
            binding.vUnreadIndicator.isVisible = !notification.isRead
            
            // Icon based on type
            val iconRes = when (notification.type) {
                NotificationType.COMMENT -> R.drawable.ic_comment_24
                NotificationType.REPLY -> R.drawable.ic_comment_24
                NotificationType.UPVOTE -> R.drawable.ic_upvote_24
                NotificationType.STATUS_UPDATE -> R.drawable.ic_status_update_24
                NotificationType.RESOLVED -> R.drawable.ic_check_circle_24
            }
            binding.ivNotificationIcon.setImageResource(iconRes)
            
            // Background tint for icon
            val tintColor = when (notification.type) {
                NotificationType.COMMENT, NotificationType.REPLY -> 
                    android.graphics.Color.parseColor("#E3F2FD") // Light blue
                NotificationType.UPVOTE -> 
                    android.graphics.Color.parseColor("#E8F5E9") // Light green
                NotificationType.STATUS_UPDATE -> 
                    android.graphics.Color.parseColor("#FFF3E0") // Light orange
                NotificationType.RESOLVED -> 
                    android.graphics.Color.parseColor("#E0F2F1") // Light teal
            }
            binding.ivNotificationIcon.setBackgroundColor(tintColor)
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<MockNotification>() {
    override fun areItemsTheSame(oldItem: MockNotification, newItem: MockNotification): Boolean {
        return oldItem.title == newItem.title && oldItem.time == newItem.time
    }

    override fun areContentsTheSame(oldItem: MockNotification, newItem: MockNotification): Boolean {
        return oldItem == newItem
    }
}
