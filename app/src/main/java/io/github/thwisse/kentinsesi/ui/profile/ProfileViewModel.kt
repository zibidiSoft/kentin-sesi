package io.github.thwisse.kentinsesi.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.data.model.PostStatus
import io.github.thwisse.kentinsesi.data.repository.AuthRepository
import io.github.thwisse.kentinsesi.data.repository.PostRepository
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: io.github.thwisse.kentinsesi.data.repository.UserRepository
) : ViewModel() {

    private val _userPosts = MutableLiveData<Resource<List<Post>>>()
    val userPosts: LiveData<Resource<List<Post>>> = _userPosts

    // Kullanıcının yorumları
    private val _userComments = MutableLiveData<Resource<List<Comment>>>()
    val userComments: LiveData<Resource<List<Comment>>> = _userComments

    // İstatistikler için LiveData
    private val _totalPostsCount = MutableLiveData<Int>()
    val totalPostsCount: LiveData<Int> = _totalPostsCount

    private val _resolvedPostsCount = MutableLiveData<Int>()
    val resolvedPostsCount: LiveData<Int> = _resolvedPostsCount

    // Kullanıcı bilgisi - Firebase User
    val currentUser = authRepository.currentUser
    
    // Kullanıcı profil bilgisi - User model (fullName, city, district için)
    private val _userProfile = MutableLiveData<Resource<io.github.thwisse.kentinsesi.data.model.User>>()
    val userProfile: LiveData<Resource<io.github.thwisse.kentinsesi.data.model.User>> = _userProfile

    init {
        loadUserProfile()
        loadUserPosts()
        loadUserComments()
    }
    
    /**
     * Kullanıcı profil bilgisini yükle (fullName, city, district için)
     */
    fun loadUserProfile() {
        val userId = currentUser?.uid ?: return
        
        viewModelScope.launch {
            _userProfile.value = Resource.Loading()
            val result = userRepository.getUser(userId)
            _userProfile.value = result
        }
    }

    fun loadUserPosts() {
        val userId = currentUser?.uid ?: return

        viewModelScope.launch {
            _userPosts.value = Resource.Loading()
            val result = postRepository.getUserPosts(userId)

            if (result is Resource.Success) {
                val posts = result.data ?: emptyList()

                // İstatistikleri hesapla - Enum kullanarak tip güvenli hale getirdik
                _totalPostsCount.value = posts.size
                _resolvedPostsCount.value = posts.count { it.statusEnum == PostStatus.RESOLVED }
            }

            _userPosts.value = result
        }
    }

    /**
     * Kullanıcının yaptığı tüm yorumları yükle (silinen yorumlar hariç)
     */
    fun loadUserComments() {
        val userId = currentUser?.uid ?: return

        viewModelScope.launch {
            _userComments.value = Resource.Loading()
            val result = postRepository.getUserComments(userId)
            
            // Profilde silinen yorumları gösterme
            if (result is Resource.Success) {
                val filteredComments = result.data?.filter { !it.isDeleted } ?: emptyList()
                _userComments.value = Resource.Success(filteredComments)
            } else {
                _userComments.value = result
            }
        }
    }
    
    /**
     * Pull-to-refresh için: Hem profil hem postları hem yorumları yenile
     */
    fun refreshAll() {
        loadUserProfile()
        loadUserPosts()
        loadUserComments()
    }
    
    fun deleteComment(comment: Comment) {
        val user = currentUser
        if (user == null || comment.postId.isBlank()) return
        
        viewModelScope.launch {
            // Sadece admin rolü için isAdmin true olmalı
            val currentUserRole = _userProfile.value?.data?.role
            val isAdmin = currentUserRole == "admin"
            
            val result = postRepository.deleteComment(comment.postId, comment.id, isAdmin)
            
            if (result is Resource.Success) {
                // Başarılıysa listeyi yenile
                loadUserComments()
            } else {
                // Hata mesajı gösterilebilir (UI observe etmeli veya tek seferlik event)
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}