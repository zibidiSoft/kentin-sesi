package io.github.thwisse.kentinsesi.data.repository

import android.net.Uri
import io.github.thwisse.kentinsesi.util.Resource

interface PostRepository {
    // Fotoğraf (Uri) ve diğer bilgileri alıp işlem sonucunu (Resource) döndürür.
    suspend fun createPost(
        imageUri: Uri,
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        district: String // YENİ
    ): Resource<Unit>

    // YENİ EKLENEN: Tüm postları getir
    suspend fun getPosts(
        districts: List<String>? = null,
        categories: List<String>? = null,
        statuses: List<String>? = null
    ): Resource<List<io.github.thwisse.kentinsesi.data.model.Post>>

    // Post ID ve Kullanıcı ID alır
    suspend fun toggleUpvote(postId: String, userId: String): Resource<Unit>

    // ... diğer fonksiyonların altına ...

    suspend fun getComments(postId: String): Resource<List<io.github.thwisse.kentinsesi.data.model.Comment>>

    suspend fun addComment(postId: String, text: String): Resource<Unit>

    suspend fun getThreadedComments(postId: String): Resource<List<io.github.thwisse.kentinsesi.data.model.Comment>>

    suspend fun addReply(
        postId: String,
        text: String,
        parentCommentId: String,
        replyToAuthorId: String?,
        replyToAuthorFullName: String?
    ): Resource<Unit>

    suspend fun updatePostStatus(postId: String, newStatus: String): Resource<Unit>
    suspend fun deletePost(postId: String): Resource<Unit>

    suspend fun getUserPosts(userId: String): Resource<List<io.github.thwisse.kentinsesi.data.model.Post>>
    
    // Post ID'ye göre tek bir post getir (State restore için)
    suspend fun getPostById(postId: String): Resource<io.github.thwisse.kentinsesi.data.model.Post>
}