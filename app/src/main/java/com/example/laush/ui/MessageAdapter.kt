package com.example.laush.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.laush.databinding.ItemMessageReceivedBinding
import com.example.laush.databinding.ItemMessageSentBinding
import com.example.laush.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: String,
    private val onMessageDoubleTap: (Message) -> Unit = {},
    private val onMessageLongPress: (Message) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            SentViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ReceivedViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    inner class SentViewHolder(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            if (message.type == "image") {
                binding.tvMessage.visibility = View.GONE
                binding.ivPhoto.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(message.message)
                    .into(binding.ivPhoto)
            } else {
                binding.tvMessage.visibility = View.VISIBLE
                binding.ivPhoto.visibility = View.GONE
                binding.tvMessage.text = message.message
            }
            binding.tvTime.text = formatTime(message.createdAt)
            
            // Pin indicator
            binding.tvPinned.visibility = if (message.isPinned) View.VISIBLE else View.GONE
            
            binding.root.setOnLongClickListener {
                onMessageLongPress(message)
                true
            }
            
            binding.root.setOnClickListener {
                onMessageDoubleTap(message)
            }
        }
    }

    inner class ReceivedViewHolder(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            if (message.type == "image") {
                binding.tvMessage.visibility = View.GONE
                binding.ivPhoto.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(message.message)
                    .into(binding.ivPhoto)
            } else {
                binding.tvMessage.visibility = View.VISIBLE
                binding.ivPhoto.visibility = View.GONE
                binding.tvMessage.text = message.message
            }
            binding.tvTime.text = formatTime(message.createdAt)
            
            // Pin indicator
            binding.tvPinned.visibility = if (message.isPinned) View.VISIBLE else View.GONE
            
            binding.root.setOnLongClickListener {
                onMessageLongPress(message)
                true
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}