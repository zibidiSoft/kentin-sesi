package io.github.thwisse.kentinsesi.ui.post

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.data.model.PostStatus
import io.github.thwisse.kentinsesi.databinding.FragmentPostDetailBinding
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.util.Resource
import io.github.thwisse.kentinsesi.util.ValidationUtils
import androidx.core.view.isVisible
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import android.content.Context
import android.view.inputmethod.InputMethodManager
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class PostDetailFragment : Fragment(io.github.thwisse.kentinsesi.R.layout.fragment_post_detail), OnMapReadyCallback {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    // ViewModel Tanımı
    private val viewModel: PostDetailViewModel by viewModels()
    private val commentAdapter = CommentAdapter(
        onCommentClick = { comment ->
            enterReplyMode(comment)
        },
        onRepliesToggleClick = { rootComment ->
            toggleReplies(rootComment)
        },
        onCommentLongClick = { comment ->
            if (comment.isDeleted) return@CommentAdapter
            // Basit yetki kontrolü: Sadece kendi yorumunu veya yetkili ise silebilir
            // Detaylı kontrolü ViewModel/Repository yapacak
            showDeleteCommentDialog(comment)
        }
    )

    private var postLocation: LatLng? = null
    private var currentPostId: String? = null
    private var currentPost: Post? = null

    private var replyingTo: Comment? = null

    private var lastRequestedStatus: PostStatus? = null

    private var isCommentsSectionExpanded: Boolean = false

    private var allThreadedComments: List<Comment> = emptyList()
    private val expandedCommentIds = mutableSetOf<String>()

    private var isRefreshingPost: Boolean = false
    private var isRefreshingComments: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailBinding.bind(view)

        val baseInputPaddingBottom = binding.inputLayout.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.inputLayout) { v, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val bottom = maxOf(imeBottom, sysBottom)
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, baseInputPaddingBottom + bottom)
            insets
        }

        binding.btnCancelReply.setOnClickListener {
            exitReplyMode()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            val postId = currentPostId
            if (postId != null) {
                binding.swipeRefreshLayout.isRefreshing = true
                isRefreshingPost = true
                isRefreshingComments = true
                viewModel.loadPostById(postId)
                viewModel.getComments(postId)
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // State restore stratejisi:
        // 1. ViewModel'den dene (eğer ViewModel hala varsa - process death olmadıysa)
        // 2. Arguments'tan postId al ve çek (ilk yükleme)
        // 3. savedInstanceState'tan post ID al ve tekrar çek (process death durumunda)
        val viewModelPost = viewModel.currentPost.value
        val argumentPostId = arguments?.getString("postId")
        val savedPostId = savedInstanceState?.getString("saved_post_id")
        
        val postIdToLoad = viewModelPost?.id ?: argumentPostId ?: savedPostId
        
        when {
            // En iyi durum: ViewModel'de post var (process death olmadı)
            viewModelPost != null -> {
                loadPost(viewModelPost)
            }
            // İkinci ve üçüncü durum: Post ID var, tekrar çekmemiz gerekiyor
            postIdToLoad != null -> {
                currentPostId = postIdToLoad
                // Post'u yükle
                viewModel.loadPostById(postIdToLoad)
                // ViewModel'den post'u observe et
                observePostFromViewModel()
            }
            else -> {
                // Hiçbir şey yok, geri git
                findNavController().navigateUp()
            }
        }

        observePostLoadState()
        observeToggleUpvoteState()
        observeOwnerActions()
        setupStatusUpdates()

        viewModel.postAuthor.observe(viewLifecycleOwner) { author ->
            if (author == null) {
                binding.cardPostAuthor.isVisible = false
            } else {
                binding.cardPostAuthor.isVisible = true
                binding.tvPostAuthorFullName.text = author.fullName.ifBlank { "-" }

                val username = author.username.trim().takeIf { it.isNotBlank() }
                binding.tvPostAuthorUsername.isVisible = username != null
                binding.tvPostAuthorUsername.text = if (username != null) "@$username" else ""

                val location = listOf(author.city, author.district)
                    .filter { it.isNotBlank() }
                    .joinToString("/")
                val title = author.title
                binding.tvPostAuthorMeta.text = buildString {
                    if (location.isNotBlank()) append(location)
                    if (location.isNotBlank() && title.isNotBlank()) append(" • ")
                    if (title.isNotBlank()) append(title)
                }.ifBlank { " " }
            }
        }

        viewModel.addReplyState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Error -> {
                    binding.etComment.error = resource.message
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Success -> {
                    exitReplyMode()
                    hideKeyboardAndClearInputFocus()
                }
                is Resource.Loading -> { }
            }
        }
        
        viewModel.deleteCommentState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Yorum silindi", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> { }
            }
        }
    }

    private fun showDeleteCommentDialog(comment: Comment) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_comment_title)
            .setMessage(R.string.dialog_delete_comment_message)
            .setPositiveButton("Sil") { _, _ ->
                viewModel.deleteComment(comment.id)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun enterReplyMode(comment: Comment) {
        replyingTo = comment
        binding.replyBanner.isVisible = true
        val name = comment.authorFullName
            .ifBlank { comment.authorFullName }
            .ifBlank { "-" }
        binding.tvReplyBannerText.text = getString(R.string.reply_to_person, name)
        binding.etComment.hint = getString(R.string.reply_write_hint)
        binding.etComment.requestFocus()
    }

    private fun exitReplyMode() {
        replyingTo = null
        binding.replyBanner.isVisible = false
        binding.tvReplyBannerText.text = ""
        binding.etComment.hint = getString(R.string.comment_write_hint)
    }

    private fun hideKeyboardAndClearInputFocus() {
        binding.etComment.clearFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etComment.windowToken, 0)
    }

    private fun observePostLoadState() {
        viewModel.postLoadState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    if (isRefreshingPost) {
                        isRefreshingPost = false
                        if (!isRefreshingComments) {
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    if (isRefreshingPost) {
                        isRefreshingPost = false
                        if (!isRefreshingComments) {
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
                is Resource.Loading -> {
                    // no-op
                }
            }
        }
    }

    private fun observeToggleUpvoteState() {
        viewModel.toggleUpvoteState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    // Optimistic güncelleme yaptıysak server state'e dönmek için yeniden çek
                    currentPostId?.let { viewModel.loadPostById(it) }
                }
                else -> {
                    // no-op
                }
            }
        }
    }
    
    /**
     * Post'u yükle ve UI'ı kur
     */
    private fun loadPost(post: Post) {
        currentPost = post
        currentPostId = post.id
        
        // ViewModel'e post bilgisini set et
        viewModel.setPost(post)
        
        setupViews(post)
        setupMap(post)
        setupComments()
        setupOfficialActions()

        binding.tvCommentsHeader.text = getString(R.string.comments_header, post.commentCount)
        
        // Menü kurulumu
        setupOwnerMenu()

        // Yorumları Çek (sadece ilk yüklemede)
        viewModel.getComments(post.id)
    }
    
    /**
     * ViewModel'den post'u observe et (process death sonrası restore için)
     */
    private fun observePostFromViewModel() {
        viewModel.currentPost.observe(viewLifecycleOwner) { post ->
            post?.let {
                if (currentPost == null) {
                    // İlk kez yükleniyor - Post restore edildi
                    loadPost(it)
                    // Yorumları da yükle
                    viewModel.getComments(it.id)
                } else {
                    // Post zaten yüklü, sadece güncelle (status değişikliği gibi)
                    currentPost = it
                    setupViews(it)
                }

                if (isRefreshingPost) {
                    isRefreshingPost = false
                    if (!isRefreshingComments) {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Post'u direkt kaydetme (GeoPoint Parcelable değil, crash veriyor)
        // Sadece post ID'sini kaydet, restore ederken tekrar çekeceğiz
        currentPostId?.let {
            outState.putString("saved_post_id", it)
        }
    }

    private fun setupOwnerMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(io.github.thwisse.kentinsesi.R.menu.menu_post_detail, menu)
                
                // Başlangıçta menü öğelerini gizle, LiveData güncellendiğinde göster
                menu.findItem(io.github.thwisse.kentinsesi.R.id.action_update_status)?.isVisible = false
                menu.findItem(io.github.thwisse.kentinsesi.R.id.action_delete)?.isVisible = false
                
                // Silme kontrolü - Post sahibi veya admin silebilir
                viewModel.canDeletePost.observe(viewLifecycleOwner) { canDelete ->
                    menu.findItem(io.github.thwisse.kentinsesi.R.id.action_delete)?.isVisible = canDelete
                }
                
                // Durumu güncelle - Yetkili kullanıcılar veya post sahibi yapabilir
                viewModel.canUpdateStatus.observe(viewLifecycleOwner) { canUpdate ->
                    menu.findItem(io.github.thwisse.kentinsesi.R.id.action_update_status)?.isVisible = canUpdate
                }
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                return when (menuItem.itemId) {
                    io.github.thwisse.kentinsesi.R.id.action_update_status -> {
                        showUpdateStatusDialog()
                        true
                    }
                    io.github.thwisse.kentinsesi.R.id.action_delete -> {
                        currentPostId?.let { viewModel.deletePost(it) }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    
    /**
     * Post durumunu güncellemek için dialog göster
     */
    private fun setupStatusUpdates() {
        // Status count observer
        viewModel.statusUpdateCount.observe(viewLifecycleOwner) { count ->
            binding.tvStatusUpdates.text = getString(R.string.status_updates_count, count)
        }
        
        // Timeline'ı yükle
        currentPostId?.let { postId ->
            viewModel.loadStatusUpdates(postId)
        }
        
        // Güncellemeler butonuna tıklama
        binding.cardStatusUpdates.setOnClickListener {
            currentPostId?.let { postId ->
                val bundle = Bundle().apply {
                    putString("postId", postId)
                }
                findNavController().navigate(
                    R.id.action_postDetailFragment_to_statusUpdatesFragment,
                    bundle
                )
            }
        }
        
        // Status update sonucu observer
        viewModel.addStatusUpdateState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), R.string.status_update_success, Toast.LENGTH_SHORT).show()
                    // Post detail'i yenile (status değişti)
                    currentPostId?.let { viewModel.loadPostById(it) }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message ?: getString(R.string.status_update_error), Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> { }
            }
        }
    }
    
    
    private fun showUpdateStatusDialog() {
        val currentPostStatus = currentPost?.statusEnum ?: viewModel.currentPost.value?.statusEnum ?: PostStatus.NEW
        
        val bottomSheet = UpdateStatusBottomSheet.newInstance(currentPostStatus)
        bottomSheet.setOnStatusUpdateListener { status, note ->
            viewModel.addStatusUpdate(status, note)
        }
        bottomSheet.show(childFragmentManager, "UpdateStatusBottomSheet")
    }

    private fun observeOwnerActions() {
        viewModel.deletePostState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Post silindi", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is Resource.Error -> Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                is Resource.Loading -> { }
            }
        }

        viewModel.updateStatusState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val statusText = when (lastRequestedStatus) {
                        PostStatus.NEW -> "Yeni"
                        PostStatus.IN_PROGRESS -> "İşlemde"
                        PostStatus.RESOLVED -> "Çözüldü"
                        PostStatus.REJECTED -> "Reddedildi"
                        else -> "Güncellendi"
                    }
                    Toast.makeText(requireContext(), "Durum güncellendi: $statusText", Toast.LENGTH_SHORT).show()

                    // ViewModel optimistic update yaptı, ekranı da güncelle
                    viewModel.currentPost.value?.let { setupViews(it) }
                }
                is Resource.Error -> Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                is Resource.Loading -> { }
            }
        }
    }

    private fun setupViews(post: Post) {
        binding.apply {
            tvDetailTitle.text = post.title
            tvDetailDescription.text = post.description
            tvDetailCategory.text = post.category
            tvDetailDistrict.text = "Hatay, ${post.district ?: "-"}"

            tvDetailDate.text = post.createdAt?.toDate()?.let { date ->
                val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                format.format(date)
            } ?: ""

            tvDetailStatus.text = when (post.statusEnum) {
                PostStatus.NEW -> "Yeni"
                PostStatus.IN_PROGRESS -> "İşlemde"
                PostStatus.RESOLVED -> "Çözüldü"
                PostStatus.REJECTED -> "Reddedildi"
            }

            tvDetailUpvoteCount.text = "${post.upvoteCount} Destek"
            ivDetailImage.load(post.imageUrl) { crossfade(true) }
        }

        binding.btnDetailUpvote.setOnClickListener {
            val postId = currentPostId ?: return@setOnClickListener
            val userId = viewModel.currentUserId ?: return@setOnClickListener

            // Optimistic UI: anında sayıyı değiştir ve buton metnini güncelle
            val latest = currentPost ?: post
            val alreadyUpvoted = latest.upvotedBy.contains(userId)

            val newCount = (latest.upvoteCount + if (alreadyUpvoted) -1 else 1).coerceAtLeast(0)
            val newUpvotedBy = if (alreadyUpvoted) {
                latest.upvotedBy.filterNot { it == userId }
            } else {
                latest.upvotedBy + userId
            }

            val optimisticPost = latest.copy(
                upvoteCount = newCount,
                upvotedBy = newUpvotedBy
            )

            currentPost = optimisticPost
            setupViews(optimisticPost)

            viewModel.toggleUpvote(postId)
        }

        // Buton metni: desteklediyse geri çek, değilse destekle
        val userId = viewModel.currentUserId
        val isUpvotedByMe = userId != null && post.upvotedBy.contains(userId)
        binding.btnDetailUpvote.text = if (isUpvotedByMe) "Desteği Geri Çek" else "Destekle"
    }

    private fun setupMap(post: Post) {
        if (post.location != null) {
            postLocation = LatLng(post.location.latitude, post.location.longitude)
            val mapFragment = childFragmentManager.findFragmentById(io.github.thwisse.kentinsesi.R.id.mapFragment) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
        }
    }

    private fun setupComments() {
        binding.rvComments.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Başlangıçta yorumlar kapalıysa input alanı da kapalı olsun
        binding.inputLayout.isVisible = isCommentsSectionExpanded
        if (!isCommentsSectionExpanded) {
            binding.replyBanner.isVisible = false
        }

        binding.cardCommentsHeader.setOnClickListener {
            isCommentsSectionExpanded = !isCommentsSectionExpanded
            binding.rvComments.isVisible = isCommentsSectionExpanded
            binding.spaceCommentsBottom.isVisible = isCommentsSectionExpanded

            binding.inputLayout.isVisible = isCommentsSectionExpanded
            if (!isCommentsSectionExpanded) {
                exitReplyMode()
                binding.replyBanner.isVisible = false
            }
        }

        binding.btnSendComment.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            
            // ValidationUtils kullanarak yorum kontrolü
            val commentError = ValidationUtils.getValidationError("comment", text)
            
            when {
                text.isEmpty() -> {
                    binding.etComment.error = "Yorum boş olamaz"
                    binding.etComment.requestFocus()
                    return@setOnClickListener
                }
                commentError != null -> {
                    binding.etComment.error = commentError
                    binding.etComment.requestFocus()
                    return@setOnClickListener
                }
                currentPostId == null -> {
                    Toast.makeText(requireContext(), "Post ID bulunamadı", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val postId = currentPostId!!
            val replying = replyingTo
            if (replying != null) {
                viewModel.addReply(
                    postId = postId,
                    text = text,
                    parentCommentId = replying.id,
                    replyToAuthorId = replying.authorId,
                    replyToAuthorFullName = replying.authorFullName
                )
            } else {
                viewModel.addComment(postId, text)
            }

            binding.etComment.text.clear()
            binding.etComment.error = null
        }

        viewModel.commentsState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val all = resource.data ?: emptyList()
                    allThreadedComments = all

                    // Sadece silinmemiş yorumları say
                    val activeCommentCount = all.count { !it.isDeleted }
                    binding.tvCommentsHeader.text = "Yorumlar ($activeCommentCount yorum)"

                    val childCounts = buildChildCountByParentId(all)
                    commentAdapter.setChildCountByParentId(childCounts)
                    commentAdapter.setExpandedCommentIds(expandedCommentIds)

                    val visible = buildVisibleComments(all, expandedCommentIds)
                    // Force refresh - yeni liste göndermeden önce null gönder
                    commentAdapter.submitList(null)
                    commentAdapter.submitList(visible.toList())
                    if (isRefreshingComments) {
                        isRefreshingComments = false
                        if (!isRefreshingPost) {
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    if (isRefreshingComments) {
                        isRefreshingComments = false
                        if (!isRefreshingPost) {
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
                is Resource.Loading -> { }
            }
        }
        
        viewModel.addCommentState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Error -> {
                    binding.etComment.error = resource.message
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Success -> {
                    // Yorum başarıyla eklendi, yorumlar otomatik yenilenecek
                    hideKeyboardAndClearInputFocus()
                }
                is Resource.Loading -> { }
            }
        }
    }

    private fun toggleReplies(rootComment: Comment) {
        val id = rootComment.id
        if (id.isBlank()) return

        if (expandedCommentIds.contains(id)) {
            expandedCommentIds.remove(id)
        } else {
            expandedCommentIds.add(id)
        }

        commentAdapter.setExpandedCommentIds(expandedCommentIds)
        val visible = buildVisibleComments(allThreadedComments, expandedCommentIds)
        commentAdapter.submitList(visible)
    }

    private fun buildChildCountByParentId(all: List<Comment>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (c in all) {
            val parentId = c.parentCommentId
            if (!parentId.isNullOrBlank()) {
                counts[parentId] = (counts[parentId] ?: 0) + 1
            }
        }
        return counts
    }

    private fun buildVisibleComments(all: List<Comment>, expandedIds: Set<String>): List<Comment> {
        if (all.isEmpty()) return emptyList()

        val indexById = all.mapIndexedNotNull { index, c ->
            if (c.id.isBlank()) null else c.id to index
        }.toMap()

        val childrenByParent = all
            .filter { !it.parentCommentId.isNullOrBlank() }
            .groupBy { it.parentCommentId!! }
            .mapValues { (_, children) ->
                children.sortedBy { child -> indexById[child.id] ?: Int.MAX_VALUE }
            }

        val topLevel = all
            .filter { it.parentCommentId.isNullOrBlank() || it.depth == 0 }
            .sortedBy { c -> indexById[c.id] ?: Int.MAX_VALUE }

        val result = mutableListOf<Comment>()
        fun appendChildren(parent: Comment) {
            if (!expandedIds.contains(parent.id)) return
            val children = childrenByParent[parent.id].orEmpty()
            for (child in children) {
                result.add(child)
                appendChildren(child)
            }
        }

        for (root in topLevel) {
            result.add(root)
            appendChildren(root)
        }

        return result
    }
    
    /**
     * Yetkili kullanıcılar için butonları ayarla
     */
    private fun setupOfficialActions() {
        // Yetkili kontrolü için LiveData'ları observe et
        viewModel.canUpdateStatus.observe(viewLifecycleOwner) { canUpdate ->
            // Yetkili butonlarını göster/gizle
            // Bu butonlar layout'ta olmalı, şimdilik sadece menüde gösteriyoruz
        }
        
        // Post durumunu göster
        viewModel.currentPost.observe(viewLifecycleOwner) { post ->
            post?.let {
                // Post durumunu göster (opsiyonel)
                val statusText = when (it.statusEnum) {
                    PostStatus.NEW -> "Yeni"
                    PostStatus.IN_PROGRESS -> "İşleme Alındı"
                    PostStatus.RESOLVED -> "Çözüldü"
                    PostStatus.REJECTED -> "Reddedildi"
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        postLocation?.let { location ->
            googleMap.addMarker(MarkerOptions().position(location).title("Sorun Konumu"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}