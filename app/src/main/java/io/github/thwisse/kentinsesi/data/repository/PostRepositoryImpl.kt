package io.github.thwisse.kentinsesi.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import io.github.thwisse.kentinsesi.data.model.Comment // <-- 1. EKSİK IMPORT EKLENDİ
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
        district: String
    ): Resource<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Resource.Error("Kullanıcı oturumu bulunamadı.")
            }

            val imageFileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("post_images/$imageFileName")

            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val newPost = Post(
                authorId = currentUser.uid,
                title = title,
                description = description,
                category = category,
                imageUrl = downloadUrl,
                location = GeoPoint(latitude, longitude),
                district = district,
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

                val upvotedBy = snapshot.get("upvotedBy") as? List<String> ?: emptyList()
                val currentCount = snapshot.getLong("upvoteCount") ?: 0

                if (upvotedBy.contains(userId)) {
                    transaction.update(postRef, "upvotedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                    transaction.update(postRef, "upvoteCount", currentCount - 1)
                } else {
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
            var query = firestore.collection("posts") as com.google.firebase.firestore.Query

            if (!district.isNullOrEmpty()) {
                query = query.whereEqualTo("district", district)
            }

            if (!category.isNullOrEmpty()) {
                query = query.whereEqualTo("category", category)
            }

            if (!status.isNullOrEmpty()) {
                query = query.whereEqualTo("status", status)
            }

            query = query.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

            val snapshot = query.get().await()
            val postList = snapshot.documents.map { doc ->
                val post = doc.toObject(Post::class.java)!!
                post.copy(id = doc.id)
            }

            Resource.Success(postList)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Veriler alınırken hata oluştu.")
        }
    }

    override suspend fun getComments(postId: String): Resource<List<Comment>> {
        return try {
            val snapshot = firestore.collection("posts").document(postId)
                .collection("comments")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()

            val comments = snapshot.toObjects(Comment::class.java) // Artık hata vermez (Import eklendi)
            Resource.Success(comments)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Yorumlar alınamadı.")
        }
    }

    override suspend fun addComment(postId: String, text: String): Resource<Unit> {
        return try {
            // 2. DÜZELTME: 'currentUser' yerine 'auth.currentUser' yazıldı.
            val user = auth.currentUser ?: throw Exception("Oturum açılmamış.")

            val authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Anonim"

            val comment = Comment(
                postId = postId,
                authorId = user.uid,
                authorName = authorName,
                text = text
            )

            firestore.collection("posts").document(postId)
                .collection("comments")
                .add(comment)
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Yorum gönderilemedi.")
        }
    }

    override suspend fun updatePostStatus(postId: String, newStatus: String): Resource<Unit> {
        return try {
            firestore.collection("posts").document(postId)
                .update("status", newStatus)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Güncelleme başarısız.")
        }
    }

    override suspend fun deletePost(postId: String): Resource<Unit> {
        return try {
            // 1. Önce Firestore'dan sil
            firestore.collection("posts").document(postId).delete().await()

            // 2. (Opsiyonel ama iyi olur) Storage'dan resmi de silmek gerekir.
            // Bunun için postu çekerken imagePath'i de kaydetmemiz gerekirdi.
            // Şimdilik sadece veritabanından silelim, storage temizliği ilerde yapılır.

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Silme başarısız.")
        }
    }

}