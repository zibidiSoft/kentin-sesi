package io.github.thwisse.kentinsesi.ui.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.data.repository.AuthRepository
import io.github.thwisse.kentinsesi.data.repository.PostRepository
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository // YENİ: Kimlik kontrolü için
) : ViewModel() {

    private val _commentsState = MutableLiveData<Resource<List<Comment>>>()
    val commentsState: LiveData<Resource<List<Comment>>> = _commentsState

    private val _addCommentState = MutableLiveData<Resource<Unit>>()
    val addCommentState: LiveData<Resource<Unit>> = _addCommentState

    // YENİ STATE'LER
    private val _deletePostState = MutableLiveData<Resource<Unit>>()
    val deletePostState: LiveData<Resource<Unit>> = _deletePostState

    private val _updateStatusState = MutableLiveData<Resource<Unit>>()
    val updateStatusState: LiveData<Resource<Unit>> = _updateStatusState

    // Şu anki kullanıcı ID'si
    val currentUserId: String?
        get() = authRepository.currentUser?.uid

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

    // YENİ FONKSİYONLAR
    fun deletePost(postId: String) {
        viewModelScope.launch {
            _deletePostState.value = Resource.Loading()
            _deletePostState.value = postRepository.deletePost(postId)
        }
    }

    fun markAsResolved(postId: String) {
        viewModelScope.launch {
            _updateStatusState.value = Resource.Loading()
            _updateStatusState.value = postRepository.updatePostStatus(postId, "resolved")
        }
    }
}