package io.github.thwisse.kentinsesi.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.data.repository.AuthRepository
import io.github.thwisse.kentinsesi.data.repository.PostRepository
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository // YENİ: ID için bunu ekledik
) : ViewModel() {

    private val _postsState = MutableLiveData<Resource<List<Post>>>()
    val postsState: LiveData<Resource<List<Post>>> = _postsState

    // Kullanıcının kendi ID'si (Adapter'a göndermek için lazım)
    val currentUserId: String
        get() = authRepository.currentUser?.uid ?: ""

    // Son seçilen filtreleri burada tutacağız
    private var lastDistrict: String? = null
    private var lastCategory: String? = null
    private var lastStatus: String? = null

    init {
        getPosts()
    }

    // Verileri getiren fonksiyon
    fun getPosts(district: String? = null, category: String? = null, status: String? = null) {
        // Gelen filtreleri hafızaya kaydet
        lastDistrict = district
        lastCategory = category
        lastStatus = status

        viewModelScope.launch {
            _postsState.value = Resource.Loading()
            val result = postRepository.getPosts(district, category, status)
            _postsState.value = result
        }
    }

    // Beğeni (Upvote) Fonksiyonu
    fun toggleUpvote(post: Post) {
        val userId = currentUserId
        if (userId.isEmpty()) return // Kullanıcı yoksa işlem yapma

        viewModelScope.launch {
            // 1. Repository'ye işlemi gönder
            val result = postRepository.toggleUpvote(post.id, userId)

            // 2. İşlem başarılıysa listeyi olduğu gibi YENİLE
            if (result is Resource.Success) {
                // Hafızadaki son filtrelerle tekrar çağırıyoruz ki ekran bozulmasın
                getPosts(lastDistrict, lastCategory, lastStatus)
            }
        }
    }
}