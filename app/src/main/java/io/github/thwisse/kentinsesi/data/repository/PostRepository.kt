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
        longitude: Double
    ): Resource<Unit>

    // YENİ EKLENEN: Tüm postları getir
    suspend fun getPosts(): Resource<List<io.github.thwisse.kentinsesi.data.model.Post>>
}