package com.example.laush.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.laush.HomeActivity
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.FragmentDiscoverBinding
import com.example.laush.model.Post
import com.example.laush.R
import androidx.media3.ui.PlayerView
import com.example.laush.ui.FriendSelectAdapter
import com.example.laush.ui.ReelsAdapter
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class DiscoverFragment : Fragment() {
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    private val repo = FirebaseRepo()
    private var posts: List<Post> = emptyList()
    private var adapter: ReelsAdapter? = null
    private val storage = FirebaseStorage.getInstance()

    private val videoPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadVideo(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        var lastPosition = 0
        
        binding.vpReels.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    val currentPos = binding.vpReels.currentItem
                    if (lastPosition != currentPos) {
                        stopVideoAtPosition(lastPosition)
                        lastPosition = currentPos
                    }
                }
            }
            
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }
        })
        
        loadReels()
    }
    
    private fun stopVideoAtPosition(position: Int) {
        try {
            val recyclerView = binding.vpReels.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView
            val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
            viewHolder?.itemView?.let { view ->
                val playerView = view.findViewById<androidx.media3.ui.PlayerView>(R.id.playerView)
                playerView?.player?.stop()
                playerView?.player?.release()
                playerView?.player = null
            }
        } catch (e: Exception) { }
    }

    override fun onResume() {
        super.onResume()
        loadReels()
    }

    private fun loadReels() {
        lifecycleScope.launch {
            posts = repo.getVideoPosts()
            adapter = ReelsAdapter(posts, { post ->
                lifecycleScope.launch {
                    repo.likePost(post.id, (activity as? HomeActivity)?.userId ?: "")
                }
            }, { post ->
                showShareDialog(post)
            }, { post, comment ->
                lifecycleScope.launch {
                    repo.addComment(post.id, (activity as? HomeActivity)?.userId ?: "", (activity as? HomeActivity)?.username ?: "", comment)
                    Toast.makeText(context, "Yorum eklendi", Toast.LENGTH_SHORT).show()
                }
            })
            binding.vpReels.adapter = adapter
        }
    }

    private fun showShareDialog(post: Post) {
        val homeActivity = activity as? HomeActivity ?: return
        lifecycleScope.launch {
            val followings = repo.getFollowings(homeActivity.userId)
            if (followings.isEmpty()) {
                Toast.makeText(requireContext(), "Arkadaşın yok!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val friends = followings.mapNotNull { repo.getUser(it) }
            if (friends.isEmpty()) {
                Toast.makeText(requireContext(), "Arkadaş bulunamadı!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val dialog = android.app.Dialog(requireContext())
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_share_video)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            val rvFriends = dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFriends)
            val btnSend = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSend)
            val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
            
            val adapter = FriendSelectAdapter(friends) { selectedIds ->
                // Selection changed
            }
            rvFriends.layoutManager = LinearLayoutManager(requireContext())
            rvFriends.adapter = adapter
            
            btnSend.setOnClickListener {
                val selectedIds = (0 until rvFriends.adapter?.itemCount!!).mapNotNull { pos ->
                    val viewHolder = rvFriends.findViewHolderForAdapterPosition(pos) as? com.example.laush.ui.FriendSelectAdapter.ViewHolder
                    if (viewHolder?.binding?.cbSelect?.isChecked == true) friends[pos].id else null
                }
                
                selectedIds.forEach { friendId ->
                    lifecycleScope.launch {
                        repo.sendMessage("", homeActivity.userId, "Video: ${post.content}\n${post.videoUrl}")
                    }
                }
                Toast.makeText(requireContext(), "Gönderildi!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }

    private fun uploadVideo(uri: Uri) {
        Toast.makeText(requireContext(), "Yükleniyor...", Toast.LENGTH_SHORT).show()
        
        val userId = (activity as? HomeActivity)?.userId ?: return
        val username = (activity as? HomeActivity)?.username ?: return
        
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            
            if (bytes != null) {
                lifecycleScope.launch {
                    try {
                        val url = com.example.laush.data.CloudinaryUpload.upload(bytes, "video", "laush_preset")
                        if (url != null) {
                            repo.createPost(userId, username, "Reels", videoUrl = url)
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "Yüklendi!", Toast.LENGTH_SHORT).show()
                                loadReels()
                            }
                        }
                    } catch (e: Exception) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}