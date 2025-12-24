package io.github.thwisse.kentinsesi.ui.post

import android.net.Uri
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
    private val postRepository: PostRepository
) : ViewModel() {

    // UI'ın dinleyeceği durum (Yükleniyor, Başarılı, Hata)
    private val _createPostState = MutableLiveData<Resource<Unit>>()
    val createPostState: LiveData<Resource<Unit>> = _createPostState

    // Kullanıcının seçtiği fotoğrafı geçici olarak burada tutabiliriz
    // Fragment, galeri sonucunu buraya set eder, ekran dönse de kaybolmaz
    var selectedImageUri: Uri? = null

    // Gönderi oluşturma isteği
    fun createPost(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        district: String // YENİ
    ) {
        val imageUri = selectedImageUri
        if (imageUri == null) {
            _createPostState.value = Resource.Error("Lütfen bir fotoğraf seçin.")
            return
        }

        viewModelScope.launch {
            _createPostState.value = Resource.Loading()

            // Repository'ye işi devret
            val result = postRepository.createPost(
                imageUri = imageUri,
                title = title,
                description = description,
                category = category,
                latitude = latitude,
                longitude = longitude,
                district = district // YENİ
            )

            _createPostState.value = result
        }
    }
}