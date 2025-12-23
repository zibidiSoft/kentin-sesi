package io.github.thwisse.kentinsesi.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.databinding.ItemPostBinding

class PostAdapter : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.apply {
                tvTitle.text = post.title
                tvDescription.text = post.description
                tvCategory.text = post.category
                tvUpvoteCount.text = "${post.upvoteCount} Destek"
                tvStatus.text = when(post.status) {
                    "new" -> "Yeni"
                    "in_progress" -> "İşlemde"
                    "resolved" -> "Çözüldü"
                    else -> post.status
                }

                // Coil ile resim yükleme
                ivPostImage.load(post.imageUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.progress_indeterminate_horizontal)
                }
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.authorId == newItem.authorId // Gerçekte postId olmalı
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
    }
}