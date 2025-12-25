package io.github.thwisse.kentinsesi.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.databinding.ItemCommentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class CommentAdapter : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment) {
            binding.tvAuthorName.text = comment.authorName
            binding.tvCommentText.text = comment.text

            // Tarihi formatla (Örn: 12 May, 14:30)
            val date = comment.createdAt
            if (date != null) {
                val format = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                binding.tvCommentDate.text = format.format(date)
            } else {
                binding.tvCommentDate.text = "Az önce"
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean = false // ID'si tam oturmadığı için false diyebiliriz veya content kontrolü
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean = oldItem == newItem
    }
}