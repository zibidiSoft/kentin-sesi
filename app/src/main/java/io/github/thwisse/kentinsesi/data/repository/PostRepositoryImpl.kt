package io.github.thwisse.kentinsesi.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : PostRepository {

    override suspend fun createPost(
        imageUri: Uri,
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        district: String // Parametre eklendi
    ): Resource<Unit> {
        return try {
            // 1. Kullanıcı giriş yapmış mı kontrol et
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Resource.Error("Kullanıcı oturumu bulunamadı.")
            }

            // 2. Fotoğrafı Firebase Storage'a yükle
            // Dosya adı benzersiz olmalı (UUID kullanıyoruz)
            val imageFileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("post_images/$imageFileName")

            // Yükleme işlemini başlat ve bitmesini bekle (.await())
            storageRef.putFile(imageUri).await()

            val downloadUrl = storageRef.downloadUrl.await().toString()

            val newPost = Post(
                authorId = currentUser.uid,
                title = title,
                description = description,
                category = category,
                imageUrl = downloadUrl,
                location = GeoPoint(latitude, longitude),
                district = district, // YENİ: Veritabanına yazıyoruz
                status = "new",
                upvoteCount = 0
            )

            firestore.collection("posts").add(newPost).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Hata")
        }
    }

    override suspend fun toggleUpvote(postId: String, userId: String): Resource<Unit> {
        return try {
            val postRef = firestore.collection("posts").document(postId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)

                // Mevcut beğeni listesini ve sayısını al
                val upvotedBy = snapshot.get("upvotedBy") as? List<String> ?: emptyList()
                val currentCount = snapshot.getLong("upvoteCount") ?: 0

                if (upvotedBy.contains(userId)) {
                    // Zaten beğenmiş -> Beğeniyi Geri Al
                    transaction.update(postRef, "upvotedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                    transaction.update(postRef, "upvoteCount", currentCount - 1)
                } else {
                    // Henüz beğenmemiş -> Beğeni Ekle
                    transaction.update(postRef, "upvotedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                    transaction.update(postRef, "upvoteCount", currentCount + 1)
                }
            }.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "İşlem başarısız")
        }
    }

    override suspend fun getPosts(
        district: String?,
        category: String?,
        status: String?
    ): Resource<List<Post>> {
        return try {
            // Temel sorgu: 'posts' koleksiyonu
            var query = firestore.collection("posts") as com.google.firebase.firestore.Query

            // --- İLÇE FİLTRESİ ARTIK ÇALIŞACAK ---
            if (!district.isNullOrEmpty()) {
                query = query.whereEqualTo("district", district)
            }
            // -------------------------------------

            if (!category.isNullOrEmpty()) {
                query = query.whereEqualTo("category", category)
            }

            if (!status.isNullOrEmpty()) {
                query = query.whereEqualTo("status", status)
            }

            // Sıralama: En yeni en üstte
            query = query.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

            val snapshot = query.get().await()
            // BU KISMI DEĞİŞTİRİYORUZ:
            val postList = snapshot.documents.map { doc ->
                val post = doc.toObject(Post::class.java)!!
                post.copy(id = doc.id) // Doküman ID'sini modele kopyalıyoruz!
            }

            Resource.Success(postList)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Veriler alınırken hata oluştu.")
        }
    }
}