package com.example.laush.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.laush.HomeActivity
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.FragmentHomeBinding
import com.example.laush.model.Post
import com.example.laush.ui.PostAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val repo = FirebaseRepo()
    private lateinit var adapter: PostAdapter
    private var posts: List<Post> = emptyList()
    private var suggestedPlayer: ExoPlayer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = PostAdapter(posts, currentUserId = (activity as? HomeActivity)?.userId ?: "", onLikeClick = { post ->
            lifecycleScope.launch {
                repo.toggleLike(post.id, (activity as? HomeActivity)?.userId ?: "")
                loadPosts()
            }
        }, onCommentClick = { post ->
            showComments(post)
        })
        
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter
        
        loadSuggestedVideo()
        loadPosts()
    }

    override fun onResume() {
        super.onResume()
        suggestedPlayer?.play()
        loadPosts()
    }

    override fun onPause() {
        super.onPause()
        suggestedPlayer?.pause()
    }

    private fun loadSuggestedVideo() {
        lifecycleScope.launch {
            val video = repo.getRandomVideo()
            video?.let {
                binding.tvSuggestedUser.text = "@${it.username}"
                binding.tvSuggestedCaption.text = it.content
                
                it.videoUrl?.let { url ->
                    suggestedPlayer = ExoPlayer.Builder(requireContext()).build().also { player ->
                        binding.playerSuggested.player = player
                        val mediaItem = MediaItem.fromUri(url)
                        player.setMediaItem(mediaItem)
                        player.repeatMode = Player.REPEAT_MODE_ONE
                        player.prepare()
                        player.play()
                    }
                }
            }
        }
    }

    private fun loadPosts() {
        lifecycleScope.launch {
            // Only text posts
            posts = repo.getTextPosts()
            adapter = PostAdapter(posts, currentUserId = (activity as? HomeActivity)?.userId ?: "", onLikeClick = { post ->
                lifecycleScope.launch {
                    repo.toggleLike(post.id, (activity as? HomeActivity)?.userId ?: "")
                    loadPosts()
                }
            }, onCommentClick = { post -> showComments(post) })
            binding.rvPosts.adapter = adapter
        }
    }

    private fun showComments(post: Post) {
        lifecycleScope.launch {
            val comments = repo.getComments(post.id)
            val text = if (comments.isEmpty()) "Henüz yorum yok"
            else comments.joinToString("\n") { "${it.username}: ${it.content}" }
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Yorumlar")
                .setMessage(text)
                .setPositiveButton("Kapat", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        suggestedPlayer?.release()
        suggestedPlayer = null
        _binding = null
    }
}