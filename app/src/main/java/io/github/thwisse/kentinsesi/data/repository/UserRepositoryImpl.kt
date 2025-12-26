package io.github.thwisse.kentinsesi.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import io.github.thwisse.kentinsesi.data.model.User
import io.github.thwisse.kentinsesi.data.model.UserRole
import io.github.thwisse.kentinsesi.util.Constants
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    // 1. Kullanıcı Profili Oluşturma
    override suspend fun createUserProfile(uid: String, fullName: String, email: String): Resource<Unit> {
        return try {
            val newUser = User(
                uid = uid,
                fullName = fullName,
                email = email,
                // Enum kullanarak tip güvenli hale getirdik
                role = UserRole.CITIZEN.value // "citizen" yerine UserRole.CITIZEN.value
            )
            // Constants kullanarak collection adını merkezileştirdik
            firestore.collection(Constants.COLLECTION_USERS).document(uid).set(newUser).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Profil oluşturulamadı.")
        }
    }

    // 2. Kullanıcı Profili Güncelleme
    override suspend fun updateUserProfile(uid: String, fullName: String, city: String, district: String): Resource<Unit> {
        return try {
            val updates = mapOf(
                "fullName" to fullName,
                "city" to city,
                "district" to district,
                // Constants kullanarak title'ı merkezileştirdik
                "title" to Constants.TITLE_SENSITIVE_CITIZEN // "Duyarlı Vatandaş" yerine
            )

            firestore.collection(Constants.COLLECTION_USERS).document(uid).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Profil güncellenemedi.")
        }
    }
    
    // 3. Kullanıcı Bilgisini Getir
    override suspend fun getUser(uid: String): Resource<User> {
        return try {
            val document = firestore.collection(Constants.COLLECTION_USERS).document(uid).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                if (user != null) {
                    Resource.Success(user)
                } else {
                    Resource.Error("Kullanıcı verisi okunamadı.")
                }
            } else {
                Resource.Error("Kullanıcı bulunamadı.")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Kullanıcı bilgisi alınamadı.")
        }
    }
    
    // 4. Kullanıcı Rolünü Getir
    override suspend fun getUserRole(uid: String): Resource<String> {
        return try {
            val document = firestore.collection(Constants.COLLECTION_USERS).document(uid).get().await()
            if (document.exists()) {
                // Enum'dan varsayılan değer kullanıyoruz
                val role = document.getString("role") ?: UserRole.CITIZEN.value
                Resource.Success(role)
            } else {
                Resource.Error("Kullanıcı bulunamadı.")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Kullanıcı rolü alınamadı.")
        }
    }
    
    // 5. Tüm kullanıcıları getir (Admin paneli için)
    override suspend fun getAllUsers(): Resource<List<User>> {
        return try {
            // fullName boş olabilir, bu yüzden sıralama yapmadan çekiyoruz
            // Sonra Kotlin'de sıralayacağız
            val snapshot = firestore.collection(Constants.COLLECTION_USERS)
                .get()
                .await()
            
            val users = snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)
                user?.copy(uid = doc.id) // UID'yi document ID'den al
            }.sortedBy { it.fullName.ifEmpty { it.email } } // fullName yoksa email'e göre sırala
            
            Resource.Success(users)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Kullanıcılar alınamadı.")
        }
    }
    
    // 6. Kullanıcı rolünü güncelle (Admin paneli için)
    override suspend fun updateUserRole(uid: String, newRole: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .update("role", newRole)
                .await()
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Rol güncellenemedi.")
        }
    }
}