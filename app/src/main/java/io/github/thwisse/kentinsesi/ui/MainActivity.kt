package io.github.thwisse.kentinsesi.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.ActivityMainBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge padding ayarları
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // BottomNav olduğu için bottom padding'i 0 yapabilir veya view'a özel verebiliriz
            // Şimdilik standart bırakıyorum, üst barın durum çubuğu altında kalmaması için top padding önemli
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // NavHost'u bul
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // --- TOOLBAR KURULUMU (YENİ) ---
        // 1. Toolbar'ı Activity'nin Action Bar'ı olarak ayarla
        setSupportActionBar(binding.toolbar)

        // 2. Alt menüdeki ana sayfaları belirle (Bunlarda "Geri" oku çıkmaz)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_map,
                R.id.nav_notifications,
                R.id.nav_profile
            )
        )

        // 3. Toolbar başlığının ve geri butonunun Navigasyonla otomatik değişmesini sağla
        setupActionBarWithNavController(navController, appBarConfiguration)

        // --- ALT MENÜ KURULUMU ---
        binding.bottomNavView.setupWithNavController(navController)
        
        // Bottom Navigation View'ı belirli ekranlarda gizle
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // PostDetail ve CreatePost ekranlarında bottom nav'i gizle
            val hideBottomNav = destination.id == R.id.postDetailFragment || 
                               destination.id == R.id.createPostFragment ||
                               destination.id == R.id.locationPickerFragment
            
            binding.bottomNavView.visibility = if (hideBottomNav) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
        }
    }

    // Geri butonuna (Toolbar üzerindeki ok) basıldığında çalışması için gerekli
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}