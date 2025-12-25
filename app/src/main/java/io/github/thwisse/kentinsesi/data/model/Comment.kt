package io.github.thwisse.kentinsesi.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "", // Yazan kişinin adı (DB'den çekmek yerine buraya kaydetmek pratik olur)
    val text: String = "",

    @ServerTimestamp
    val createdAt: Date? = null
)