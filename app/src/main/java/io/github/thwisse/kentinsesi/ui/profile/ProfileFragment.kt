package io.github.thwisse.kentinsesi.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.databinding.FragmentProfileBinding
import io.github.thwisse.kentinsesi.ui.AuthActivity
import io.github.thwisse.kentinsesi.ui.home.PostAdapter
import io.github.thwisse.kentinsesi.util.Resource
import io.github.thwisse.kentinsesi.util.LocaleHelper
import io.github.thwisse.kentinsesi.util.loadAvatar

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter
    private lateinit var commentAdapter: UserCommentAdapter

    private var isAdminUser: Boolean = false
    private var isPostsTabSelected: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        // Tab state'i restore et
        savedInstanceState?.let {
            isPostsTabSelected = it.getBoolean("isPostsTabSelected", true)
        }

        setupMenu()
        setupUserInfo()
        setupRecyclerViews()
        setupTabs()
        setupSwipeRefresh()
        setupObservers()
        
        // Başlangıçta doğru tab'ı göster
        if (isPostsTabSelected) {
            showPostsTab()
        } else {
            showCommentsTab()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isPostsTabSelected", isPostsTabSelected)
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_profile, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.action_admin_panel)?.isVisible = isAdminUser
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_language -> {
                        showLanguageDialog()
                        true
                    }
                    R.id.action_admin_panel -> {
                        findNavController().navigate(R.id.action_nav_profile_to_adminPanelFragment)
                        true
                    }
                    R.id.action_theme -> {
                        showThemeDialog()
                        true
                    }
                    R.id.action_logout -> {
                        doLogout()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showLanguageDialog() {
        val currentLanguage = LocaleHelper.getPersistedLanguage(requireContext())
        
        val options = arrayOf(
            getString(R.string.language_system),
            getString(R.string.language_turkish),
            getString(R.string.language_english)
        )
        val values = arrayOf(LocaleHelper.LANGUAGE_SYSTEM, LocaleHelper.LANGUAGE_TURKISH, LocaleHelper.LANGUAGE_ENGLISH)
        val checkedIndex = values.indexOf(currentLanguage).takeIf { it >= 0 } ?: 0

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.language_select_title))
            .setSingleChoiceItems(options, checkedIndex) { dialog, which ->
                val selected = values.getOrNull(which) ?: LocaleHelper.LANGUAGE_SYSTEM
                if (selected != currentLanguage) {
                    LocaleHelper.setLocaleAndRestart(requireContext(), selected)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showThemeDialog() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val current = prefs.getString("theme_mode", "system")

        val options = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        val values = arrayOf("system", "light", "dark")
        val checkedIndex = values.indexOf(current).takeIf { it >= 0 } ?: 0

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.theme_select_title))
            .setSingleChoiceItems(options, checkedIndex) { dialog, which ->
                val selected = values.getOrNull(which) ?: "system"
                prefs.edit().putString("theme_mode", selected).apply()

                when (selected) {
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }

                requireActivity().recreate()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun doLogout() {
        viewModel.signOut()
        val intent = Intent(requireContext(), AuthActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun setupUserInfo() {
        // Email'i Firebase User'dan göster
        val firebaseUser = viewModel.currentUser
        binding.tvUserEmail.text = firebaseUser?.email ?: "Kullanıcı"
        
        // Profil bilgilerini (fullName, city, district) User model'den göster
        viewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val user = resource.data
                    if (user != null) {
                        val newIsAdmin = user.isAdmin
                        if (newIsAdmin != isAdminUser) {
                            isAdminUser = newIsAdmin
                            requireActivity().invalidateOptionsMenu()
                        }

                        // Kullanıcı adını göster
                        binding.tvUserName.text = user.fullName.ifEmpty { "İsim Belirtilmemiş" }
                        
                        // Avatar yükle
                        binding.ivProfileAvatar.loadAvatar(user.avatarSeed)

                        val username = user.username.trim().takeIf { it.isNotBlank() }
                        binding.tvUserUsername.visibility = if (username != null) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                        binding.tvUserUsername.text = if (username != null) "@$username" else ""
                        
                        // Şehir ve ilçe bilgisini birleştirerek göster
                        val locationText = when {
                            user.city.isNotEmpty() && user.district.isNotEmpty() -> 
                                "${user.city}, ${user.district}"
                            user.city.isNotEmpty() -> user.city
                            user.district.isNotEmpty() -> user.district
                            else -> "Konum Belirtilmemiş"
                        }
                        binding.tvUserLocation.text = locationText
                    } else {
                        // Data null ise varsayılan değerleri göster
                        if (isAdminUser) {
                            isAdminUser = false
                            requireActivity().invalidateOptionsMenu()
                        }
                        binding.tvUserName.text = "Kullanıcı"
                        binding.tvUserUsername.visibility = View.GONE
                        binding.tvUserUsername.text = ""
                        binding.tvUserLocation.text = "Konum Belirtilmemiş"
                    }
                }
                is Resource.Error -> {
                    // Hata durumunda varsayılan değerleri göster
                    if (isAdminUser) {
                        isAdminUser = false
                        requireActivity().invalidateOptionsMenu()
                    }
                    binding.tvUserName.text = "Kullanıcı"
                    binding.tvUserUsername.visibility = View.GONE
                    binding.tvUserUsername.text = ""
                    binding.tvUserLocation.text = "Konum Belirtilmemiş"
                }
                is Resource.Loading -> {
                    // Loading durumu
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        // Posts adapter
        postAdapter = PostAdapter(
            onItemClick = { post ->
                val bundle = Bundle().apply { putString("postId", post.id) }
                findNavController().navigate(R.id.action_nav_profile_to_postDetailFragment, bundle)
            }
        )
        binding.rvUserPosts.adapter = postAdapter

        // Comments adapter
        commentAdapter = UserCommentAdapter(
            onItemClick = { comment ->
                // Yorumun ait olduğu post'a git
                val bundle = Bundle().apply { putString("postId", comment.postId) }
                // PostId boş olabilir (eski yorumlar için)
                if (comment.postId.isNotBlank()) {
                    // Post'un var olup olmadığını kontrol et
                    viewModel.checkPostExists(comment.postId) { exists ->
                        if (exists) {
                            findNavController().navigate(R.id.action_nav_profile_to_postDetailFragment, bundle)
                        } else {
                            Toast.makeText(requireContext(), "Bu post silinmiş", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Post detayına gidilemiyor", Toast.LENGTH_SHORT).show()
                }
            },
            onItemLongClick = { comment ->
                if (comment.isDeleted) return@UserCommentAdapter
                showDeleteCommentDialog(comment)
            }
        )
        binding.rvUserComments.adapter = commentAdapter
    }

    private fun showDeleteCommentDialog(comment: Comment) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_comment_title)
            .setMessage(R.string.dialog_delete_comment_message)
            .setPositiveButton("Sil") { _, _ ->
                viewModel.deleteComment(comment)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun setupTabs() {
        binding.tvTabPosts.setOnClickListener {
            selectPostsTab()
        }

        binding.tvTabComments.setOnClickListener {
            selectCommentsTab()
        }
    }

    private fun selectPostsTab() {
        if (isPostsTabSelected) return
        isPostsTabSelected = true
        showPostsTab()
    }

    private fun selectCommentsTab() {
        if (!isPostsTabSelected) return
        isPostsTabSelected = false
        showCommentsTab()
    }
    
    // UI güncelleme - state restore için kullanılır
    private fun showPostsTab() {
        // Tab text styles
        binding.tvTabPosts.setTextColor(requireContext().getColor(R.color.colorPrimary))
        binding.tvTabPosts.setTypeface(null, android.graphics.Typeface.BOLD)
        binding.tvTabComments.setTextColor(requireContext().getColor(android.R.color.darker_gray))
        binding.tvTabComments.setTypeface(null, android.graphics.Typeface.NORMAL)

        // Indicator position
        updateTabIndicator(binding.tvTabPosts)

        // Show/hide RecyclerViews
        binding.rvUserPosts.visibility = View.VISIBLE
        binding.rvUserComments.visibility = View.GONE
    }

    // UI güncelleme - state restore için kullanılır
    private fun showCommentsTab() {
        // Tab text styles
        binding.tvTabComments.setTextColor(requireContext().getColor(R.color.colorPrimary))
        binding.tvTabComments.setTypeface(null, android.graphics.Typeface.BOLD)
        binding.tvTabPosts.setTextColor(requireContext().getColor(android.R.color.darker_gray))
        binding.tvTabPosts.setTypeface(null, android.graphics.Typeface.NORMAL)

        // Indicator position
        updateTabIndicator(binding.tvTabComments)

        // Show/hide RecyclerViews
        binding.rvUserPosts.visibility = View.GONE
        binding.rvUserComments.visibility = View.VISIBLE
    }

    private fun updateTabIndicator(targetTab: View) {
        val constraintLayout = binding.root as ConstraintLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(
            binding.tabIndicator.id,
            ConstraintSet.START,
            targetTab.id,
            ConstraintSet.START
        )
        constraintSet.connect(
            binding.tabIndicator.id,
            ConstraintSet.END,
            targetTab.id,
            ConstraintSet.END
        )
        constraintSet.applyTo(constraintLayout)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshAll()
        }
    }
    
    private fun setupObservers() {
        // UserPosts observer
        viewModel.userPosts.observe(viewLifecycleOwner) { resource ->
            if (isPostsTabSelected) {
                binding.swipeRefreshLayout.isRefreshing = resource is Resource.Loading
            }
            
            when(resource) {
                is Resource.Success -> {
                    postAdapter.submitList(resource.data)
                    if (isPostsTabSelected) {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    if (isPostsTabSelected) {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
                is Resource.Loading -> { }
            }
        }

        // UserComments observer
        viewModel.userComments.observe(viewLifecycleOwner) { resource ->
            if (!isPostsTabSelected) {
                binding.swipeRefreshLayout.isRefreshing = resource is Resource.Loading
            }

            when(resource) {
                is Resource.Success -> {
                    commentAdapter.submitList(resource.data)
                    if (!isPostsTabSelected) {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    if (!isPostsTabSelected) {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
                is Resource.Loading -> { }
            }
        }
        
        // UserProfile observer
        viewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Loading && 
                viewModel.userPosts.value !is Resource.Loading &&
                viewModel.userComments.value !is Resource.Loading) {
                binding.swipeRefreshLayout.isRefreshing = true
            } else if (resource !is Resource.Loading && 
                       viewModel.userPosts.value !is Resource.Loading &&
                       viewModel.userComments.value !is Resource.Loading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        viewModel.totalPostsCount.observe(viewLifecycleOwner) { count ->
            binding.tvPostCount.text = count.toString()
        }
        viewModel.resolvedPostsCount.observe(viewLifecycleOwner) { count ->
            binding.tvResolvedCount.text = count.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}