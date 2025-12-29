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
import io.github.thwisse.kentinsesi.data.model.User
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

    private val _addReplyState = MutableLiveData<Resource<Unit>>()
    val addReplyState: LiveData<Resource<Unit>> = _addReplyState

    private val _deletePostState = MutableLiveData<Resource<Unit>>()
    val deletePostState: LiveData<Resource<Unit>> = _deletePostState

    private val _updateStatusState = MutableLiveData<Resource<Unit>>()
    val updateStatusState: LiveData<Resource<Unit>> = _updateStatusState
    
    // Status Updates (Timeline)
    private val _statusUpdates = MutableLiveData<Resource<List<io.github.thwisse.kentinsesi.data.model.StatusUpdate>>>()
    val statusUpdates: LiveData<Resource<List<io.github.thwisse.kentinsesi.data.model.StatusUpdate>>> = _statusUpdates
    
    private val _statusUpdateCount = MutableLiveData<Int>(0)
    val statusUpdateCount: LiveData<Int> = _statusUpdateCount
    
    private val _addStatusUpdateState = MutableLiveData<Resource<Unit>>()
    val addStatusUpdateState: LiveData<Resource<Unit>> = _addStatusUpdateState
    
    // Post bilgisi ve kullanıcı bilgisi
    private val _currentPost = MutableLiveData<Post?>()
    val currentPost: LiveData<Post?> = _currentPost

    private val _postLoadState = MutableLiveData<Resource<Post>>()
    val postLoadState: LiveData<Resource<Post>> = _postLoadState

    private val _toggleUpvoteState = MutableLiveData<Resource<Unit>>()
    val toggleUpvoteState: LiveData<Resource<Unit>> = _toggleUpvoteState
    
    private val _currentUser = MutableLiveData<io.github.thwisse.kentinsesi.data.model.User?>()
    val currentUser: LiveData<io.github.thwisse.kentinsesi.data.model.User?> = _currentUser

    private val _postAuthor = MutableLiveData<User?>()
    val postAuthor: LiveData<User?> = _postAuthor
    
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
            loadPostAuthor(post.authorId)
        }
    }

    private suspend fun loadPostAuthor(authorId: String) {
        if (authorId.isBlank()) {
            _postAuthor.value = null
            return
        }
        val authorResult = userRepository.getUser(authorId)
        if (authorResult is Resource.Success) {
            _postAuthor.value = authorResult.data
        }
    }
    
    /**
     * Post ID'ye göre post bilgisini yükle (State restore için)
     */
    fun loadPostById(postId: String) {
        viewModelScope.launch {
            _postLoadState.value = Resource.Loading()

            val result = postRepository.getPostById(postId)
            when (result) {
                is Resource.Success -> {
                    result.data?.let { post ->
                        _currentPost.value = post
                        loadCurrentUser()
                        loadPostAuthor(post.authorId)
                        _postLoadState.value = Resource.Success(post)
                    }
                }
                is Resource.Error -> {
                    // Hata durumunda bir şey yapma, Fragment handle edecek
                    _postLoadState.value = Resource.Error(result.message ?: "Post yüklenemedi")
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
            _commentsState.value = postRepository.getThreadedComments(postId)
        }
    }

    fun toggleUpvote(postId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _toggleUpvoteState.value = Resource.Loading()
            val result = postRepository.toggleUpvote(postId, userId)
            _toggleUpvoteState.value = result
            if (result is Resource.Success) loadPostById(postId)
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

    fun addReply(
        postId: String,
        text: String,
        parentCommentId: String,
        replyToAuthorId: String?,
        replyToAuthorFullName: String?
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _addReplyState.value = Resource.Loading()
            val result = postRepository.addReply(
                postId = postId,
                text = text,
                parentCommentId = parentCommentId,
                replyToAuthorId = replyToAuthorId,
                replyToAuthorFullName = replyToAuthorFullName
            )
            _addReplyState.value = result
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

    private val _deleteCommentState = MutableLiveData<Resource<Unit>>()
    val deleteCommentState: LiveData<Resource<Unit>> = _deleteCommentState

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
    
    // ======================== STATUS UPDATES ========================
    
    fun loadStatusUpdates(postId: String) {
        viewModelScope.launch {
            _statusUpdates.value = Resource.Loading()
            val result = postRepository.getStatusUpdates(postId)
            _statusUpdates.value = result
            
            // Count güncelle
            if (result is Resource.Success) {
                _statusUpdateCount.value = result.data?.size ?: 0
            }
        }
    }
    
    fun addStatusUpdate(status: PostStatus, note: String) {
        val postId = _currentPost.value?.id ?: return
        
        viewModelScope.launch {
            _addStatusUpdateState.value = Resource.Loading()
            val result = postRepository.addStatusUpdate(postId, status, note)
            _addStatusUpdateState.value = result
            
            if (result is Resource.Success) {
                // Timeline ve count'u yenile
                loadStatusUpdates(postId)
            }
        }
    }
    
    // ======================== COMMENTS ========================
    
    fun deleteComment(commentId: String) {
        val postId = _currentPost.value?.id
        val user = _currentUser.value
        
        android.util.Log.d("DeleteComment", "deleteComment called - postId: $postId, commentId: $commentId, user: $user")
        
        if (postId == null) {
            android.util.Log.e("DeleteComment", "postId is null, returning early")
            return
        }
        if (user == null) {
            android.util.Log.e("DeleteComment", "user is null, returning early")
            return
        }
        
        viewModelScope.launch {
            _deleteCommentState.value = Resource.Loading()
            
            // Sadece admin rolü için isAdmin true olmalı, official değil
            val isAdmin = user.role == "admin"
            android.util.Log.d("DeleteComment", "Calling repository.deleteComment - isAdmin: $isAdmin")
            
            val result = postRepository.deleteComment(postId, commentId, isAdmin)
            
            android.util.Log.d("DeleteComment", "Result: $result, is Success: ${result is Resource.Success}")
            _deleteCommentState.value = result
            
            if (result is Resource.Success) {
                android.util.Log.d("DeleteComment", "SUCCESS! Now calling getComments with postId: $postId")
                getComments(postId) // Listeyi yenile
                android.util.Log.d("DeleteComment", "getComments called")
            } else {
                android.util.Log.e("DeleteComment", "Result is not Success: $result")
            }
        }
    }
}