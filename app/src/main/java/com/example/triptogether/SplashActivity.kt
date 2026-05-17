package com.example.triptogether

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.triptogether.databinding.ActivitySplashBinding
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.FirebaseManager
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initTextFadeIn()
        startAnimation()
    }

    private fun initTextFadeIn() {
        binding.splashLBLTitle.alpha = 0f
        binding.splashLBLTagline.alpha = 0f

        Handler(Looper.getMainLooper()).postDelayed({
            binding.splashLBLTitle.animate()
                .alpha(1f)
                .setDuration(800L)
                .start()

            binding.splashLBLTagline.animate()
                .alpha(0.7f)
                .setDuration(800L)
                .start()
        }, 400L)
    }

    private fun startAnimation() {
        binding.splashLOTTIEAnimation.resumeAnimation()
        binding.splashLOTTIEAnimation.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
                // Animation started
            }

            override fun onAnimationEnd(p0: Animator) {
                checkAuthAndNavigate()
            }

            override fun onAnimationCancel(p0: Animator) {
                // Animation cancelled
            }

            override fun onAnimationRepeat(p0: Animator) {
                // Animation repeated
            }
        })
    }

    private fun checkAuthAndNavigate() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            transactToAnotherActivity(Constants.Activities.LOGIN)
        } else {
            FirebaseManager.getInstance().saveCurrentUser()
            transactToAnotherActivity(Constants.Activities.MAIN)
        }
    }

    private fun transactToAnotherActivity(className: String) {
        val targetIntent = when(className) {
            Constants.Activities.LOGIN -> Intent(this, LoginActivity::class.java)
            Constants.Activities.MAIN -> Intent(this, MainActivity::class.java)
            else -> null
        }
        targetIntent?.let {
            startActivity(it)
            finish()
        }
    }
}
