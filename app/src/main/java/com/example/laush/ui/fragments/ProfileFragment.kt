package com.example.laush.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.app.Dialog
import android.net.Uri
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.laush.HomeActivity
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.FragmentProfileBinding
import com.example.laush.model.User
import com.example.laush.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val repo = FirebaseRepo()
    private var currentUser: User? = null
    private val storage = FirebaseStorage.getInstance()
    private var selectedVideoUri: android.net.Uri? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadPhoto(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnChangePhoto.setOnClickListener {
            val intents = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            photoPicker.launch(intents)
        }
        
        binding.btnSettings.setOnClickListener {
            showSettings()
        }
        
        binding.btnAddVideo.setOnClickListener {
            (activity as? HomeActivity)?.pickVideo()
        }
        
        loadProfile()
    }

    private val videoPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedVideoUri = uri
                showUploadDialog()
            }
        }
    }

    private fun showUploadDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_upload_video)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etCaption = dialog.findViewById<android.widget.EditText>(R.id.etCaption)
        val spCategory = dialog.findViewById<Spinner>(R.id.spCategory)
        val btnUpload = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpload)
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        
        val categories = arrayOf("Müzik", "Komedi", "Oyun", "Spor", "Haber", "Eğitim", "Yaşam", "Diğer")
        spCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        
        btnUpload.setOnClickListener {
            val caption = etCaption.text.toString()
            val category = spCategory.selectedItem.toString()
            selectedVideoUri?.let { uri ->
                uploadVideo(uri, caption, category)
            }
            dialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun uploadVideo(uri: Uri, caption: String, category: String) {
        Toast.makeText(requireContext(), "Video yükleniyor...", Toast.LENGTH_SHORT).show()
        val homeActivity = activity as? HomeActivity ?: return
        
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            
            if (bytes != null) {
                lifecycleScope.launch {
                    try {
                        val url = com.example.laush.data.CloudinaryUpload.upload(bytes, "video", "laush_preset")
                        if (url != null) {
                            repo.createPost(homeActivity.userId, homeActivity.username, caption, videoUrl = url, category = category)
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "Video yüklendi!", Toast.LENGTH_SHORT).show()
                                selectedVideoUri = null
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

    private fun loadProfile() {
        val homeActivity = activity as? HomeActivity ?: return
        lifecycleScope.launch {
            currentUser = repo.getUser(homeActivity.userId)
            currentUser?.let { user ->
                binding.tvDisplayName.text = user.displayName
                binding.tvUsername.text = "@${user.username}"
                binding.tvUserNumber.text = "#${user.userNumber}"
                binding.tvBio.text = user.bio.ifEmpty { "Bio ekle" }
                binding.tvPosts.text = user.posts.toString()
                binding.tvFollowers.text = user.followers.toString()
                binding.tvFollowing.text = user.following.toString()
                
user.photoUrl?.let { url ->
                    Glide.with(this@ProfileFragment).load(url).into(binding.ivProfile)
                }
            }
        }
    }

    private fun uploadPhoto(uri: Uri) {
        Toast.makeText(requireContext(), "Yükleniyor...", Toast.LENGTH_SHORT).show()
        
        val homeActivity = activity as? HomeActivity ?: return
        
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            
            if (bytes != null && bytes.size > 10_000_000) {
                Toast.makeText(requireContext(), "Fotoğraf çok büyük! 10MB'den küçük olmalı", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (bytes != null) {
                lifecycleScope.launch {
                    try {
                        val url = com.example.laush.data.CloudinaryUpload.upload(bytes, "image", "laush_preset")
                        if (url != null) {
                            repo.updateProfile(homeActivity.userId, currentUser?.displayName ?: "", currentUser?.bio ?: "", url)
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "Fotoğraf değişti!", Toast.LENGTH_SHORT).show()
                                loadProfile()
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

    private fun showSettings() {
        val options = arrayOf(
            getString(R.string.edit_profile),
            getString(R.string.select_language),
            "Videolarımı Sil",
            getString(R.string.logout)
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditProfile()
                    1 -> showLanguageDialog()
                    2 -> showDeleteVideosDialog()
                    3 -> (activity as? HomeActivity)?.logout()
                }
            }
            .show()
    }
    
    private fun showDeleteVideosDialog() {
        val homeActivity = activity as? HomeActivity ?: return
        lifecycleScope.launch {
            val allPosts = repo.getVideoPosts()
            val myVideos = allPosts.filter { it.userId == homeActivity.userId }
            
            if (myVideos.isEmpty()) {
                Toast.makeText(requireContext(), "Silinecek video yok", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val videoTitles = myVideos.map { it.content.ifEmpty { "Video" } }.toTypedArray()
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Videolarını Sil")
                .setItems(videoTitles) { _, which ->
                    val video = myVideos[which]
                    lifecycleScope.launch {
                        val success = repo.deletePost(video.id)
                        if (success) {
                            Toast.makeText(requireContext(), "Video silindi", Toast.LENGTH_SHORT).show()
                            loadProfile()
                        } else {
                            Toast.makeText(requireContext(), "Silme başarısız", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("İptal", null)
                .show()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Türkçe", "English")
        val langCodes = arrayOf("tr", "en")
        val prefs = requireContext().getSharedPreferences("laush", Context.MODE_PRIVATE)
        val currentLang = prefs.getString("language", "tr") ?: "tr"
        val currentIndex = langCodes.indexOf(currentLang)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_language))
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val newLang = langCodes[which]
                prefs.edit().putString("language", newLang).apply()
                dialog.dismiss()
                activity?.recreate()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showEditProfile() {
        val editName = android.widget.EditText(requireContext()).apply {
            setText(currentUser?.displayName)
            hint = "İsim"
        }
        val editBio = android.widget.EditText(requireContext()).apply {
            setText(currentUser?.bio)
            hint = "Bio"
        }
        
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(editName)
            addView(editBio)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Profil Düzenle")
            .setView(layout)
            .setPositiveButton("Kaydet") { _, _ ->
                val homeActivity = activity as? HomeActivity ?: return@setPositiveButton
                lifecycleScope.launch {
                    repo.updateProfile(homeActivity.userId, editName.text.toString(), editBio.text.toString(), null)
                    loadProfile()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}