package io.github.thwisse.kentinsesi.ui.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.data.model.PostStatus
import io.github.thwisse.kentinsesi.data.repository.AuthRepository
import io.github.thwisse.kentinsesi.data.repository.PostRepository
import io.github.thwisse.kentinsesi.data.repository.UserRepository
import io.github.thwisse.kentinsesi.util.AuthorizationUtils
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository // Yetkili kontrolü için
) : ViewModel() {

    private val _commentsState = MutableLiveData<Resource<List<Comment>>>()
    val commentsState: LiveData<Resource<List<Comment>>> = _commentsState

    private val _addCommentState = MutableLiveData<Resource<Unit>>()
    val addCommentState: LiveData<Resource<Unit>> = _addCommentState

    private val _deletePostState = MutableLiveData<Resource<Unit>>()
    val deletePostState: LiveData<Resource<Unit>> = _deletePostState

    private val _updateStatusState = MutableLiveData<Resource<Unit>>()
    val updateStatusState: LiveData<Resource<Unit>> = _updateStatusState
    
    // Post bilgisi ve kullanıcı bilgisi
    private val _currentPost = MutableLiveData<Post?>()
    val currentPost: LiveData<Post?> = _currentPost
    
    private val _currentUser = MutableLiveData<io.github.thwisse.kentinsesi.data.model.User?>()
    val currentUser: LiveData<io.github.thwisse.kentinsesi.data.model.User?> = _currentUser
    
    // Yetki kontrolleri için LiveData'lar - MediatorLiveData kullanarak
    // Post sahibi veya yetkili kullanıcılar durum güncelleyebilir
    val canUpdateStatus: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        fun update() {
            val user = _currentUser.value
            val post = _currentPost.value
            val isOwner = post?.authorId == currentUserId
            value = AuthorizationUtils.canUpdatePostStatus(user) || isOwner
        }
        addSource(_currentUser) { update() }
        addSource(_currentPost) { update() }
    }
    
    val canDeletePost: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        fun update() {
            value = AuthorizationUtils.canDeletePost(_currentUser.value, _currentPost.value?.authorId)
        }
        addSource(_currentUser) { update() }
        addSource(_currentPost) { update() }
    }

    // Şu anki kullanıcı ID'si
    val currentUserId: String?
        get() = authRepository.currentUser?.uid
    
    /**
     * Post detayını yükle ve kullanıcı bilgisini çek
     */
    fun loadPostDetails(postId: String) {
        viewModelScope.launch {
            // Post bilgisini çek (şimdilik getPosts'u kullanıyoruz, ilerde tek post çekme fonksiyonu eklenebilir)
            loadCurrentUser()
        }
    }
    
    /**
     * Mevcut kullanıcının bilgisini yükle
     */
    private suspend fun loadCurrentUser() {
        val userId = currentUserId ?: return
        val userResult = userRepository.getUser(userId)
        if (userResult is Resource.Success) {
            _currentUser.value = userResult.data
        }
    }
    
    /**
     * Post bilgisini set et (Fragment'tan çağrılacak)
     */
    fun setPost(post: Post) {
        _currentPost.value = post
        // Kullanıcı bilgisini coroutine içinde yükle
        viewModelScope.launch {
            loadCurrentUser()
        }
    }
    
    /**
     * Post ID'ye göre post bilgisini yükle (State restore için)
     */
    fun loadPostById(postId: String) {
        viewModelScope.launch {
            _currentPost.value?.let { 
                // Zaten yüklüyse tekrar yükleme
                if (it.id == postId) return@launch
            }
            
            val result = postRepository.getPostById(postId)
            when (result) {
                is Resource.Success -> {
                    result.data?.let { post ->
                        _currentPost.value = post
                        loadCurrentUser()
                    }
                }
                is Resource.Error -> {
                    // Hata durumunda bir şey yapma, Fragment handle edecek
                }
                is Resource.Loading -> {
                    // Loading durumu
                }
            }
        }
    }

    fun getComments(postId: String) {
        viewModelScope.launch {
            _commentsState.value = Resource.Loading()
            _commentsState.value = postRepository.getComments(postId)
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _addCommentState.value = Resource.Loading()
            val result = postRepository.addComment(postId, text)
            _addCommentState.value = result
            if (result is Resource.Success) getComments(postId)
        }
    }

    /**
     * Post'u sil - Sadece post sahibi veya admin silebilir
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            val post = _currentPost.value
            
            // Yetki kontrolü
            if (!AuthorizationUtils.canDeletePost(user, post?.authorId)) {
                _deletePostState.value = Resource.Error("Bu işlem için yetkiniz yok.")
                return@launch
            }
            
            _deletePostState.value = Resource.Loading()
            val result = postRepository.deletePost(postId)
            _deletePostState.value = result
        }
    }

    /**
     * Post durumunu güncelle - Yetkili kullanıcılar veya post sahibi yapabilir
     * @param postId Güncellenecek post'un ID'si
     * @param status Yeni durum
     */
    fun updatePostStatus(postId: String, status: PostStatus) {
        viewModelScope.launch {
            val user = _currentUser.value
            val post = _currentPost.value
            
            // Yetki kontrolü: Yetkili kullanıcılar veya post sahibi güncelleyebilir
            val isOwner = post?.authorId == currentUserId
            val canUpdate = AuthorizationUtils.canUpdatePostStatus(user) || isOwner
            
            if (!canUpdate) {
                _updateStatusState.value = Resource.Error("Post durumunu güncellemek için yetkiniz yok.")
                return@launch
            }
            
            _updateStatusState.value = Resource.Loading()
            val result = postRepository.updatePostStatus(postId, status.value)
            _updateStatusState.value = result
            
            // Başarılıysa post bilgisini güncelle
            if (result is Resource.Success) {
                _currentPost.value?.let { currentPost ->
                    _currentPost.value = currentPost.copy(status = status.value)
                }
            }
        }
    }
}