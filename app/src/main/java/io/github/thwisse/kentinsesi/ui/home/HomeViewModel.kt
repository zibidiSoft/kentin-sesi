package io.github.thwisse.kentinsesi.ui.home

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
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _postsState = MutableLiveData<Resource<List<Post>>>()
    val postsState: LiveData<Resource<List<Post>>> = _postsState

    init {
        // ViewModel ilk oluştuğunda verileri çekmeye başla
        getPosts()
    }

    // Parametreler varsayılan olarak null (Filtresiz)
    fun getPosts(district: String? = null, category: String? = null, status: String? = null) {
        viewModelScope.launch {
            _postsState.value = Resource.Loading()
            // Filtreleri repository'ye ilet
            val result = postRepository.getPosts(district, category, status)
            _postsState.value = result
        }
    }
}