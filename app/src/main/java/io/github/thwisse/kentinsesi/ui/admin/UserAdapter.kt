package io.github.thwisse.kentinsesi.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.thwisse.kentinsesi.data.model.User
import io.github.thwisse.kentinsesi.data.model.UserRole
import io.github.thwisse.kentinsesi.databinding.ItemUserAdminBinding

class UserAdapter(
    private val onRoleUpdate: (User, UserRole) -> Unit
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserAdminBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding, onRoleUpdate)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserViewHolder(
        private val binding: ItemUserAdminBinding,
        private val onRoleUpdate: (User, UserRole) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentUser: User? = null
        private var isInitializing = true

        fun bind(user: User) {
            currentUser = user
            
            binding.tvUserName.text = user.fullName.ifEmpty { "İsimsiz" }
            binding.tvUserEmail.text = user.email
            binding.tvUserLocation.text = if (user.city.isNotEmpty() && user.district.isNotEmpty()) {
                "${user.city}, ${user.district}"
            } else if (user.city.isNotEmpty()) {
                user.city
            } else if (user.district.isNotEmpty()) {
                user.district
            } else {
                "Konum belirtilmemiş"
            }

            // Role spinner'ı doldur
            val roles = listOf("Vatandaş", "Yetkili", "Admin")
            val roleAdapter = ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_spinner_item,
                roles
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerRole.adapter = roleAdapter

            // Mevcut rolü seç (listener'ı geçici olarak kaldır)
            isInitializing = true
            val currentRoleIndex = when (user.roleEnum) {
                UserRole.CITIZEN -> 0
                UserRole.OFFICIAL -> 1
                UserRole.ADMIN -> 2
            }
            binding.spinnerRole.setSelection(currentRoleIndex)
            isInitializing = false

            // Rol değiştiğinde güncelle
            binding.spinnerRole.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // İlk yüklemede çağrılmasını engelle
                    if (isInitializing) return
                    
                    val newRole = when (position) {
                        0 -> UserRole.CITIZEN
                        1 -> UserRole.OFFICIAL
                        2 -> UserRole.ADMIN
                        else -> UserRole.CITIZEN
                    }

                    // Sadece rol gerçekten değiştiyse güncelle
                    currentUser?.let { user ->
                        if (newRole != user.roleEnum) {
                            onRoleUpdate(user, newRole)
                        }
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}

