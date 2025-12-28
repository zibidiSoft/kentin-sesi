package io.github.thwisse.kentinsesi.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.data.model.PostStatus
import io.github.thwisse.kentinsesi.data.model.User
import io.github.thwisse.kentinsesi.util.Constants
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
            // Constants kullanarak hard-coded string'den kurtulduk
            val storageRef = storage.reference.child("${Constants.STORAGE_POST_IMAGES}/$imageFileName")

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
                // Enum kullanarak tip güvenli hale getirdik
                status = PostStatus.NEW.value,
                // Post oluşturulduğunda yazarın kendi oyu otomatik +1 olarak ekleniyor
                upvoteCount = 1,
                upvotedBy = listOf(currentUser.uid), // Yazarın kendisi otomatik upvote ediyor
                commentCount = 0
            )

            // Constants kullanarak collection adını merkezileştirdik
            val docRef = firestore.collection(Constants.COLLECTION_POSTS).add(newPost).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Hata")
        }
    }

    override suspend fun toggleUpvote(postId: String, userId: String): Resource<Unit> {
        return try {
            val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)

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
        districts: List<String>?,
        categories: List<String>?,
        statuses: List<String>?
    ): Resource<List<Post>> {
        return try {
            val hasDistrictFilter = !districts.isNullOrEmpty()
            val hasCategoryFilter = !categories.isNullOrEmpty()
            val hasStatusFilter = !statuses.isNullOrEmpty()

            // Hiç filtre yoksa tüm postları çek
            if (!hasDistrictFilter && !hasCategoryFilter && !hasStatusFilter) {
                val snapshot = firestore.collection(Constants.COLLECTION_POSTS)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val postList = snapshot.documents.map { doc ->
                    val post = doc.toObject(Post::class.java)!!
                    // @DocumentId normalde id'yi doldurur; yine de boş gelirse doc.id ile dolduralım
                    if (post.id.isBlank()) post.copy(id = doc.id) else post
                }
                return Resource.Success(postList)
            }

            // Firestore'da whereIn kullanarak çoklu filtreleme yapıyoruz
            // Ancak Firestore'da aynı anda sadece bir whereIn kullanılabilir
            // Bu yüzden en kısıtlayıcı filtreyi Firestore'da uygulayıp, diğerlerini client-side'da filtreleyeceğiz
            
            var query = firestore.collection(Constants.COLLECTION_POSTS) as com.google.firebase.firestore.Query
            var postList: List<Post>

            // En kısıtlayıcı filtreyi seç (en az sonuç döndüren)
            when {
                // Sadece bir filtre varsa Firestore'da filtrele
                hasDistrictFilter && !hasCategoryFilter && !hasStatusFilter -> {
                    if (districts!!.size == 1) {
                        query = query.whereEqualTo("district", districts[0])
                    } else {
                        // whereIn maksimum 10 item alabilir, daha fazlası için client-side filtreleme
                        if (districts.size <= 10) {
                            query = query.whereIn("district", districts)
                        }
                    }
                    query = query.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    val snapshot = query.get().await()
                    postList = snapshot.documents.map { doc ->
                        val post = doc.toObject(Post::class.java)!!
                        if (post.id.isBlank()) post.copy(id = doc.id) else post
                    }
                    // whereIn 10'dan fazla item için kullanılamaz, client-side filtrele
                    if (districts.size > 10) {
                        postList = postList.filter { districts.contains(it.district) }
                    }
                }
                !hasDistrictFilter && hasCategoryFilter && !hasStatusFilter -> {
                    if (categories!!.size == 1) {
                        query = query.whereEqualTo("category", categories[0])
                    } else {
                        if (categories.size <= 10) {
                            query = query.whereIn("category", categories)
                        }
                    }
                    query = query.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    val snapshot = query.get().await()
                    postList = snapshot.documents.map { doc ->
                        val post = doc.toObject(Post::class.java)!!
                        if (post.id.isBlank()) post.copy(id = doc.id) else post
                    }
                    if (categories.size > 10) {
                        postList = postList.filter { categories.contains(it.category) }
                    }
                }
                !hasDistrictFilter && !hasCategoryFilter && hasStatusFilter -> {
                    if (statuses!!.size == 1) {
                        query = query.whereEqualTo("status", statuses[0])
                    } else {
                        if (statuses.size <= 10) {
                            query = query.whereIn("status", statuses)
                        }
                    }
                    query = query.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    val snapshot = query.get().await()
                    postList = snapshot.documents.map { doc ->
                        val post = doc.toObject(Post::class.java)!!
                        if (post.id.isBlank()) post.copy(id = doc.id) else post
                    }
                    if (statuses.size > 10) {
                        postList = postList.filter { statuses.contains(it.status) }
                    }
                }
                // Birden fazla filtre varsa - tümünü çek, client-side'da filtrele
                else -> {
                    val snapshot = firestore.collection(Constants.COLLECTION_POSTS)
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .get()
                        .await()
                    postList = snapshot.documents.map { doc ->
                        val post = doc.toObject(Post::class.java)!!
                        if (post.id.isBlank()) post.copy(id = doc.id) else post
                    }
                }
            }

            // Client-side filtreleme (birden fazla filtre varsa veya whereIn limiti aşıldıysa)
            if (hasDistrictFilter) {
                postList = postList.filter { post ->
                    districts!!.contains(post.district)
                }
            }
            
            if (hasCategoryFilter) {
                postList = postList.filter { post ->
                    categories!!.contains(post.category)
                }
            }
            
            if (hasStatusFilter) {
                postList = postList.filter { post ->
                    statuses!!.contains(post.status)
                }
            }

            Resource.Success(postList)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Veriler alınırken hata oluştu.")
        }
    }

    override suspend fun getComments(postId: String): Resource<List<Comment>> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()

            val comments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Comment::class.java)?.copy(id = doc.id)
            }
            Resource.Success(comments)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Yorumlar alınamadı.")
        }
    }

    override suspend fun getThreadedComments(postId: String): Resource<List<Comment>> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()

            val all = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Comment::class.java)?.copy(id = doc.id)
            }

            val maxDepth = Constants.MAX_COMMENT_DEPTH

            val topLevel = all.filter { it.parentCommentId.isNullOrBlank() || it.depth == 0 }
            val childrenByParent = all
                .filter { !it.parentCommentId.isNullOrBlank() && it.depth > 0 }
                .groupBy { it.parentCommentId!! }

            val flattened = mutableListOf<Comment>()
            fun appendChildren(parent: Comment) {
                val nextDepth = parent.depth + 1
                if (nextDepth > maxDepth) return
                val children = childrenByParent[parent.id].orEmpty()
                for (child in children) {
                    flattened.add(child)
                    appendChildren(child)
                }
            }

            for (c in topLevel) {
                flattened.add(c)
                appendChildren(c)
            }

            Resource.Success(flattened)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Yorumlar alınamadı.")
        }
    }

    override suspend fun addComment(postId: String, text: String): Resource<Unit> {
        return try {
            // 2. DÜZELTME: 'currentUser' yerine 'auth.currentUser' yazıldı.
            val user = auth.currentUser ?: throw Exception("Oturum açılmamış.")

            val userDoc = firestore.collection(Constants.COLLECTION_USERS).document(user.uid).get().await()
            val profile = userDoc.toObject(User::class.java)

            val fullName = profile?.fullName?.takeIf { it.isNotBlank() }
                ?: user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Anonim"

            val username = profile?.username?.trim().orEmpty()

            val city = profile?.city ?: ""
            val district = profile?.district ?: ""
            val title = profile?.title ?: ""

            val comment = Comment(
                authorId = user.uid,
                authorFullName = fullName,
                authorUsername = username,
                authorCity = city,
                authorDistrict = district,
                authorTitle = title,
                text = text
            )

            val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
            val commentsCol = postRef.collection(Constants.COLLECTION_COMMENTS)

            firestore.runTransaction { tx ->
                tx.set(commentsCol.document(), comment)
                tx.update(postRef, "commentCount", FieldValue.increment(1))
            }.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Yorum gönderilemedi.")
        }
    }

    override suspend fun addReply(
        postId: String,
        text: String,
        parentCommentId: String,
        replyToAuthorId: String?,
        replyToAuthorFullName: String?
    ): Resource<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("Oturum açılmamış.")

            val userDoc = firestore.collection(Constants.COLLECTION_USERS).document(user.uid).get().await()
            val profile = userDoc.toObject(User::class.java)

            val fullName = profile?.fullName?.takeIf { it.isNotBlank() }
                ?: user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Anonim"

            val username = profile?.username?.trim().orEmpty()

            val city = profile?.city ?: ""
            val district = profile?.district ?: ""
            val title = profile?.title ?: ""

            val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
            val commentsCol = postRef.collection(Constants.COLLECTION_COMMENTS)

            val parentDoc = commentsCol.document(parentCommentId).get().await()
            if (!parentDoc.exists()) {
                return Resource.Error("Yanıtlanacak yorum bulunamadı")
            }

            val parent = parentDoc.toObject(Comment::class.java)?.copy(id = parentDoc.id)
                ?: return Resource.Error("Yanıtlanacak yorum okunamadı")

            if (parent.depth >= Constants.MAX_COMMENT_DEPTH) {
                return Resource.Error("Maksimum yanıt derinliğine ulaşıldı")
            }

            val rootId = if (parent.depth == 0) {
                parent.id
            } else {
                parent.rootCommentId ?: parent.id
            }

            val replyToFullNameResolved = replyToAuthorFullName?.takeIf { it.isNotBlank() }
                ?: parent.authorFullName.takeIf { it.isNotBlank() }

            val replyToUsernameResolved = parent.authorUsername.takeIf { it.isNotBlank() }

            val reply = Comment(
                authorId = user.uid,
                authorFullName = fullName,
                authorUsername = username,
                authorCity = city,
                authorDistrict = district,
                authorTitle = title,
                text = text,
                parentCommentId = parent.id,
                rootCommentId = rootId,
                depth = parent.depth + 1,
                replyToAuthorId = replyToAuthorId,
                replyToAuthorFullName = replyToFullNameResolved,
                replyToAuthorUsername = replyToUsernameResolved
            )

            val rootRef = commentsCol.document(rootId)

            firestore.runTransaction { tx ->
                // reply write
                tx.set(commentsCol.document(), reply)
                // denormalized reply count on root comment
                tx.update(rootRef, "replyCount", FieldValue.increment(1))
                // denormalized total comment count on post
                tx.update(postRef, "commentCount", FieldValue.increment(1))
            }.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Yanıt gönderilemedi.")
        }
    }

    override suspend fun updatePostStatus(postId: String, newStatus: String): Resource<Unit> {
        return try {
            // Status'u enum'a çevirerek geçerliliğini kontrol ediyoruz
            val statusEnum = PostStatus.fromString(newStatus)
            firestore.collection(Constants.COLLECTION_POSTS).document(postId)
                .update("status", statusEnum.value)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Güncelleme başarısız.")
        }
    }

    override suspend fun deletePost(postId: String): Resource<Unit> {
        return try {
            // 1. Önce Firestore'dan sil
            firestore.collection(Constants.COLLECTION_POSTS).document(postId).delete().await()

            // 2. (Opsiyonel ama iyi olur) Storage'dan resmi de silmek gerekir.
            // Bunun için postu çekerken imagePath'i de kaydetmemiz gerekirdi.
            // Şimdilik sadece veritabanından silelim, storage temizliği ilerde yapılır.

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Silme başarısız.")
        }
    }

    override suspend fun getUserPosts(userId: String): Resource<List<Post>> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_POSTS)
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val postList = snapshot.documents.map { doc ->
                val post = doc.toObject(Post::class.java)!!
                if (post.id.isBlank()) post.copy(id = doc.id) else post
            }
            Resource.Success(postList)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Veriler alınamadı.")
        }
    }
    
    override suspend fun getPostById(postId: String): Resource<Post> {
        return try {
            val document = firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .get()
                .await()
            
            if (document.exists()) {
                val post = document.toObject(Post::class.java)
                if (post != null) {
                    Resource.Success(if (post.id.isBlank()) post.copy(id = document.id) else post)
                } else {
                    Resource.Error("Post verisi okunamadı.")
                }
            } else {
                Resource.Error("Post bulunamadı.")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Post alınamadı.")
        }
    }
    
    override suspend fun getUserComments(userId: String): Resource<List<Comment>> {
        return try {
            // Collection group query - tüm posts altındaki comments subcollection'larını tarar
            val snapshot = firestore.collectionGroup(Constants.COLLECTION_COMMENTS)
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            val comments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Comment::class.java)?.copy(id = doc.id)
            }
            Resource.Success(comments)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Yorumlar alınamadı.")
        }
    }
}