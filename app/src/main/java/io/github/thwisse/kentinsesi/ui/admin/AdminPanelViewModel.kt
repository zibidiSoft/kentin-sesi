package io.github.thwisse.kentinsesi.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.thwisse.kentinsesi.data.model.User
import io.github.thwisse.kentinsesi.data.model.UserRole
import io.github.thwisse.kentinsesi.data.repository.AuthRepository
import io.github.thwisse.kentinsesi.data.repository.UserRepository
import io.github.thwisse.kentinsesi.util.AuthorizationUtils
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _usersState = MutableLiveData<Resource<List<User>>>()
    val usersState: LiveData<Resource<List<User>>> = _usersState

    private val _updateRoleState = MutableLiveData<Resource<Unit>>()
    val updateRoleState: LiveData<Resource<Unit>> = _updateRoleState

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    val currentUserId: String?
        get() = authRepository.currentUser?.uid

    init {
        loadCurrentUser()
        loadAllUsers()
    }

    /**
     * Mevcut kullanıcının bilgisini yükle (admin kontrolü için)
     */
    private fun loadCurrentUser() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val result = userRepository.getUser(userId)
            if (result is Resource.Success) {
                _currentUser.value = result.data
            }
        }
    }

    /**
     * Tüm kullanıcıları yükle
     */
    fun loadAllUsers() {
        viewModelScope.launch {
            _usersState.value = Resource.Loading()
            _usersState.value = userRepository.getAllUsers()
        }
    }

    /**
     * Kullanıcı rolünü güncelle
     */
    fun updateUserRole(userId: String, newRole: UserRole) {
        viewModelScope.launch {
            // Yetki kontrolü
            val user = _currentUser.value
            if (!AuthorizationUtils.isAdmin(user)) {
                _updateRoleState.value = Resource.Error("Bu işlem için admin yetkisi gereklidir.")
                return@launch
            }

            _updateRoleState.value = Resource.Loading()
            val result = userRepository.updateUserRole(userId, newRole.value)
            _updateRoleState.value = result

            // Başarılıysa kullanıcı listesini yenile
            if (result is Resource.Success) {
                loadAllUsers()
            }
        }
    }

    /**
     * Kullanıcının admin olup olmadığını kontrol et
     */
    fun isCurrentUserAdmin(): Boolean {
        return AuthorizationUtils.isAdmin(_currentUser.value)
    }
}

