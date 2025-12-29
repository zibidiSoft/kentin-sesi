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

            // Post oluştur
            val docRef = firestore.collection(Constants.COLLECTION_POSTS).add(newPost).await()
            val postId = docRef.id
            
            // Kullanıcı bilgilerini çek (ilk status update için)
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUser.uid)
                .get()
                .await()
            val profile = userDoc.toObject(User::class.java)
            
            // Otomatik ilk status update ekle
            val initialUpdate = hashMapOf(
                "postId" to postId,
                "status" to "new",
                "note" to "Paylaşım yapıldı",
                "authorId" to currentUser.uid,
                "authorFullName" to (profile?.fullName ?: ""),
                "authorUsername" to (profile?.username ?: ""),
                "createdAt" to FieldValue.serverTimestamp()
            )
            
            docRef.collection("statusUpdates").add(initialUpdate).await()
            
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
            android.util.Log.d("GetComments", "Fetching comments for postId: $postId")
            
            val snapshot = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()

            android.util.Log.d("GetComments", "Got ${snapshot.documents.size} documents from Firestore")
            
            val comments = snapshot.documents.mapNotNull { doc ->
                // Raw data log
                val rawData = doc.data
                android.util.Log.d("GetComments", "Doc ${doc.id} raw data: isDeleted=${rawData?.get("isDeleted")}, deletedBy=${rawData?.get("deletedBy")}")
                
                val comment = doc.toObject(Comment::class.java)?.copy(id = doc.id)
                
                // Parsed object log
                android.util.Log.d("GetComments", "Doc ${doc.id} parsed: isDeleted=${comment?.isDeleted}, deletedBy=${comment?.deletedBy}")
                
                comment
            }
            
            android.util.Log.d("GetComments", "Returning ${comments.size} comments")
            Resource.Success(comments)
        } catch (e: Exception) {
            android.util.Log.e("GetComments", "Error: ${e.message}")
            Resource.Error(e.message ?: "Yorumlar alınamadı.")
        }
    }

    override suspend fun getThreadedComments(postId: String): Resource<List<Comment>> {
        return try {
            android.util.Log.d("GetThreadedComments", "Fetching threaded comments for postId: $postId")
            
            val snapshot = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()

            android.util.Log.d("GetThreadedComments", "Got ${snapshot.documents.size} documents from server")

            val all = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                // Manuel mapping - toObject() isDeleted alanını düzgün parse etmiyor
                val comment = Comment(
                    id = doc.id,
                    authorId = data["authorId"] as? String ?: "",
                    authorFullName = data["authorFullName"] as? String ?: "",
                    authorUsername = data["authorUsername"] as? String ?: "",
                    authorCity = data["authorCity"] as? String ?: "",
                    authorDistrict = data["authorDistrict"] as? String ?: "",
                    authorTitle = data["authorTitle"] as? String ?: "",
                    text = data["text"] as? String ?: "",
                    parentCommentId = data["parentCommentId"] as? String,
                    rootCommentId = data["rootCommentId"] as? String,
                    depth = (data["depth"] as? Long)?.toInt() ?: 0,
                    replyCount = data["replyCount"] as? Long ?: 0L,
                    replyToAuthorId = data["replyToAuthorId"] as? String,
                    replyToAuthorFullName = data["replyToAuthorFullName"] as? String,
                    replyToAuthorUsername = data["replyToAuthorUsername"] as? String,
                    postId = postId,
                    isDeleted = data["isDeleted"] as? Boolean ?: false,
                    deletedBy = data["deletedBy"] as? String,
                    createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                )
                
                android.util.Log.d("GetThreadedComments", "Parsed ${doc.id}: isDeleted=${comment.isDeleted}, deletedBy=${comment.deletedBy}")
                comment
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

            android.util.Log.d("GetThreadedComments", "Returning ${flattened.size} comments")
            Resource.Success(flattened)
        } catch (e: Exception) {
            android.util.Log.e("GetThreadedComments", "Error: ${e.message}")
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

            // Firestore'a yazılacak alanları explicit olarak belirt
            val commentData = hashMapOf(
                "authorId" to user.uid,
                "authorFullName" to fullName,
                "authorUsername" to username,
                "authorCity" to city,
                "authorDistrict" to district,
                "authorTitle" to title,
                "text" to text,
                "parentCommentId" to null,
                "rootCommentId" to null,
                "depth" to 0,
                "replyCount" to 0,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
            val commentsCol = postRef.collection(Constants.COLLECTION_COMMENTS)

            firestore.runTransaction { tx ->
                tx.set(commentsCol.document(), commentData)
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

            // Firestore'a yazılacak alanları explicit olarak belirt
            val replyData = hashMapOf<String, Any?>(
                "authorId" to user.uid,
                "authorFullName" to fullName,
                "authorUsername" to username,
                "authorCity" to city,
                "authorDistrict" to district,
                "authorTitle" to title,
                "text" to text,
                "parentCommentId" to parent.id,
                "rootCommentId" to rootId,
                "depth" to (parent.depth + 1),
                "replyCount" to 0,
                "replyToAuthorId" to replyToAuthorId,
                "replyToAuthorFullName" to replyToFullNameResolved,
                "replyToAuthorUsername" to replyToUsernameResolved,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val rootRef = commentsCol.document(rootId)

            firestore.runTransaction { tx ->
                // reply write
                tx.set(commentsCol.document(), replyData)
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
            // 1. Önce post verisini çek (resim URL'i için)
            // Hata olsa bile dokümanı silmeye devam edebiliriz, ama resmi silmek için URL lazım.
            val postSnapshot = firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .get()
                .await()
            
            val imageUrl = postSnapshot.getString("imageUrl")
            
            // 2. Eğer resim URL'i varsa Storage'dan sil
            if (!imageUrl.isNullOrBlank()) {
                try {
                    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                        .getReferenceFromUrl(imageUrl)
                    storageRef.delete().await()
                } catch (e: Exception) {
                    // Resim silinirken hata oluşursa logla ama postu silmeye devam et
                    e.printStackTrace()
                }
            }

            // 3. Firestore dokümanını sil
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .delete()
                .await()
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Post silinemedi.")
        }
    }
    
    override suspend fun deleteComment(postId: String, commentId: String, isAdmin: Boolean): Resource<Unit> {
        return try {
            val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
            val commentRef = postRef.collection(Constants.COLLECTION_COMMENTS).document(commentId)
            
            // Sadece admin rolü için "admin" yaz, official dahil diğerleri "user" olsun
            val deletedBy = if (isAdmin) "admin" else "user"
            
            // Soft delete: isDeleted=true yap ve commentCount'u azalt
            firestore.runTransaction { tx ->
                // Yorum güncelle
                tx.update(commentRef, mapOf(
                    "isDeleted" to true,
                    "deletedBy" to deletedBy
                ))
                // Post'un commentCount'unu azalt
                tx.update(postRef, "commentCount", FieldValue.increment(-1))
            }.await()
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Yorum silinemedi.")
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
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            
            val comments = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val parentPostId = doc.reference.parent.parent?.id ?: ""
                
                // Manuel mapping - toObject() isDeleted alanını düzgün parse etmiyor
                Comment(
                    id = doc.id,
                    authorId = data["authorId"] as? String ?: "",
                    authorFullName = data["authorFullName"] as? String ?: "",
                    authorUsername = data["authorUsername"] as? String ?: "",
                    authorCity = data["authorCity"] as? String ?: "",
                    authorDistrict = data["authorDistrict"] as? String ?: "",
                    authorTitle = data["authorTitle"] as? String ?: "",
                    text = data["text"] as? String ?: "",
                    parentCommentId = data["parentCommentId"] as? String,
                    rootCommentId = data["rootCommentId"] as? String,
                    depth = (data["depth"] as? Long)?.toInt() ?: 0,
                    replyCount = data["replyCount"] as? Long ?: 0L,
                    replyToAuthorId = data["replyToAuthorId"] as? String,
                    replyToAuthorFullName = data["replyToAuthorFullName"] as? String,
                    replyToAuthorUsername = data["replyToAuthorUsername"] as? String,
                    postId = parentPostId,
                    isDeleted = data["isDeleted"] as? Boolean ?: false,
                    deletedBy = data["deletedBy"] as? String,
                    createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                )
            }
            Resource.Success(comments)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Yorumlar alınamadı.")
        }
    }
    
    // ======================== STATUS UPDATES ========================
    
    override suspend fun getStatusUpdates(postId: String): Resource<List<io.github.thwisse.kentinsesi.data.model.StatusUpdate>> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection("statusUpdates")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get(com.google.firebase.firestore.Source.SERVER) // Her zaman fresh data
                .await()
            
            val updates = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                io.github.thwisse.kentinsesi.data.model.StatusUpdate(
                    id = doc.id,
                    postId = postId,
                    status = data["status"] as? String ?: "",
                    note = data["note"] as? String ?: "",
                    authorId = data["authorId"] as? String ?: "",
                    authorFullName = data["authorFullName"] as? String ?: "",
                    authorUsername = data["authorUsername"] as? String ?: "",
                    createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                )
            }
            
            Resource.Success(updates)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Güncellemeler alınamadı.")
        }
    }
    
    override suspend fun addStatusUpdate(
        postId: String, 
        status: PostStatus, 
        note: String
    ): Resource<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("Oturum açılmamış.")
            
            // Kullanıcı bilgilerini çek
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(user.uid)
                .get()
                .await()
            val profile = userDoc.toObject(User::class.java)
            
            val statusValue = when (status) {
                PostStatus.NEW -> "new"
                PostStatus.IN_PROGRESS -> "in_progress"
                PostStatus.RESOLVED -> "resolved"
                PostStatus.REJECTED -> "rejected"
            }
            
            val updateData = hashMapOf(
                "postId" to postId,
                "status" to statusValue,
                "note" to note,
                "authorId" to user.uid,
                "authorFullName" to (profile?.fullName ?: ""),
                "authorUsername" to (profile?.username ?: ""),
                "createdAt" to FieldValue.serverTimestamp()
            )
            
            // Transaction: StatusUpdate ekle + Post status güncelle
            firestore.runTransaction { transaction ->
                val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
                val updateRef = postRef.collection("statusUpdates").document()
                
                // StatusUpdate ekle
                transaction.set(updateRef, updateData)
                
                // Post'un status alanını güncelle
                transaction.update(postRef, "status", statusValue)
            }.await()
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Durum güncellenemedi.")
        }
    }
}