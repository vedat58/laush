package com.example.laush.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.laush.HomeActivity
import com.example.laush.R
import com.example.laush.data.FirebaseRepo
import com.example.laush.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import java.util.Locale

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val repo = FirebaseRepo()
    private var savedAccounts = mutableListOf<SavedAccount>()
    private var selectedLang = "tr"

    data class SavedAccount(
        val email: String,
        val password: String,
        val userId: String,
        val username: String,
        val displayName: String,
        val userNumber: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val prefs = getSharedPreferences("laush", MODE_PRIVATE)
            val lang = prefs.getString("language", "tr") ?: "tr"
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            createConfigurationContext(config)
        } catch (e: Exception) { }
        
        // Auto-login check
        val prefs = getSharedPreferences("laush", MODE_PRIVATE)
        val savedUserId = prefs.getString("user_id", "") ?: ""
        val savedPassword = prefs.getString("password", "") ?: ""
        
        if (savedUserId.isNotEmpty() && savedPassword.isNotEmpty()) {
            // Auto-login
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadSavedAccounts()
        setupLanguageSpinner()
        
        binding.btnLogin.setOnClickListener { login() }
        binding.btnRegister.setOnClickListener { showRegisterDialog() }
        
        binding.btnAccount1.setOnClickListener { switchAccount(0) }
        binding.btnAccount2.setOnClickListener { switchAccount(1) }
        binding.btnAccount3.setOnClickListener { switchAccount(2) }
        
        updateAccountButtons()
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf("Türkçe", "English")
        val langCodes = arrayOf("tr", "en")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spLanguage.adapter = adapter
        binding.spLanguage.setSelection(langCodes.indexOf(selectedLang))
        
        binding.spLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newLang = langCodes[position]
                if (newLang != selectedLang) {
                    selectedLang = newLang
                    getSharedPreferences("laush", MODE_PRIVATE).edit().putString("language", newLang).apply()
                    recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadSavedAccounts() {
        val prefs = getSharedPreferences("laush", MODE_PRIVATE)
        val accountsJson = prefs.getString("accounts", "") ?: ""
        if (accountsJson.isNotEmpty()) {
            try {
                val parts = accountsJson.split("|||")
                savedAccounts.clear()
                for (part in parts) {
                    if (part.isNotEmpty()) {
                        val p = part.split(":::")
                        if (p.size >= 6) {
                            savedAccounts.add(SavedAccount(p[0], p[1], p[2], p[3], p[4], p[5]))
                        }
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun saveAccounts() {
        val prefs = getSharedPreferences("laush", MODE_PRIVATE)
        val accountsStr = savedAccounts.joinToString("|||") { acc ->
            "${acc.email}:::${acc.password}:::${acc.userId}:::${acc.username}:::${acc.displayName}:::${acc.userNumber}"
        }
        prefs.edit().putString("accounts", accountsStr).apply()
    }

    private fun updateAccountButtons() {
        val buttons = listOf(binding.btnAccount1, binding.btnAccount2, binding.btnAccount3)
        buttons.forEachIndexed { index, btn ->
            btn.visibility = if (index < savedAccounts.size) View.VISIBLE else View.GONE
            if (index < savedAccounts.size) {
                btn.text = savedAccounts[index].displayName.take(1).uppercase()
            }
        }
    }

    private fun login() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_fill_all), Toast.LENGTH_SHORT).show()
            return
        }
        
        val email = "$username@laush.com"
        
        lifecycleScope.launch {
            val result = repo.login(username, password)
            result.onSuccess { user ->
                saveAndGoHome(user.id, user.username, user.displayName, user.userNumber, email, password)
            }.onFailure {
                Toast.makeText(this@LoginActivity, getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRegisterDialog() {
        if (savedAccounts.size >= 3) {
            Toast.makeText(this, getString(R.string.error_max_accounts), Toast.LENGTH_LONG).show()
            return
        }
        
        val etUsername = android.widget.EditText(this)
        etUsername.hint = getString(R.string.username)
        
        val etPassword = android.widget.EditText(this)
        etPassword.hint = getString(R.string.password)
        etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
            addView(etUsername)
            addView(etPassword)
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.new_account))
            .setView(layout)
            .setPositiveButton(getString(R.string.register_button)) { _, _ ->
                val displayName = etUsername.text.toString().trim()
                val password = etPassword.text.toString()
                
                if (displayName.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, getString(R.string.error_fill_all), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                register(displayName, password)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun register(displayName: String, password: String) {
        if (savedAccounts.size >= 3) {
            Toast.makeText(this, getString(R.string.error_max_accounts), Toast.LENGTH_SHORT).show()
            return
        }
        
        val username = displayName.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
        val email = "$username@laush.com"
        
        lifecycleScope.launch {
            val result = repo.register(username, password, displayName)
            result.onSuccess { user ->
                savedAccounts.add(SavedAccount(email, password, user.id, user.username, user.displayName, user.userNumber))
                saveAccounts()
                saveAndGoHome(user.id, user.username, user.displayName, user.userNumber, email, password)
            }.onFailure { e ->
                Toast.makeText(this@LoginActivity, "${getString(R.string.error_occurred)}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun switchAccount(index: Int) {
        if (index >= savedAccounts.size) return
        val acc = savedAccounts[index]
        val email = acc.email
        val password = acc.password
        
        lifecycleScope.launch {
            val username = acc.username
            val result = repo.login(username, password)
            result.onSuccess { user ->
                saveAndGoHome(user.id, user.username, user.displayName, user.userNumber, email, password)
            }.onFailure {
                savedAccounts.removeAt(index)
                saveAccounts()
                updateAccountButtons()
                Toast.makeText(this@LoginActivity, getString(R.string.account_removed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAndGoHome(userId: String, username: String, displayName: String, userNumber: String, email: String, password: String) {
        val prefs = getSharedPreferences("laush", MODE_PRIVATE)
        prefs.edit()
            .putString("user_id", userId)
            .putString("username", username)
            .putString("display_name", displayName)
            .putString("user_number", userNumber)
            .putString("email", email)
            .putString("password", password)
            .apply()
        
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}