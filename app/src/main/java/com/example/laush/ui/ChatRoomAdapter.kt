package com.example.laush.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.laush.databinding.ItemChatRoomBinding
import com.example.laush.model.ChatRoom

class ChatRoomAdapter(
    private val chatRooms: List<ChatRoom>,
    private val onChatClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder>() {

    inner class ChatRoomViewHolder(val binding: ItemChatRoomBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatRoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val chat = chatRooms[position]
        holder.binding.apply {
            tvName.text = chat.otherUserName
            tvNumber.text = "#${chat.otherUserNumber}"
            tvLastMessage.text = chat.lastMessage
            
            if (!chat.otherUserPhoto.isNullOrEmpty()) {
                Glide.with(root.context)
                    .load(chat.otherUserPhoto)
                    .circleCrop()
                    .into(ivAvatar)
            }
            
            root.setOnClickListener { onChatClick(chat) }
        }
    }

    override fun getItemCount() = chatRooms.size
}