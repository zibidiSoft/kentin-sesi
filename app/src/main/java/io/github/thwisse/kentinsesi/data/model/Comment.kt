package io.github.thwisse.kentinsesi.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    @DocumentId
    val id: String = "",
    val authorId: String = "",
    val authorFullName: String = "",
    val authorUsername: String = "",
    val authorCity: String = "",
    val authorDistrict: String = "",
    val authorTitle: String = "",
    val text: String = "",

    // Reply alanları
    val parentCommentId: String? = null, // Direct parent (null => top-level)
    val rootCommentId: String? = null, // Top-level comment id for the thread
    val depth: Int = 0, // 0=top-level, 1=reply, 2=reply-to-reply (max)
    val replyCount: Long = 0L, // Only meaningful for top-level comments
    val replyToAuthorId: String? = null,
    val replyToAuthorFullName: String? = null,
    val replyToAuthorUsername: String? = null,

    @ServerTimestamp
    val createdAt: Date? = null
)