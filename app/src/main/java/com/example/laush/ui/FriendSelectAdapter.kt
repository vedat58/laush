package com.example.laush.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.laush.databinding.ItemFriendSelectBinding
import com.example.laush.model.User
import com.bumptech.glide.Glide

class FriendSelectAdapter(
    private val users: List<User>,
    private val onSelectionChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<FriendSelectAdapter.ViewHolder>() {

    private val selectedIds = mutableSetOf<String>()

    inner class ViewHolder(val binding: ItemFriendSelectBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.binding.apply {
            tvName.text = user.displayName
            user.photoUrl?.let { url ->
                    Glide.with(root.context).load(url).into(ivAvatar)
                }
            
            cbSelect.isChecked = selectedIds.contains(user.id)
            
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedIds.add(user.id) else selectedIds.remove(user.id)
                onSelectionChanged(selectedIds.toList())
            }
            
            root.setOnClickListener {
                cbSelect.isChecked = !cbSelect.isChecked
            }
        }
    }

    override fun getItemCount() = users.size
}