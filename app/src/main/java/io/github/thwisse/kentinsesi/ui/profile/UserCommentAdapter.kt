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
    private val onItemClick: (Comment) -> Unit
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

            // Comment text
            binding.tvCommentText.text = comment.text

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
