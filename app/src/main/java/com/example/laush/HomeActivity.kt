package com.example.laush

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.laush.R
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.ActivityHomeBinding
import com.example.laush.databinding.DialogUploadVideoBinding
import com.example.laush.ui.fragments.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val repo = FirebaseRepo()
    private val storage = FirebaseStorage.getInstance()
    var userId: String = ""
    var username: String = ""
    var displayName: String = ""
    var userNumber: String = ""
    private var pendingVideoUri: android.net.Uri? = null

    override fun attachBaseContext(newBase: Context) {
        try {
            val prefs = newBase.getSharedPreferences("laush", Context.MODE_PRIVATE)
            val lang = prefs.getString("language", "tr") ?: "tr"
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } catch (e: Exception) {
            super.attachBaseContext(newBase)
        }
    }

    private val videoPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                pendingVideoUri = uri
                showFullScreenUploadDialog()
            }
        }
    }

    fun pickVideo() {
        val intents = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
        }
        videoPicker.launch(intents)
    }

    private fun showFullScreenUploadDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        val dialogBinding = DialogUploadVideoBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
        
        val categories = arrayOf("Müzik", "Komedi", "Oyun", "Spor", "Haber", "Eğitim", "Yaşam", "Diğer")
        dialogBinding.spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        
        dialogBinding.btnClose.setOnClickListener {
            pendingVideoUri = null
            dialog.dismiss()
        }
        
        dialogBinding.btnUpload.setOnClickListener {
            val caption = dialogBinding.etCaption.text.toString()
            val category = dialogBinding.spCategory.selectedItem.toString()
            pendingVideoUri?.let { uri ->
                uploadVideo(uri, caption, category)
            }
            dialog.dismiss()
        }
        
        dialogBinding.btnCancel.setOnClickListener {
            pendingVideoUri = null
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun uploadVideo(uri: android.net.Uri, caption: String, category: String) {
        Toast.makeText(this, "Video yükleniyor... (Bu biraz sürebilir)", Toast.LENGTH_LONG).show()
        
        Thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (bytes != null && bytes.size > 50_000_000) {
                    runOnUiThread {
                        Toast.makeText(this, "Video çok büyük! 50MB'den küçük olmalı", Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }
                
                if (bytes != null) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            val progressDialog = android.app.AlertDialog.Builder(this@HomeActivity)
                                .setMessage("Yükleniyor... %0")
                                .setCancelable(false)
                                .create()
                            progressDialog.show()
                            
                            val url = withContext(Dispatchers.IO) {
                                com.example.laush.data.CloudinaryUpload.upload(bytes, "video", "laush_preset")
                            }
                            
                            progressDialog.dismiss()
                            
                            if (url != null) {
                                repo.createPost(userId, username, caption, null, url, category)
                                Toast.makeText(this@HomeActivity, "Video yüklendi!", Toast.LENGTH_SHORT).show()
                                pendingVideoUri = null
                                loadFragment(DiscoverFragment())
                            } else {
                                Toast.makeText(this@HomeActivity, "Yükleme başarısız!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@HomeActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("laush", MODE_PRIVATE)
        userId = prefs.getString("user_id", "") ?: ""
        username = prefs.getString("username", "") ?: ""
        displayName = prefs.getString("display_name", "") ?: ""
        userNumber = prefs.getString("user_number", "") ?: ""

        if (userId.isEmpty()) {
            startActivity(Intent(this, com.example.laush.ui.LoginActivity::class.java))
            finish()
            return
        }

        setupBottomNavigation()
        loadFragment(HomeFragment())
        
        checkForUpdates()
    }

    private fun checkForUpdates() {
        thread {
            try {
                val url = URL("https://raw.githubusercontent.com/vedat58/laush/main/version.json")
                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val input = connection.getInputStream().bufferedReader().readText()
                val json = com.google.gson.JsonParser.parseString(input).asJsonObject
                val latestVersion = json.get("version").asString.toInt()
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionCode
                
                if (latestVersion > currentVersion) {
                    runOnUiThread {
                        android.app.AlertDialog.Builder(this)
                            .setTitle(getString(R.string.update_available))
                            .setMessage(getString(R.string.update_message))
                            .setPositiveButton(getString(R.string.update_now)) { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/vedat58/laush/releases"))
                                startActivity(intent)
                            }
                            .setNegativeButton(getString(R.string.later), null)
                            .show()
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_discover -> {
                    loadFragment(DiscoverFragment())
                    true
                }
                R.id.nav_add -> {
                    loadFragment(SearchFragment())
                    true
                }
                R.id.nav_messages -> {
                    loadFragment(MessagesFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun logout() {
        repo.logout()
        getSharedPreferences("laush", MODE_PRIVATE).edit().clear().apply()
        startActivity(Intent(this, com.example.laush.ui.LoginActivity::class.java))
        finish()
    }
}