package com.example.laush.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.laush.HomeActivity
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.FragmentNotesBinding
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {
    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private val repo = FirebaseRepo()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadNotes()
        
        binding.btnSave.setOnClickListener {
            saveNotes()
        }
    }

    private fun loadNotes() {
        val homeActivity = activity as? HomeActivity ?: return
        lifecycleScope.launch {
            val user = repo.getUser(homeActivity.userId)
            binding.etNotes.setText(user?.bio ?: "")
        }
    }

    private fun saveNotes() {
        val homeActivity = activity as? HomeActivity ?: return
        val notes = binding.etNotes.text.toString()
        
        lifecycleScope.launch {
            try {
                repo.updateProfile(homeActivity.userId, homeActivity.displayName, notes, homeActivity.userId)
                Toast.makeText(requireContext(), "Notlar kaydedildi!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}