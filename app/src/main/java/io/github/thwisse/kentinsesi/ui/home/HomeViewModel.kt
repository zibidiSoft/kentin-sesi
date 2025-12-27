package io.github.thwisse.kentinsesi.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.data.model.FilterCriteria
import io.github.thwisse.kentinsesi.data.model.FilterPreset
import io.github.thwisse.kentinsesi.data.repository.AuthRepository
import io.github.thwisse.kentinsesi.data.repository.FilterRepository
import io.github.thwisse.kentinsesi.data.repository.PostRepository
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository, // YENİ: ID için bunu ekledik
    private val filterRepository: FilterRepository
) : ViewModel() {

    private val _postsState = MutableLiveData<Resource<List<Post>>>()
    val postsState: LiveData<Resource<List<Post>>> = _postsState

    val presets: LiveData<List<FilterPreset>> = filterRepository.observePresets().asLiveData()

    // Kullanıcının kendi ID'si (Adapter'a göndermek için lazım)
    val currentUserId: String
        get() = authRepository.currentUser?.uid ?: ""

    // Son seçilen filtreleri burada tutacağız
    var lastDistricts: List<String>? = null
        private set
    var lastCategories: List<String>? = null
        private set
    var lastStatuses: List<String>? = null
        private set

    var lastOnlyMyPosts: Boolean = false
        private set

    init {
        viewModelScope.launch {
            filterRepository.ensureSystemDefaultExists()

            val criteria = filterRepository.getDefaultPreset()?.criteria
                ?: run {
                    val presetId = filterRepository.observeLastAppliedPresetId().first()
                    val preset = presetId?.let { filterRepository.getPresetById(it) }
                    preset?.criteria
                }
                ?: filterRepository.observeLastCriteria().first()
                ?: FilterCriteria()

            applyCriteriaInternal(criteria)
        }
    }

    // Verileri getiren fonksiyon
    fun getPosts(
        districts: List<String>? = null,
        categories: List<String>? = null,
        statuses: List<String>? = null,
        onlyMyPosts: Boolean = false
    ) {
        val criteria = FilterCriteria(
            districts = districts.orEmpty(),
            categories = categories.orEmpty(),
            statuses = statuses.orEmpty(),
            onlyMyPosts = onlyMyPosts
        )

        viewModelScope.launch {
            // Ad-hoc filtre uygulandıysa preset seçimi yok sayılır
            filterRepository.setLastAppliedPresetId(null)
            filterRepository.setLastCriteria(criteria)
            applyCriteriaInternal(criteria)
        }
    }

    fun applyPreset(presetId: String) {
        viewModelScope.launch {
            val preset = filterRepository.getPresetById(presetId)
                ?: return@launch

            filterRepository.setLastAppliedPresetId(preset.id)
            filterRepository.setLastCriteria(null)
            applyCriteriaInternal(preset.criteria)
        }
    }

    fun setDefaultPreset(presetId: String) {
        viewModelScope.launch {
            val result = filterRepository.setDefaultPreset(presetId)
            if (result is Resource.Error) {
                android.util.Log.e("HomeViewModel", "setDefaultPreset: ${result.message}")
            }
        }
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            val result = filterRepository.deletePreset(presetId)
            if (result is Resource.Error) {
                android.util.Log.e("HomeViewModel", "deletePreset: ${result.message}")
            }
        }
    }

    fun savePreset(name: String, criteria: FilterCriteria) {
        viewModelScope.launch {
            val result = filterRepository.savePreset(name, criteria)
            if (result is Resource.Error) {
                android.util.Log.e("HomeViewModel", "savePreset: ${result.message}")
            }
        }
    }

    suspend fun savePresetNow(name: String, criteria: FilterCriteria): Resource<Unit> {
        return filterRepository.savePreset(name, criteria)
    }

    private suspend fun applyCriteriaInternal(criteria: FilterCriteria) {
        lastDistricts = criteria.districts.takeIf { it.isNotEmpty() }
        lastCategories = criteria.categories.takeIf { it.isNotEmpty() }
        lastStatuses = criteria.statuses.takeIf { it.isNotEmpty() }
        lastOnlyMyPosts = criteria.onlyMyPosts

        _postsState.value = Resource.Loading()
        val result = postRepository.getPosts(lastDistricts, lastCategories, lastStatuses)

        if (result is Resource.Success && lastOnlyMyPosts) {
            val userId = currentUserId
            val filtered = result.data.orEmpty().filter { it.authorId == userId }
            _postsState.value = Resource.Success(filtered)
        } else {
            _postsState.value = result
        }
    }

    // Mevcut filtrelerle yenileme yap
    fun refreshPosts() {
        getPosts(lastDistricts, lastCategories, lastStatuses, lastOnlyMyPosts)
    }

    // Beğeni (Upvote) Fonksiyonu
    fun toggleUpvote(post: Post) {
        val userId = currentUserId
        if (userId.isEmpty()) {
            android.util.Log.e("HomeViewModel", "toggleUpvote: userId boş")
            return // Kullanıcı yoksa işlem yapma
        }

        val postId = post.id
        if (postId.isBlank()) {
            android.util.Log.e("HomeViewModel", "toggleUpvote: postId boş - post.id: '${post.id}'")
            return
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
                    getPosts(lastDistricts, lastCategories, lastStatuses)
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