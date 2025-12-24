package io.github.thwisse.kentinsesi.data.model

import com.google.firebase.Timestamp // <-- Timestamp için import
import com.google.firebase.firestore.DocumentId // <-- Firestore ID'si için import
import com.google.firebase.firestore.GeoPoint // <-- Konum için import
import com.google.firebase.firestore.ServerTimestamp // <-- Sunucu zaman damgası için import

data class Post(
    @DocumentId // Bu anotasyon, Firestore'un doküman ID'sini bu alana otomatik atamasını sağlar.
    val postId: String = "",

    val authorId: String = "", // Gönderiyi oluşturan kullanıcının UID'si
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String? = null, // Fotoğraf olmayabilir, bu yüzden nullable (?) yaptık
    val location: GeoPoint? = null, // Konum bilgisi olmayabilir (?)

    // YENİ EKLENEN ALAN:
    val district: String? = null, // Örn: "İskenderun"

    @ServerTimestamp // Firestore'a yazarken sunucu saatini otomatik atar, okurken Timestamp döner.
    val createdAt: Timestamp? = null, // İlk başta null olabilir

    val status: String = "new", // Varsayılan durum: yeni
    val upvoteCount: Long = 0,
    val upvotedBy: List<String> = emptyList() // Oy veren kullanıcıların UID listesi
) {
    // Firestore için boş constructor (tüm alanların varsayılan değeri olduğu için otomatik üretilir)
}