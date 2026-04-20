package com.example.laush.ui

import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.laush.databinding.ItemPostBinding
import com.example.laush.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter(
    private val posts: List<Post>,
    private val currentUserId: String = "",
    private val onLikeClick: (Post) -> Unit = {},
    private val onCommentClick: (Post) -> Unit = {}
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.binding.apply {
            tvUsername.text = "@${post.username}"
            tvContent.text = post.content
            tvLikes.text = "❤️ ${post.likes}"
            tvComments.text = "💬 ${post.comments}"
            tvTime.text = formatTime(post.createdAt)

            btnLike.setOnClickListener { onLikeClick(post) }
            btnComment.setOnClickListener { onCommentClick(post) }

            val gestureDetector = GestureDetector(root.context, object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onLikeClick(post)
                    return true
                }
            })
            root.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event); false }
        }
    }

    override fun getItemCount() = posts.size

    private fun formatTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "az önce"
            diff < 3_600_000 -> "${diff / 60_000} dk"
            diff < 86_400_000 -> "${diff / 3_600_000} sa"
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
        }
    }
}