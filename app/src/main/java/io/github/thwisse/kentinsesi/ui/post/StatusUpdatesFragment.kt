package io.github.thwisse.kentinsesi.ui.post

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentStatusUpdatesBinding
import io.github.thwisse.kentinsesi.util.Resource

@AndroidEntryPoint
class StatusUpdatesFragment : Fragment(R.layout.fragment_status_updates) {

    private var _binding: FragmentStatusUpdatesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailViewModel by viewModels()
    private val adapter = StatusUpdateAdapter()

    private var currentPostId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusUpdatesBinding.bind(view)

        // PostId'yi al
        currentPostId = arguments?.getString("postId")
        if (currentPostId == null) {
            Toast.makeText(requireContext(), "Post ID bulunamadı", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupRecyclerView()
        setupSwipeRefresh()
        setupObservers()

        // İlk yükleme
        viewModel.loadStatusUpdates(currentPostId!!)
    }

    private fun setupRecyclerView() {
        binding.rvStatusUpdates.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            currentPostId?.let {
                viewModel.loadStatusUpdates(it)
            }
        }
    }

    private fun setupObservers() {
        viewModel.statusUpdates.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    val updates = resource.data ?: emptyList()
                    
                    if (updates.isEmpty()) {
                        binding.tvEmptyState.isVisible = true
                        binding.rvStatusUpdates.isVisible = false
                    } else {
                        binding.tvEmptyState.isVisible = false
                        binding.rvStatusUpdates.isVisible = true
                        adapter.submitList(updates)
                    }
                }
                is Resource.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    if (!binding.swipeRefreshLayout.isRefreshing) {
                        // İlk yükleme için loading indicator buraya eklenebilir
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
