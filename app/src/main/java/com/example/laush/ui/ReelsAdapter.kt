package com.example.laush.ui

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.example.laush.HomeActivity
import com.example.laush.R
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.DialogShareVideoBinding
import com.example.laush.databinding.ItemReelsBinding
import com.example.laush.model.Post
import kotlinx.coroutines.launch

class ReelsAdapter(
    private val posts: List<Post>,
    private val onLike: (Post) -> Unit,
    private val onShare: (Post) -> Unit,
    private val onComment: (Post, String) -> Unit
) : RecyclerView.Adapter<ReelsAdapter.ViewHolder>() {

    private var likedPosts = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReelsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount() = posts.size

    fun stopCurrentVideo(position: Int) {
    }

    inner class ViewHolder(private val binding: ItemReelsBinding) : RecyclerView.ViewHolder(binding.root) {
        private var player: ExoPlayer? = null
        private var currentPost: Post? = null

        fun bind(post: Post, forceStop: Boolean = false) {
            if (forceStop) {
                player?.release()
                player = null
                return
            }
            
            currentPost = post
            binding.tvUsername.text = "@${post.username}"
            binding.tvCaption.text = post.content
            binding.tvCategory.text = post.category
            binding.tvLikeCount.text = post.likes.toString()
            binding.tvCommentCount.text = post.comments.toString()

            // Kalp durumu
            val isLiked = likedPosts.contains(post.id)
            binding.btnLike.setImageResource(if (isLiked) R.drawable.ic_heart_red else R.drawable.ic_heart_outline)

            binding.root.setOnClickListener {
                if (player?.isPlaying == true) {
                    player?.pause()
                } else {
                    player?.play()
                }
            }

            binding.btnLike.setOnClickListener {
                val currentLikes = post.likes
                if (likedPosts.contains(post.id)) {
                    likedPosts.remove(post.id)
                    binding.btnLike.setImageResource(R.drawable.ic_heart_outline)
                    binding.tvLikeCount.text = (currentLikes - 1).toString()
                } else {
                    likedPosts.add(post.id)
                    binding.btnLike.setImageResource(R.drawable.ic_heart_red)
                    binding.tvLikeCount.text = (currentLikes + 1).toString()
                }
                onLike(post)
            }

            binding.btnShare.setOnClickListener {
                onShare(post)
            }

            binding.btnSendComment.setOnClickListener {
                val commentText = binding.etComment.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    val newCommentCount = post.comments + 1
                    binding.tvCommentCount.text = newCommentCount.toString()
                    onComment(post, commentText)
                    binding.etComment.text?.clear()
                    binding.commentsSection.visibility = View.GONE
                }
            }

            binding.btnComment.setOnClickListener {
                binding.commentsSection.visibility = if (binding.commentsSection.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }

            player?.release()
            player = ExoPlayer.Builder(binding.root.context).build().also {
                binding.playerView.player = it
                post.videoUrl?.let { url ->
                    it.setMediaItem(MediaItem.fromUri(url))
                    it.repeatMode = Player.REPEAT_MODE_ONE
                    it.prepare()
                    it.play()
                }
            }
        }

        fun stop() {
            player?.release()
            player = null
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.stop()
    }
}