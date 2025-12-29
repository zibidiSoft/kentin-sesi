package io.github.thwisse.kentinsesi.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Durum güncelleme modeli - Post'ların yaşam döngüsünü takip eder
 */
data class StatusUpdate(
    @DocumentId
    val id: String = "",
    
    // İlgili post ID'si
    val postId: String = "",
    
    // Güncelleme anındaki durum (new, in_progress, resolved)
    val status: String = "",
    
    // Kullanıcı notu (zorunlu)
    val note: String = "",
    
    // Güncellemeyi yapan kişi
    val authorId: String = "",
    val authorFullName: String = "",
    val authorUsername: String = "",
    
    @ServerTimestamp
    val createdAt: Date? = null
)
