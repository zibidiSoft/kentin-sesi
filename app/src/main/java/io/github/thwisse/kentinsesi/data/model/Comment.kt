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
    
    // Parent Post ID (Needed for navigation and deletion from profile)
    var postId: String = "",

    // Soft Delete fields - VAR olmalı çünkü Firestore toObject() setter gerektirir
    var isDeleted: Boolean = false,
    var deletedBy: String? = null, // "user" or "admin"
    
    @ServerTimestamp
    val createdAt: Date? = null
)