package io.github.thwisse.kentinsesi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentHomeBinding
import io.github.thwisse.kentinsesi.util.Resource

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setupMenu() {
        // MenuProvider ile Toolbar menüsünü bağlıyoruz
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_post -> {
                        // Artı butonuna basılınca CreatePost ekranına git
                        findNavController().navigate(R.id.action_homeFragment_to_createPostFragment)
                        true
                    }
                    R.id.action_filter -> {
                        // Mevcut filtre değerlerini ViewModel'den al
                        val bundle = android.os.Bundle().apply {
                            viewModel.lastDistricts?.let { putStringArrayList("districts", ArrayList(it)) }
                            viewModel.lastCategories?.let { putStringArrayList("categories", ArrayList(it)) }
                            viewModel.lastStatuses?.let { putStringArrayList("statuses", ArrayList(it)) }
                        }

                        // BottomSheet'i bu verilerle aç
                        findNavController().navigate(R.id.action_homeFragment_to_filterBottomSheetFragment, bundle)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    // ViewModel ve Adapter tanımları
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter // <-- BUNU EKLE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupMenu()
        setupSwipeRefresh() // YENİ: Bunu çağır
        observePosts()

        // Filtre ekranından gelen sonuçları dinle
        setFragmentResultListener("filter_request") { _, bundle ->
            val districts = bundle.getStringArrayList("districts")
            val categories = bundle.getStringArrayList("categories")
            val statuses = bundle.getStringArrayList("statuses")

            // Listeyi temizle (opsiyonel görsel iyileştirme) veya direkt yüklemeye geç
            viewModel.getPosts(
                districts = districts?.toList(),
                categories = categories?.toList(),
                statuses = statuses?.toList()
            )

            // Kullanıcıya bilgi ver
            Toast.makeText(requireContext(), "Filtreler uygulandı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshPosts() // YENİ FONKSİYONU ÇAĞIRIYORUZ
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = viewModel.currentUserId

        postAdapter = PostAdapter(
            currentUserId = currentUserId,
            onUpvoteClick = { clickedPost ->
                viewModel.toggleUpvote(clickedPost)
            },
            onItemClick = { clickedPost ->
                // Karta tıklandı -> Detay sayfasına git (sadece post ID gönder)
                val bundle = android.os.Bundle().apply {
                    putString("postId", clickedPost.id)
                }
                findNavController().navigate(R.id.action_homeFragment_to_postDetailFragment, bundle)
            }
        )

        binding.rvPosts.adapter = postAdapter
    }

    private fun observePosts() {
        viewModel.postsState.observe(viewLifecycleOwner) { resource ->
            // Yükleniyor durumunda SwipeRefresh'in dönen dairesini göster/gizle
            binding.swipeRefreshLayout.isRefreshing = resource is Resource.Loading

            when (resource) {
                is Resource.Success -> {
                    postAdapter.submitList(resource.data)
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    // Hata olsa bile dönmeyi durdur
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is Resource.Loading -> {
                    // isRefreshing = true zaten yukarıda yapıldı
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}