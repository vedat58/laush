package com.example.laush.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.laush.HomeActivity
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.FragmentSearchBinding
import com.example.laush.model.User
import com.example.laush.ui.UserAdapter
import com.example.laush.ui.UserProfileDialog
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val repo = FirebaseRepo()
    private lateinit var adapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val homeActivity = activity as? HomeActivity ?: return@onViewCreated
        
        adapter = UserAdapter(emptyList(), homeActivity.userId, { user ->
            UserProfileDialog(user, homeActivity.userId, homeActivity).show()
        }, { user ->
            toggleFollow(user)
        })
        
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
        
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                search(s.toString())
            }
        })
    }

    private fun search(query: String) {
        if (query.isEmpty()) return
        
        val homeActivity = activity as? HomeActivity ?: return
        lifecycleScope.launch {
            val followings = repo.getFollowings(homeActivity.userId)
            adapter.setFollowing(followings)
            
            val queryLower = query.lowercase()
            val byNumber = repo.getUserByNumber(query)
            val results = if (byNumber != null) {
                listOf(byNumber) + repo.searchUsers(queryLower)
            } else {
                repo.searchUsers(queryLower)
            }
            
            adapter = UserAdapter(results, homeActivity.userId, { user -> startChat(user) }, { user -> toggleFollow(user) })
            adapter.setFollowing(followings)
            binding.rvUsers.adapter = adapter
        }
    }

    private fun toggleFollow(user: User) {
        val homeActivity = activity as? HomeActivity ?: return
        lifecycleScope.launch {
            val isFollowing = repo.isFollowing(homeActivity.userId, user.id)
            if (isFollowing) {
                repo.unfollowUser(homeActivity.userId, user.id)
                Toast.makeText(requireContext(), "Takipten çıkıldı", Toast.LENGTH_SHORT).show()
            } else {
                repo.followUser(homeActivity.userId, user.id)
                Toast.makeText(requireContext(), "Takip edildi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startChat(user: User) {
        val homeActivity = activity as? HomeActivity ?: return
        lifecycleScope.launch {
            val canMessage = repo.canMessage(homeActivity.userId, user.id)
            if (canMessage || user.id == homeActivity.userId) {
                val chatRoomId = repo.getOrCreateChatRoom(homeActivity.userId, user.id)
                Toast.makeText(requireContext(), "DM açıldı: ${user.displayName}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Önce takip etmelisin!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}