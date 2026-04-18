package com.example.laush.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.laush.databinding.ItemUserBinding
import com.example.laush.model.User
import com.bumptech.glide.Glide

class UserAdapter(
    private val users: List<User>,
    private val currentUserId: String,
    private val onUserClick: (User) -> Unit,
    private val onFollowClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var followingIds: Set<String> = emptySet()

    fun setFollowing(ids: List<String>) {
        followingIds = ids.toSet()
        notifyDataSetChanged()
    }

    inner class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.binding.apply {
            tvUsername.text = "@${user.username}"
            tvDisplayName.text = user.displayName
            
            user.photoUrl?.let { url ->
                Glide.with(root.context).load(url).into(ivAvatar)
            }
            
            val isFollowing = followingIds.contains(user.id)
            val isMe = user.id == currentUserId
            
            btnFollow.visibility = if (isMe) android.view.View.GONE else android.view.View.VISIBLE
            btnFollow.text = if (isFollowing) "Takiptesin" else "Takip Et"
            
            root.setOnClickListener { onUserClick(user) }
            btnFollow.setOnClickListener { onFollowClick(user) }
        }
    }

    override fun getItemCount() = users.size
}