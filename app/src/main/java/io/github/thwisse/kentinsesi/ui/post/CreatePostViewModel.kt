package io.github.thwisse.kentinsesi.ui.post

import android.net.Uri // <-- Bu import gerekli
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.thwisse.kentinsesi.data.repository.PostRepository
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _createPostState = MutableLiveData<Resource<Unit>>()
    val createPostState: LiveData<Resource<Unit>> = _createPostState

    fun createPost(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        district: String,
        imageUri: Uri // <-- EKSİK OLAN BU PARAMETREYİ EKLEDİK
    ) {
        viewModelScope.launch {
            _createPostState.value = Resource.Loading()

            // Repository'ye iletiyoruz
            val result = repository.createPost(
                imageUri = imageUri,
                title = title,
                description = description,
                category = category,
                latitude = latitude,
                longitude = longitude,
                district = district
            )
            _createPostState.value = result
        }
    }
}