package io.github.thwisse.kentinsesi.data.repository

import io.github.thwisse.kentinsesi.data.model.User
import io.github.thwisse.kentinsesi.util.Resource

// Kullanıcı profili işlemleri için sözleşme
interface UserRepository {
    suspend fun createUserProfile(uid: String, fullName: String, email: String): Resource<Unit>
    
    suspend fun updateUserProfile(uid: String, fullName: String, city: String, district: String): Resource<Unit>
    
    // Kullanıcı bilgisini getir
    suspend fun getUser(uid: String): Resource<User>
    
    // Kullanıcının rolünü kontrol et
    suspend fun getUserRole(uid: String): Resource<String>
    
    // Admin paneli için: Tüm kullanıcıları getir
    suspend fun getAllUsers(): Resource<List<User>>
    
    // Admin paneli için: Kullanıcı rolünü güncelle
    suspend fun updateUserRole(uid: String, newRole: String): Resource<Unit>
}