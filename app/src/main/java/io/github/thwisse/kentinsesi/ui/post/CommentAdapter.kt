package io.github.thwisse.kentinsesi.ui.post

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.databinding.ItemCommentBinding
import io.github.thwisse.kentinsesi.util.Constants
import java.text.SimpleDateFormat
import java.util.Locale

class CommentAdapter(
    private val onCommentClick: ((Comment) -> Unit)? = null,
    private val onRepliesToggleClick: ((Comment) -> Unit)? = null,
    private val onCommentLongClick: ((Comment) -> Unit)? = null
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    private var expandedCommentIds: Set<String> = emptySet()
    private var childCountByParentId: Map<String, Int> = emptyMap()

    fun setExpandedCommentIds(ids: Set<String>) {
        expandedCommentIds = ids
        notifyDataSetChanged()
    }

    fun setChildCountByParentId(map: Map<String, Int>) {
        childCountByParentId = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment) {
            val context = binding.root.context
            
            // Yazar bilgisi
            val fullName = comment.authorFullName.ifBlank {
                context.getString(io.github.thwisse.kentinsesi.R.string.anonymous_user)
            }
            val username = comment.authorUsername.trim().takeIf { it.isNotBlank() }
            binding.tvAuthorFullName.text = if (username != null) {
                "$fullName (@$username)"
            } else {
                fullName
            }

            // Metin ve Silindi Durumu Kontrolü
            if (comment.isDeleted) {
                binding.tvCommentText.setTypeface(null, android.graphics.Typeface.ITALIC)
                binding.tvCommentText.setTextColor(Color.GRAY)
                
                binding.tvCommentText.text = if (comment.deletedBy == "admin") {
                    context.getString(io.github.thwisse.kentinsesi.R.string.comment_deleted_by_admin)
                } else {
                    context.getString(io.github.thwisse.kentinsesi.R.string.comment_deleted_by_user)
                }
                
                // Silinmişse yanıtla butonu gizle
                binding.tvReplyAction.visibility = View.GONE
                binding.root.setOnLongClickListener(null)
            } else {
                binding.tvCommentText.setTypeface(null, android.graphics.Typeface.NORMAL)
                // Sabit koyu renk kullan - tema rengine güvenme
                binding.tvCommentText.setTextColor(Color.parseColor("#333333"))

                binding.tvCommentText.text = comment.text
                
                // Debug log
                android.util.Log.d("CommentAdapter", "Comment ID: ${comment.id}, Text: '${comment.text}', isDeleted: ${comment.isDeleted}")
                
                // Yanıtla butonu
                binding.tvReplyAction.visibility = View.VISIBLE
                
                // Uzun basma -> Silme
                binding.root.setOnLongClickListener { 
                    onCommentLongClick?.invoke(comment)
                    true
                }
            }

            // Konum ve Unvan
            val location = listOf(comment.authorCity, comment.authorDistrict)
                .filter { it.isNotBlank() }
                .joinToString("/")
            val title = comment.authorTitle
            binding.tvAuthorMeta.text = buildString {
                if (location.isNotBlank()) append(location)
                if (location.isNotBlank() && title.isNotBlank()) append(" • ")
                if (title.isNotBlank()) append(title)
            }.ifBlank { " " }

            // Reply To
            val replyTo = comment.replyToAuthorFullName?.takeIf { it.isNotBlank() }
            binding.tvReplyingTo.isVisible = !replyTo.isNullOrBlank()
            binding.tvReplyingTo.text = if (!replyTo.isNullOrBlank()) {
                context.getString(io.github.thwisse.kentinsesi.R.string.reply_to_person, replyTo)
            } else {
                ""
            }

            // Tarih
            val date = comment.createdAt
            if (date != null) {
                val format = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                binding.tvCommentDate.text = format.format(date)
            } else {
                binding.tvCommentDate.text = context.getString(io.github.thwisse.kentinsesi.R.string.just_now)
            }

            // Girintileme (Indentation) ve Arka Plan Rengi
            val density = binding.root.resources.displayMetrics.density
            val baseHorizontalMargin = if (comment.depth <= 0) 0 else (12 * density).toInt()
            val baseVerticalMargin = (6 * density).toInt()
            val indent = (comment.depth.coerceIn(0, Constants.MAX_COMMENT_DEPTH) * 12 * density).toInt()
            
            val depth = comment.depth.coerceIn(0, 4)
            val bg = when (depth) {
                0 -> Color.parseColor("#E3F2FD")
                1 -> Color.parseColor("#FFEBEE")
                2 -> Color.parseColor("#E8F5E9")
                3 -> Color.parseColor("#F3E5F5")
                else -> Color.parseColor("#FFFDE7")
            }
            binding.root.setCardBackgroundColor(bg)

            val lp = binding.root.layoutParams
            if (lp is MarginLayoutParams) {
                lp.marginStart = baseHorizontalMargin + indent
                lp.marginEnd = 0
                lp.topMargin = baseVerticalMargin
                lp.bottomMargin = baseVerticalMargin
                binding.root.layoutParams = lp
            }

            // Alt yorum sayısı toggle
            val count = childCountByParentId[comment.id] ?: 0
            binding.tvRepliesToggle.visibility = if (count > 0) View.VISIBLE else View.INVISIBLE
            binding.tvRepliesToggle.text = context.getString(io.github.thwisse.kentinsesi.R.string.replies_count, count)
            binding.tvRepliesToggle.setOnClickListener {
                if (count > 0) onRepliesToggleClick?.invoke(comment)
            }

            // Yanıtla butonu mantığı (silinmemişse)
            if (!comment.isDeleted) {
                val canReply = comment.depth < Constants.MAX_COMMENT_DEPTH
                binding.tvReplyAction.isEnabled = canReply
                binding.tvReplyAction.alpha = if (canReply) 1f else 0.5f
                binding.tvReplyAction.setOnClickListener {
                    if (canReply) onCommentClick?.invoke(comment)
                }
            } else {
                // Silinmişse zaten visible GONE yapıldı, ama yine de listener/enable temizle
                binding.tvReplyAction.setOnClickListener(null)
            }

            // Kart click: aç/kapat (her durumda çalışsın)
            binding.root.setOnClickListener {
                if (count > 0) onRepliesToggleClick?.invoke(comment)
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.id.isNotBlank() && newItem.id.isNotBlank() && oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean = oldItem == newItem
    }
}