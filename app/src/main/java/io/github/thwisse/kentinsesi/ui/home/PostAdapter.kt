package io.github.thwisse.kentinsesi.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.github.thwisse.kentinsesi.R // R sınıfını import etmeyi unutma
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.databinding.ItemPostBinding

class PostAdapter(
    private val currentUserId: String, // YENİ: Kullanıcı ID'si
    private val onUpvoteClick: (Post) -> Unit // YENİ: Tıklama Fonksiyonu
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.apply {
                tvTitle.text = post.title
                tvDescription.text = post.description
                tvCategory.text = post.category
                tvUpvoteCount.text = "${post.upvoteCount} Destek"

                // Durum metni
                tvStatus.text = when(post.status) {
                    "new" -> "Yeni"
                    "in_progress" -> "İşlemde"
                    "resolved" -> "Çözüldü"
                    else -> post.status
                }

                // Resim yükleme
                ivPostImage.load(post.imageUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.progress_indeterminate_horizontal)
                }

                // --- OYLAMA MANTIĞI (YENİ) ---

                // 1. Kullanıcı bu postu daha önce beğenmiş mi?
                val isUpvotedByMe = post.upvotedBy.contains(currentUserId)

                // 2. İkonu buna göre ayarla
                if (isUpvotedByMe) {
                    // Dolu kalp (Kırmızı)
                    ivUpvote.setImageResource(R.drawable.ic_heart_filled)
                    // İstersen rengini buradan da verebilirsin ama drawable daha temizdir
                } else {
                    // Boş kalp (Çerçeve)
                    ivUpvote.setImageResource(R.drawable.ic_heart_outlined)
                }

                // 3. Tıklanınca Fragment'a haber ver
                ivUpvote.setOnClickListener {
                    onUpvoteClick(post)
                }
                // -----------------------------
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
    }
}