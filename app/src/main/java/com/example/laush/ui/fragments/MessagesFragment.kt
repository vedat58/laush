package com.example.laush.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.laush.HomeActivity
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.FragmentMessagesBinding
import com.example.laush.R
import com.example.laush.model.ChatRoom
import com.example.laush.model.User
import com.example.laush.ui.ChatActivity
import com.example.laush.ui.ChatRoomAdapter
import com.example.laush.ui.FriendSelectAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class MessagesFragment : Fragment() {
    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private val repo = FirebaseRepo()
    private lateinit var adapter: ChatRoomAdapter
    private var chatRooms: List<ChatRoom> = emptyList()
    private var listenerRegistration: ListenerRegistration? = null
    private val storage = FirebaseStorage.getInstance()

    private val storyPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadStory(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = ChatRoomAdapter(chatRooms) { chatRoom ->
            openChat(chatRoom)
        }
        
        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChats.adapter = adapter
        
        binding.btnNotifications.setOnClickListener {
            showNotifications()
        }
        
        binding.btnCreateGroup.setOnClickListener {
            showCreateGroupDialog()
        }
        
binding.fabStory.setOnClickListener {
            pickStory()
        }
        
        loadChats()
    }

    private fun pickStory() {
        val intents = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        storyPicker.launch(intents)
    }

    private fun uploadStory(uri: Uri) {
        Toast.makeText(requireContext(), "Hikaye yükleniyor...", Toast.LENGTH_SHORT).show()
        val homeActivity = activity as? HomeActivity ?: return
        val storyRef = storage.reference.child("stories/${homeActivity.userId}/${System.currentTimeMillis()}")
        
        storyRef.putFile(uri)
            .addOnSuccessListener {
                storyRef.downloadUrl.addOnSuccessListener { url ->
                    lifecycleScope.launch {
                        val storyData = mapOf(
                            "userId" to homeActivity.userId,
                            "url" to url.toString(),
                            "createdAt" to System.currentTimeMillis(),
                            "expiresAt" to (System.currentTimeMillis() + 24*60*60*1000)
                        )
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("stories").document().set(storyData)
                        Toast.makeText(requireContext(), "Hikaye paylaşıldı!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showNotifications() {
        val homeActivity = activity as? HomeActivity ?: return
        lifecycleScope.launch {
            val notifications = repo.getNotifications(homeActivity.userId)
            val text = if (notifications.isEmpty()) "Bildirim yok"
            else notifications.joinToString("\n") { it.message }
            
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Bildirimler")
                .setMessage(text)
                .setPositiveButton("Kapat", null)
                .show()
        }
    }

    private fun showCreateGroupDialog() {
        val homeActivity = activity as? HomeActivity ?: return
        lifecycleScope.launch {
            val followings = repo.getFollowings(homeActivity.userId)
            val friends = followings.mapNotNull { repo.getUser(it) }
            
            if (friends.isEmpty()) {
                Toast.makeText(requireContext(), "Önce arkadaş ekle!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_create_group)
            dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            
            val etGroupName = dialog.findViewById<android.widget.EditText>(R.id.etGroupName)
            val rvMembers = dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvMembers)
            val btnCreate = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCreate)
            val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
            val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
            
            val adapter = FriendSelectAdapter(friends) { selectedIds -> }
            rvMembers.layoutManager = LinearLayoutManager(requireContext())
            rvMembers.adapter = adapter
            
            btnClose.setOnClickListener { dialog.dismiss() }
            btnCancel.setOnClickListener { dialog.dismiss() }
            
            btnCreate.setOnClickListener {
                val groupName = etGroupName.text.toString()
                if (groupName.isEmpty()) {
                    Toast.makeText(requireContext(), "Grup adı gir!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val selectedIds = (0 until rvMembers.adapter?.itemCount!!).mapNotNull { pos ->
                    val viewHolder = rvMembers.findViewHolderForAdapterPosition(pos) as? com.example.laush.ui.FriendSelectAdapter.ViewHolder
                    if (viewHolder?.binding?.cbSelect?.isChecked == true) friends[pos].id else null
                }
                
                lifecycleScope.launch {
                    selectedIds.forEach { memberId ->
                        repo.getOrCreateChatRoom(homeActivity.userId, memberId)
                    }
                    Toast.makeText(requireContext(), "Grup oluşturuldu!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            
            dialog.show()
        }
    }

    private fun loadChats() {
        val homeActivity = activity as? HomeActivity ?: return
        
        // Real-time listener for chat rooms
        listenerRegistration?.remove()
        listenerRegistration = FirebaseFirestore.getInstance()
            .collection("chatRooms")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                lifecycleScope.launch {
                    chatRooms = repo.getChatRooms(homeActivity.userId)
                    adapter = ChatRoomAdapter(chatRooms) { chatRoom ->
                        openChat(chatRoom)
                    }
                    binding.rvChats.adapter = adapter
                }
            }
    }

    private fun openChat(chatRoom: ChatRoom) {
        val homeActivity = activity as? HomeActivity ?: return
        lifecycleScope.launch {
            val otherUserId = if (chatRoom.user1 == homeActivity.userId) chatRoom.user2 else chatRoom.user1
            val otherUser = repo.getUser(otherUserId)
            
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_CHAT_ROOM_ID, chatRoom.id)
                putExtra(ChatActivity.EXTRA_OTHER_USER_ID, otherUserId)
                putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, otherUser?.displayName ?: "")
                putExtra(ChatActivity.EXTRA_OTHER_USER_NUMBER, otherUser?.userNumber ?: "")
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }
}