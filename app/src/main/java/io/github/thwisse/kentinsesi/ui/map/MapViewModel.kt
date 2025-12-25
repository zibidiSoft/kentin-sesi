package io.github.thwisse.kentinsesi.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.data.repository.PostRepository
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _postsState = MutableLiveData<Resource<List<Post>>>()
    val postsState: LiveData<Resource<List<Post>>> = _postsState

    init {
        getAllPosts()
    }

    // Haritada şimdilik filtresiz tüm postları gösterelim
    fun getAllPosts() {
        viewModelScope.launch {
            _postsState.value = Resource.Loading()
            val result = postRepository.getPosts(null, null, null)
            _postsState.value = result
        }
    }
}