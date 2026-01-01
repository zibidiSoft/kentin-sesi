package io.github.thwisse.kentinsesi.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: MockNotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadMockNotifications()
    }
    
    private fun setupRecyclerView() {
        adapter = MockNotificationsAdapter()
        binding.rvNotifications.adapter = adapter
    }
    
    private fun loadMockNotifications() {
        val mockNotifications = listOf(
            MockNotification(
                type = NotificationType.COMMENT,
                title = "Ahmet Yılmaz paylaşımınıza yorum yaptı",
                message = "Çok haklısınız, bu soruna bir çözüm bulunmalı...",
                time = "2 saat önce",
                isRead = false
            ),
            MockNotification(
                type = NotificationType.UPVOTE,
                title = "Paylaşımınız destek aldı",
                message = "\"Büyük Çukur Sorunu\" başlıklı paylaşımınız 10 destek aldı!",
                time = "5 saat önce",
                isRead = false
            ),
            MockNotification(
                type = NotificationType.STATUS_UPDATE,
                title = "Paylaşımınızda durum güncellemesi",
                message = "\"Park Alanı Eksikliği\" durumu \"İşlemde\" olarak güncellendi",
                time = "1 gün önce",
                isRead = true
            ),
            MockNotification(
                type = NotificationType.REPLY,
                title = "Mehmet Demir yorumunuza yanıt verdi",
                message = "Ben de aynı sorunla karşılaşıyorum, teşekkürler...",
                time = "2 gün önce",
                isRead = true
            ),
            MockNotification(
                type = NotificationType.RESOLVED,
                title = "Paylaşımınız çözüldü! 🎉",
                message = "\"Sokak Aydınlatması\" şikayetiniz yetkililerce çözüldü",
                time = "3 gün önce",
                isRead = true
            ),
            MockNotification(
                type = NotificationType.COMMENT,
                title = "Ayşe Kaya paylaşımınıza yorum yaptı",
                message = "Bizim mahallede de aynı sorun var, ne yapacağız acaba?",
                time = "4 gün önce",
                isRead = true
            ),
            MockNotification(
                type = NotificationType.UPVOTE,
                title = "Paylaşımınız popüler!",
                message = "Son 24 saatte 25 destek aldınız",
                time = "5 gün önce",
                isRead = true
            )
        )
        
        adapter.submitList(mockNotifications)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Mock data classes
data class MockNotification(
    val type: NotificationType,
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean
)

enum class NotificationType {
    COMMENT,     // Yorum
    REPLY,       // Yanıt
    UPVOTE,      // Destek
    STATUS_UPDATE, // Durum güncellemesi
    RESOLVED     // Çözüldü
}