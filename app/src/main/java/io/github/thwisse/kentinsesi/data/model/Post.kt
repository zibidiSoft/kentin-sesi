package io.github.thwisse.kentinsesi.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.RawValue

/**
 * Post modeli - Vatandaşların oluşturduğu paylaşımları temsil eder
 * 
 * ÖNEMLİ: 
 * - Firestore string olarak sakladığı için status'u String olarak tutuyoruz
 * - GeoPoint Parcelable olmadığı için Post'u Parcelable yapmıyoruz
 * - Navigation'da postId kullanılıyor, Post direkt gönderilmiyor
 */
data class Post(
    val id: String = "", // Firestore Document ID'si
    @DocumentId
    val postId: String = "",

    val authorId: String = "", // Gönderiyi oluşturan kullanıcının UID'si
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String? = null, // Fotoğraf olmayabilir
    val location: @RawValue GeoPoint? = null, // Konum bilgisi olmayabilir
    val district: String? = null, // Örn: "İskenderun"

    @ServerTimestamp
    val createdAt: @RawValue Timestamp? = null,

    // NOT: Firestore string olarak sakladığı için String kullanıyoruz
    // Ama enum'a çevirmek için statusEnum property'si ekledik
    val status: String = PostStatus.NEW.value, // Varsayılan: "new"
    val upvoteCount: Long = 0,
    val upvotedBy: List<String> = emptyList(),
) {
    
    /**
     * Status'u enum olarak döndürür - Kod içinde kullanım için
     * Örnek: if (post.statusEnum == PostStatus.RESOLVED) { ... }
     */
    val statusEnum: PostStatus
        get() = PostStatus.fromString(status)
    
    /**
     * Post'un çözülüp çözülmediğini kontrol eder
     */
    val isResolved: Boolean
        get() = statusEnum == PostStatus.RESOLVED
    
    /**
     * Post'un yeni olup olmadığını kontrol eder
     */
    val isNew: Boolean
        get() = statusEnum == PostStatus.NEW
}