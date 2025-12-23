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
        longitude: Double
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

            // 3. Yüklenen fotoğrafın indirme linkini (URL) al
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // 4. Firestore'a kaydedilecek Post nesnesini hazırla
            val newPost = Post(
                authorId = currentUser.uid,
                title = title,
                description = description,
                category = category,
                imageUrl = downloadUrl,
                location = GeoPoint(latitude, longitude),
                status = "new",       // Yeni post varsayılan olarak "yeni" durumundadır
                upvoteCount = 0,
                createdAt = null      // Firestore sunucusu burayı otomatik dolduracak (@ServerTimestamp)
            )

            // 5. Veriyi Firestore 'posts' koleksiyonuna ekle
            firestore.collection("posts").add(newPost).await()

            // İşlem başarılı
            Resource.Success(Unit)

        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Post oluşturulurken bilinmeyen bir hata oluştu.")
        }
    }

    override suspend fun getPosts(): Resource<List<Post>> {
        return try {
            // "posts" koleksiyonuna git
            // createdAt tarihine göre tersten sırala (En yeni en üstte)
            val snapshot = firestore.collection("posts")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            // Gelen dokümanları Post nesnesine çevir ve listele
            val postList = snapshot.toObjects(Post::class.java)

            Resource.Success(postList)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Veriler alınırken hata oluştu.")
        }
    }
}