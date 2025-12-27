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
    private val onRepliesToggleClick: ((Comment) -> Unit)? = null
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
            val fullName = comment.authorFullName.ifBlank { "Anonim" }
            val username = comment.authorUsername.trim().takeIf { it.isNotBlank() }
            binding.tvAuthorFullName.text = if (username != null) {
                "$fullName (@$username)"
            } else {
                fullName
            }
            binding.tvCommentText.text = comment.text

            val location = listOf(comment.authorCity, comment.authorDistrict)
                .filter { it.isNotBlank() }
                .joinToString("/")
            val title = comment.authorTitle

            binding.tvAuthorMeta.text = buildString {
                if (location.isNotBlank()) append(location)
                if (location.isNotBlank() && title.isNotBlank()) append(" • ")
                if (title.isNotBlank()) append(title)
            }.ifBlank { " " }

            val replyTo = comment.replyToAuthorFullName?.takeIf { it.isNotBlank() }

            binding.tvReplyingTo.isVisible = !replyTo.isNullOrBlank()
            binding.tvReplyingTo.text = if (!replyTo.isNullOrBlank()) {
                "${replyTo} kişisine yanıt"
            } else {
                ""
            }

            // Tarihi formatla (Örn: 12 May, 14:30)
            val date = comment.createdAt
            if (date != null) {
                val format = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                binding.tvCommentDate.text = format.format(date)
            } else {
                binding.tvCommentDate.text = "Az önce"
            }

            val density = binding.root.resources.displayMetrics.density

            val baseHorizontalMargin = if (comment.depth <= 0) 0 else (12 * density).toInt()
            val baseVerticalMargin = (6 * density).toInt()
            val indent = (comment.depth.coerceIn(0, Constants.MAX_COMMENT_DEPTH) * 12 * density).toInt()

            val depth = comment.depth.coerceIn(0, 4)
            val bg = when (depth) {
                0 -> Color.parseColor("#E3F2FD") // pastel mavi
                1 -> Color.parseColor("#FFEBEE") // pastel kirmizi
                2 -> Color.parseColor("#E8F5E9") // pastel yesil
                3 -> Color.parseColor("#F3E5F5") // pastel mor
                else -> Color.parseColor("#FFFDE7") // pastel sari
            }
            binding.root.setCardBackgroundColor(bg)

            // Kartın kendisini sağa kaydır (kart daha küçük görünür), içerik padding'ini değil.
            val lp = binding.root.layoutParams
            if (lp is MarginLayoutParams) {
                lp.marginStart = baseHorizontalMargin + indent
                lp.marginEnd = 0
                lp.topMargin = baseVerticalMargin
                lp.bottomMargin = baseVerticalMargin
                binding.root.layoutParams = lp
            }

            val count = childCountByParentId[comment.id] ?: 0

            // Sol alanın genişliği sabit kalsın: reply yoksa INVISIBLE (GONE değil)
            binding.tvRepliesToggle.visibility = if (count > 0) View.VISIBLE else View.INVISIBLE
            binding.tvRepliesToggle.text = "$count yanıt"
            binding.tvRepliesToggle.setOnClickListener {
                if (count > 0) onRepliesToggleClick?.invoke(comment)
            }

            val canReply = comment.depth < Constants.MAX_COMMENT_DEPTH

            // Kart click: aç/kapat
            binding.root.isEnabled = true
            binding.root.setOnClickListener {
                if (count > 0) onRepliesToggleClick?.invoke(comment)
            }

            // Alt sağ "Yanıtla": reply mode
            binding.tvReplyAction.isEnabled = canReply
            binding.tvReplyAction.alpha = if (canReply) 1f else 0.5f
            binding.tvReplyAction.setOnClickListener {
                if (canReply) onCommentClick?.invoke(comment)
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