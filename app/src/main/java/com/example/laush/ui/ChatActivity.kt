package com.example.laush.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.ActivityChatBinding
import com.example.laush.model.ChatRoom
import com.example.laush.model.Message
import com.example.laush.model.User
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val repo = FirebaseRepo()
    private lateinit var adapter: MessageAdapter
    private var messages: List<Message> = emptyList()
    private var chatRoomId: String = ""
    private var currentUserId: String = ""
    private var otherUserId: String = ""
    private var listenerRegistration: ListenerRegistration? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUri ->
            sendPhoto(imageUri)
        }
    }

    companion object {
        const val EXTRA_CHAT_ROOM_ID = "chat_room_id"
        const val EXTRA_OTHER_USER_ID = "other_user_id"
        const val EXTRA_OTHER_USER_NAME = "other_user_name"
        const val EXTRA_OTHER_USER_NUMBER = "other_user_number"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatRoomId = intent.getStringExtra(EXTRA_CHAT_ROOM_ID) ?: ""
        otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: ""
        val otherUserName = intent.getStringExtra(EXTRA_OTHER_USER_NAME) ?: ""
        val otherUserNumber = intent.getStringExtra(EXTRA_OTHER_USER_NUMBER) ?: ""

        val prefs = getSharedPreferences("laush", MODE_PRIVATE)
        currentUserId = prefs.getString("user_id", "") ?: ""

        if (chatRoomId.isEmpty() || currentUserId.isEmpty()) {
            finish()
            return
        }

        binding.tvChatName.text = otherUserName
        binding.tvChatNumber.text = "#$otherUserNumber"
        
        // Profil fotoğrafı yükle
        lifecycleScope.launch {
            val user = repo.getUser(otherUserId)
            user?.photoUrl?.let { url ->
                com.bumptech.glide.Glide.with(this@ChatActivity)
                    .load(url)
                    .circleCrop()
                    .into(binding.ivChatAvatar)
            }
        }

        adapter = MessageAdapter(messages, currentUserId)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnTheme.setOnClickListener {
            showThemeDialog()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.btnAttachPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        setupRealTimeListener()
        
        loadTheme()
    }

    private fun setupRealTimeListener() {
        listenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("messages")
            .whereEqualTo("chatRoomId", chatRoomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Hata: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                messages = snapshot?.documents?.map { doc ->
                    Message(
                        id = doc.id,
                        chatRoomId = chatRoomId,
                        senderId = doc.getString("senderId") ?: "",
                        message = doc.getString("message") ?: "",
                        type = doc.getString("type") ?: "text",
                        isPinned = doc.getBoolean("isPinned") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                }?.sortedByDescending { it.isPinned }
                    ?.sortedByDescending { it.createdAt } ?: emptyList()

adapter = MessageAdapter(messages, currentUserId) { message ->
                    // Kalp atıldığında
                    Toast.makeText(this@ChatActivity, "❤️", Toast.LENGTH_SHORT).show()
                } { message ->
                    // Long press - pin/unpin
                    showPinDialog(message)
                }
                binding.rvMessages.adapter = adapter
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString().trim()
        if (message.isEmpty()) return

        lifecycleScope.launch {
            val canSend = repo.canMessage(currentUserId, otherUserId)
            if (!canSend) {
                Toast.makeText(this@ChatActivity, "Bu kişiyi takip etmelisin!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val result = repo.sendMessage(chatRoomId, currentUserId, message)
            result.onSuccess {
                binding.etMessage.text?.clear()
            }.onFailure { e ->
                Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendPhoto(imageUri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val result = repo.sendPhotoMessage(chatRoomId, currentUserId, imageUri, this@ChatActivity)
                result.onSuccess {
                    Toast.makeText(this@ChatActivity, "Fotoğraf gönderildi", Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showThemeDialog() {
        val themes = arrayOf("Mavi", "Pembe", "Yeşil", "Aşk ❤️", "Twin 👫", "Şero 💙")
        val colors = arrayOf("#2196F3", "#E91E63", "#4CAF50", "gradient_ask", "gradient_twin", "gradient_shero")
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Tema Seç")
            .setItems(themes) { _, which ->
                val colorName = colors[which]
                
                val bgDrawable = when (colorName) {
                    "gradient_ask" -> com.example.laush.R.drawable.bg_theme_ask
                    "gradient_twin" -> com.example.laush.R.drawable.bg_theme_twin
                    "gradient_shero" -> com.example.laush.R.drawable.bg_theme_shero
                    else -> {
                        val bgColor = android.graphics.Color.parseColor(colorName)
                        binding.root.setBackgroundColor(bgColor)
                        binding.inputLayout.setBackgroundColor(bgColor)
                        getSharedPreferences("laush", MODE_PRIVATE).edit().putString("chat_theme", colorName).apply()
                        null
                    }
                }
                
                if (bgDrawable != null) {
                    binding.root.background = android.content.res.Resources.getSystem().getDrawable(bgDrawable, null)
                    binding.inputLayout.background = android.content.res.Resources.getSystem().getDrawable(bgDrawable, null)
                    getSharedPreferences("laush", MODE_PRIVATE).edit().putString("chat_theme", colorName).apply()
                }
            }
            .show()
    }
    
    private fun loadTheme() {
        val themeColor = getSharedPreferences("laush", MODE_PRIVATE).getString("chat_theme", "#E91E63")
        
        when (themeColor) {
            "gradient_ask" -> {
                binding.root.background = android.content.res.Resources.getSystem().getDrawable(com.example.laush.R.drawable.bg_theme_ask, null)
                binding.inputLayout.background = android.content.res.Resources.getSystem().getDrawable(com.example.laush.R.drawable.bg_theme_ask, null)
            }
            "gradient_twin" -> {
                binding.root.background = android.content.res.Resources.getSystem().getDrawable(com.example.laush.R.drawable.bg_theme_twin, null)
                binding.inputLayout.background = android.content.res.Resources.getSystem().getDrawable(com.example.laush.R.drawable.bg_theme_twin, null)
            }
            "gradient_shero" -> {
                binding.root.background = android.content.res.Resources.getSystem().getDrawable(com.example.laush.R.drawable.bg_theme_shero, null)
                binding.inputLayout.background = android.content.res.Resources.getSystem().getDrawable(com.example.laush.R.drawable.bg_theme_shero, null)
            }
            else -> {
                val bgColor = android.graphics.Color.parseColor(themeColor!!)
                binding.root.setBackgroundColor(bgColor)
                binding.inputLayout.setBackgroundColor(bgColor)
            }
        }
    }

    private fun showPinDialog(message: Message) {
        val isOwnMessage = message.senderId == currentUserId
        if (!isOwnMessage) {
            Toast.makeText(this, "Sadece kendi mesajını başa tutturabilirsin", Toast.LENGTH_SHORT).show()
            return
        }

        val options = if (message.isPinned) arrayOf("Baştan Kaldır", "Mesajı Sil") else arrayOf("Başa Tuttur", "Mesajı Sil")
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Mesaj İşlemleri")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> togglePinMessage(message)
                    1 -> deleteMessage(message)
                }
            }
            .show()
    }

    private fun togglePinMessage(message: Message) {
        lifecycleScope.launch {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("messages")
                    .document(message.id)
                    .update("isPinned", !message.isPinned)
                
                // Önceki pinned mesajı kaldır (sadece 1 pinned olmalı)
                if (!message.isPinned) {
                    val pinnedMessages = messages.filter { it.isPinned && it.id != message.id }
                    pinnedMessages.forEach { pinnedMsg ->
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("messages")
                            .document(pinnedMsg.id)
                            .update("isPinned", false)
                    }
                }
                
                loadMessages()
                val status = if (message.isPinned) "Baştan kaldırıldı" else "Başa tutturuldu"
                Toast.makeText(this@ChatActivity, status, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteMessage(message: Message) {
        lifecycleScope.launch {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("messages")
                    .document(message.id)
                    .delete()
                loadMessages()
                Toast.makeText(this@ChatActivity, "Mesaj silindi", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            messages = repo.getMessages(chatRoomId)
            adapter = MessageAdapter(messages, currentUserId) { msg ->
                Toast.makeText(this@ChatActivity, "❤️", Toast.LENGTH_SHORT).show()
            } { msg ->
                showPinDialog(msg)
            }
            binding.rvMessages.adapter = adapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}