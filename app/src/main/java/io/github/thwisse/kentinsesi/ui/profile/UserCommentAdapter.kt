package io.github.thwisse.kentinsesi.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.databinding.ItemUserCommentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class UserCommentAdapter(
    private val onItemClick: (Comment) -> Unit,
    private val onItemLongClick: ((Comment) -> Unit)? = null
) : ListAdapter<Comment, UserCommentAdapter.UserCommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserCommentViewHolder {
        val binding = ItemUserCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserCommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserCommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserCommentViewHolder(
        private val binding: ItemUserCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Comment) {
            val context = binding.root.context

            // Comment text & Soft Delete UI
            if (comment.isDeleted) {
                binding.tvCommentText.setTypeface(null, android.graphics.Typeface.ITALIC)
                binding.tvCommentText.setTextColor(android.graphics.Color.GRAY)
                
                binding.tvCommentText.text = if (comment.deletedBy == "admin") {
                    context.getString(R.string.comment_deleted_by_admin)
                } else {
                    context.getString(R.string.comment_deleted_by_user)
                }
                
                // Silinmişse tıklama ve uzun tıklama iptal (isteğe bağlı sadece uzun tıklama iptal)
                // Detaya gitmek için tıklama kalabilir, ancak silme işlemi yapılmasın.
                binding.root.setOnLongClickListener(null)
            } else {
                binding.tvCommentText.setTypeface(null, android.graphics.Typeface.NORMAL)
                // Sabit koyu renk kullan
                binding.tvCommentText.setTextColor(android.graphics.Color.parseColor("#333333"))

                binding.tvCommentText.text = comment.text
                
                // Uzun basma -> Silme
                binding.root.setOnLongClickListener { 
                    onItemLongClick?.invoke(comment)
                    true
                }
            }

            // Date
            val date = comment.createdAt
            if (date != null) {
                val format = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                binding.tvCommentDate.text = format.format(date)
            } else {
                binding.tvCommentDate.text = context.getString(R.string.just_now)
            }

            // Click listener
            binding.root.setOnClickListener {
                onItemClick(comment)
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.id.isNotBlank() && newItem.id.isNotBlank() && oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }
}
