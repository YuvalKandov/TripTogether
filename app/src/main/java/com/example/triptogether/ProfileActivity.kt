package com.example.triptogether

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.triptogether.databinding.ActivityProfileBinding
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.ImageLoader
import com.example.triptogether.utilities.SignalManager
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        loadUserData()
    }

    private fun initViews() {
        binding.profileTOOLBARToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.profileBTNLogout.setOnClickListener {
            signOut()
        }

        initDarkModeSwitch()
    }

    private fun initDarkModeSwitch() {
        val prefs = getSharedPreferences(Constants.Prefs.PREFS_NAME, MODE_PRIVATE)
        val isDarkMode: Boolean = prefs.getBoolean(Constants.Prefs.KEY_DARK_MODE, false)

        binding.profileSWITCHDarkMode.isChecked = isDarkMode

        binding.profileSWITCHDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean(Constants.Prefs.KEY_DARK_MODE, isChecked)
                .apply()

            val mode: Int = if (isChecked)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO

            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun loadUserData() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            binding.profileLBLName.text = it.displayName ?: "User"
            binding.profileLBLEmail.text = it.email ?: ""

            it.photoUrl?.let { photoUrl ->
                ImageLoader.getInstance().loadImage(
                    photoUrl.toString(),
                    binding.profileIMGPhoto
                )
            }
        }
    }

    private fun signOut() {
        binding.profileBTNLogout.isEnabled = false

        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener { task ->
                if (isFinishing || isDestroyed) return@addOnCompleteListener

                if (task.isSuccessful) {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    binding.profileBTNLogout.isEnabled = true
                    SignalManager.getInstance().toast("Failed to sign out")
                }
            }
    }
}
