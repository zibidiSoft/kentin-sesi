package io.github.thwisse.kentinsesi.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentAdminPanelBinding
import io.github.thwisse.kentinsesi.data.model.User
import io.github.thwisse.kentinsesi.data.model.UserRole
import io.github.thwisse.kentinsesi.util.Resource

@AndroidEntryPoint
class AdminPanelFragment : Fragment(R.layout.fragment_admin_panel) {

    private var _binding: FragmentAdminPanelBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminPanelViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminPanelBinding.bind(view)

        // Admin kontrolü - LiveData observe ile yapılmalı (asenkron yükleme nedeniyle)
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                // Kullanıcı bilgisi henüz yüklenmedi, bekle
                return@observe
            }

            if (!user.isAdmin) {
                Toast.makeText(requireContext(), "Bu sayfaya erişim yetkiniz yok.", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return@observe
            }

            // Admin ise UI'ı kur
            setupRecyclerView()
            setupObservers()
            setupSwipeRefresh()

            binding.btnRefresh.setOnClickListener {
                viewModel.loadAllUsers()
            }
        }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter { user, newRole ->
            viewModel.updateUserRole(user.uid, newRole)
        }

        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadAllUsers()
        }
    }

    private fun setupObservers() {
        viewModel.usersState.observe(viewLifecycleOwner) { resource ->
            binding.swipeRefreshLayout.isRefreshing = resource is Resource.Loading

            when (resource) {
                is Resource.Success -> {
                    userAdapter.submitList(resource.data ?: emptyList())
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    // Loading durumu
                }
            }
        }

        viewModel.updateRoleState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Rol güncellendi", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    // Loading durumu
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

