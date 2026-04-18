package com.example.laush.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.laush.HomeActivity
import com.example.laush.R
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.DialogUserProfileBinding
import com.example.laush.model.User
import kotlinx.coroutines.launch

class UserProfileDialog(
    private val user: User,
    private val currentUserId: String,
    private val homeActivity: HomeActivity
) {
    private val repo = FirebaseRepo()
    private var isFollowing = false

    fun show() {
        val dialog = Dialog(homeActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogUserProfileBinding.inflate(LayoutInflater.from(homeActivity))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(-1, -2)

        binding.tvDisplayName.text = user.displayName
        binding.tvUsername.text = "@${user.username}"
        binding.tvFollowers.text = formatCount(user.followers)
        binding.tvFollowing.text = formatCount(user.following)

        user.photoUrl?.let { url ->
            Glide.with(homeActivity).load(url).circleCrop().into(binding.ivAvatar)
        }

        // Tik sistemi
        val tickRes = when {
            user.id == homeActivity.userId -> null
            user.followers >= 1_000_000 -> R.drawable.ic_tick_blue
            user.followers >= 100_000 -> R.drawable.ic_tick_red
            user.followers >= 10_000 -> R.drawable.ic_tick_yellow
            user.followers >= 1_000 -> R.drawable.ic_tick_bronze
            else -> null
        }
        if (tickRes != null) {
            binding.ivTick.setImageResource(tickRes)
            binding.ivTick.visibility = android.view.View.VISIBLE
        }

        // Takip durumu
        homeActivity.lifecycleScope.launch {
            val following = repo.getFollowing(currentUserId)
            isFollowing = following.any { it.id == user.id }
            updateFollowButton(binding)
        }

        binding.btnFollow.setOnClickListener {
            homeActivity.lifecycleScope.launch {
                if (isFollowing) {
                    repo.unfollowUser(currentUserId, user.id)
                    isFollowing = false
                } else {
                    repo.followUser(currentUserId, user.id)
                    isFollowing = true
                }
                updateFollowButton(binding)
                user.followers = if (isFollowing) user.followers + 1 else user.followers - 1
                binding.tvFollowers.text = formatCount(user.followers)
            }
        }

        binding.btnMessage.setOnClickListener {
            dialog.dismiss()
            homeActivity.lifecycleScope.launch {
                val chatRoomId = repo.getOrCreateChatRoom(currentUserId, user.id)
                val intent = android.content.Intent(homeActivity, ChatActivity::class.java)
                intent.putExtra(ChatActivity.EXTRA_CHAT_ROOM_ID, chatRoomId)
                intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, user.id)
                intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, user.displayName)
                intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NUMBER, user.userNumber)
                homeActivity.startActivity(intent)
            }
        }

        binding.root.setOnClickListener { dialog.dismiss() }
        binding.root.findViewById<android.view.View>(R.id.btnFollow)?.parent?.let { parent ->
            (parent as android.view.ViewGroup).setOnClickListener(null)
        }

        dialog.show()
    }

    private fun updateFollowButton(binding: DialogUserProfileBinding) {
        if (user.id == currentUserId) {
            binding.btnFollow.visibility = android.view.View.GONE
            binding.btnMessage.visibility = android.view.View.GONE
        } else {
            binding.btnFollow.text = if (isFollowing) "Takiptesin" else "Takip Et"
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> "${count / 1_000_000}M"
            count >= 1_000 -> "${count / 1_000}K"
            else -> count.toString()
        }
    }
}