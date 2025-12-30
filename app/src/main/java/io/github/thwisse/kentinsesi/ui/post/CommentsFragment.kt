package io.github.thwisse.kentinsesi.ui.post

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.databinding.FragmentCommentsBinding
import io.github.thwisse.kentinsesi.util.Resource

@AndroidEntryPoint
class CommentsFragment : Fragment(R.layout.fragment_comments) {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailViewModel by viewModels()
    
    private lateinit var commentAdapter: CommentAdapter
    private var currentPostId: String? = null
    private var replyingTo: Comment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCommentsBinding.bind(view)
        
        // Klavye açıldığında input layout'u yukarı taşı
        val inputLayout = binding.inputLayout
        val basePaddingBottom = inputLayout.paddingBottom
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(inputLayout) { v, insets ->
            val imeInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val bottomPadding = maxOf(imeInsets.bottom, systemBarsInsets.bottom)
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, basePaddingBottom + bottomPadding)
            insets
        }

        // Get postId from arguments
        currentPostId = arguments?.getString("postId")
        if (currentPostId == null) {
            Toast.makeText(requireContext(), getString(R.string.post_id_not_found), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupCommentAdapter()
        setupCommentInput()
        setupObservers()
        setupSwipeRefresh()

        // Load post (required for delete permission check - also loads current user)
        viewModel.loadPostById(currentPostId!!)
        
        // Load comments
        viewModel.getComments(currentPostId!!)
    }

    private fun setupCommentAdapter() {
        commentAdapter = CommentAdapter(
            onCommentClick = { comment ->
                enterReplyMode(comment)
            },
            onRepliesToggleClick = { rootComment ->
                // Toggle replies (empty - not needed in dedicated fragment)
            },
            onCommentLongClick = { comment ->
                if (!comment.isDeleted) {
                    showDeleteCommentDialog(comment)
                }
            }
        )
        binding.rvComments.adapter = commentAdapter
    }

    private fun setupCommentInput() {
        // Enable/disable send button based on input
        binding.etComment.addTextChangedListener { text ->
            binding.btnSendComment.isEnabled = !text.isNullOrBlank()
        }

        binding.btnSendComment.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                if (replyingTo != null) {
                    // Sending reply
                    viewModel.addReply(
                        postId = currentPostId!!,
                        text = text,
                        parentCommentId = replyingTo!!.id,
                        replyToAuthorId = replyingTo!!.authorId,
                        replyToAuthorFullName = replyingTo!!.authorFullName
                    )
                } else {
                    // Sending top-level comment
                    viewModel.addComment(currentPostId!!, text)
                }
                binding.etComment.text.clear()
            }
        }

        binding.btnCancelReply.setOnClickListener {
            cancelReplyMode()
        }
    }

    private fun setupObservers() {
        // Comments observer
        viewModel.commentsState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val comments = resource.data ?: emptyList()
                    commentAdapter.submitList(comments)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message ?: getString(R.string.error), Toast.LENGTH_SHORT).show()
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is Resource.Loading -> {}
            }
        }

        // Add comment result
        viewModel.addCommentState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.comment_added), Toast.LENGTH_SHORT).show()
                    hideKeyboard()
                    currentPostId?.let { viewModel.getComments(it) }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        resource.message ?: getString(R.string.comment_add_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Loading -> {}
            }
        }

        // Add reply result
        viewModel.addReplyState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.reply_added), Toast.LENGTH_SHORT).show()
                    cancelReplyMode()
                    hideKeyboard()
                    currentPostId?.let { viewModel.getComments(it) }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        resource.message ?: getString(R.string.reply_add_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Loading -> {}
            }
        }

        // Delete comment result
        viewModel.deleteCommentState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.comment_deleted), Toast.LENGTH_SHORT).show()
                    currentPostId?.let { viewModel.getComments(it) }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        resource.message ?: getString(R.string.comment_delete_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            currentPostId?.let {
                viewModel.getComments(it)
            }
        }
    }

    private fun enterReplyMode(comment: Comment) {
        replyingTo = comment
        binding.replyBanner.isVisible = true
        val name = comment.authorFullName.ifBlank { comment.authorUsername }
        binding.tvReplyBannerText.text = "$name kişisine yanıt"
        binding.etComment.requestFocus()
    }

    private fun cancelReplyMode() {
        replyingTo = null
        binding.replyBanner.isVisible = false
        binding.etComment.text.clear()
    }

    private fun showDeleteCommentDialog(comment: Comment) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_comment_title)
            .setMessage(R.string.dialog_delete_comment_message)
            .setPositiveButton("Sil") { _, _ ->
                viewModel.deleteComment(comment.id)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etComment.windowToken, 0)
        binding.etComment.clearFocus()
    }

    override fun onResume() {
        super.onResume()
        // Hide bottom navigation
        (activity as? io.github.thwisse.kentinsesi.ui.MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            io.github.thwisse.kentinsesi.R.id.bottom_nav_view
        )?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // Show bottom navigation
        (activity as? io.github.thwisse.kentinsesi.ui.MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            io.github.thwisse.kentinsesi.R.id.bottom_nav_view
        )?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
