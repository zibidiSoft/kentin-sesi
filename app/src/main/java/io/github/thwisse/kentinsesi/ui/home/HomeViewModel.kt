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
    // private var yerine var yapıyoruz.
    // Dışarıdan okunabilsin ama değiştirilebilsin (set işlemini zaten getPosts içinde yapıyoruz)
    var lastDistrict: String? = null
        private set
    var lastCategory: String? = null
        private set
    var lastStatus: String? = null
        private set

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

    // Mevcut filtrelerle yenileme yap
    fun refreshPosts() {
        getPosts(lastDistrict, lastCategory, lastStatus)
    }

    // Beğeni (Upvote) Fonksiyonu
    fun toggleUpvote(post: Post) {
        val userId = currentUserId
        if (userId.isEmpty()) {
            android.util.Log.e("HomeViewModel", "toggleUpvote: userId boş")
            return // Kullanıcı yoksa işlem yapma
        }

        // Post ID kontrolü - id veya postId kullan
        // Önce id'yi kontrol et, boşsa postId'yi kullan
        val postId = when {
            post.id.isNotEmpty() -> post.id
            post.postId.isNotEmpty() -> post.postId
            else -> {
                android.util.Log.e("HomeViewModel", "toggleUpvote: postId boş - post.id: '${post.id}', post.postId: '${post.postId}'")
                return // Post ID yoksa işlem yapma
            }
        }

        android.util.Log.d("HomeViewModel", "toggleUpvote: postId=$postId, userId=$userId")

        viewModelScope.launch {
            // 1. Repository'ye işlemi gönder
            val result = postRepository.toggleUpvote(postId, userId)

            // 2. İşlem sonucunu kontrol et
            when (result) {
                is Resource.Success -> {
                    android.util.Log.d("HomeViewModel", "toggleUpvote: Başarılı")
                    // Hafızadaki son filtrelerle tekrar çağırıyoruz ki ekran bozulmasın
                    getPosts(lastDistrict, lastCategory, lastStatus)
                }
                is Resource.Error -> {
                    android.util.Log.e("HomeViewModel", "toggleUpvote: Hata - ${result.message}")
                }
                is Resource.Loading -> {
                    android.util.Log.d("HomeViewModel", "toggleUpvote: Loading")
                }
            }
        }
    }
}