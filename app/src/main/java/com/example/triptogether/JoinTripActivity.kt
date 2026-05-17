package com.example.triptogether

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.triptogether.databinding.ActivityJoinTripBinding
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.SignalManager

class JoinTripActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJoinTripBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinTripBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
    }

    private fun initViews() {
        binding.joinTOOLBARToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.joinBTNSubmit.setOnClickListener {
            val code = binding.joinETCode.text.toString().trim().uppercase()

            if (code.isEmpty()) {
                Toast.makeText(this, "Please enter an invite code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (code.length != Constants.InviteCode.CODE_LENGTH) {
                Toast.makeText(
                    this,
                    "Invite code must be ${Constants.InviteCode.CODE_LENGTH} characters",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            joinTrip(code)
        }
    }

    private fun joinTrip(inviteCode: String) {
        binding.joinPBLoading.isVisible = true
        binding.joinBTNSubmit.isEnabled = false

        FirebaseManager.getInstance().joinTripByCode(
            inviteCode = inviteCode,
            onSuccess = { tripId ->
                binding.joinPBLoading.isVisible = false
                binding.joinBTNSubmit.isEnabled = true

                SignalManager.getInstance().toast("Successfully joined trip!")
                SignalManager.getInstance().vibrate()

                // Navigate to trip detail
                val intent = Intent(this, TripDetailActivity::class.java)
                val bundle = Bundle()
                bundle.putString(Constants.BundleKeys.TRIP_ID, tripId)
                intent.putExtras(bundle)
                startActivity(intent)
                finish()
            },
            onFailure = { exception ->
                binding.joinPBLoading.isVisible = false
                binding.joinBTNSubmit.isEnabled = true

                Toast.makeText(
                    this,
                    "Failed to join trip: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}
