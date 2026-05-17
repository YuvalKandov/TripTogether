package com.example.triptogether

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.triptogether.databinding.ActivityCreateTripBinding
import com.example.triptogether.model.Trip
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.DateUtils
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.ImageLoader
import com.example.triptogether.utilities.InviteCodeGenerator
import com.example.triptogether.utilities.SignalManager
import com.example.triptogether.utilities.StorageManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class CreateTripActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateTripBinding
    private var selectedImageUri: Uri? = null
    private var startDateTimestamp: Long = 0L
    private var endDateTimestamp: Long = 0L
    private var tripId: String = ""
    private var isEditMode: Boolean = false
    private var existingCoverImageUrl: String = ""
    private var existingInviteCode: String = ""
    private var existingOwnerId: String = ""
    private var existingCreatedAt: Long = 0L

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            binding.createtripIMGCover.setImageURI(uri)
            selectedImageUri = uri
            SignalManager.getInstance().toast("Image selected")
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTripBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        getBundleData()
        initViews()
        loadTrip()
    }

    private fun getBundleData() {
        val bundle: Bundle? = intent.extras
        tripId = bundle?.getString(Constants.BundleKeys.TRIP_ID, "") ?: ""
        isEditMode = tripId.isNotEmpty()
    }

    private fun initViews() {
        binding.createtripTOOLBARToolbar.title = if (isEditMode) "Edit Trip" else "Create Trip"
        binding.createtripTOOLBARToolbar.setNavigationOnClickListener {
            finish()
        }

        if (isEditMode) {
            binding.createtripBTNCreate.text = "Update Trip"
        }

        binding.createtripBTNSelectimage.setOnClickListener {
            openImagePicker()
        }

        binding.createtripETStartdate.setOnClickListener {
            showStartDatePicker()
        }

        binding.createtripETEnddate.setOnClickListener {
            showEndDatePicker()
        }

        binding.createtripBTNCreate.setOnClickListener {
            createTrip()
        }
    }

    private fun loadTrip() {
        if (!isEditMode) return

        FirebaseManager.getInstance().getTrip(
            tripId,
            { trip ->
                if (trip != null) {
                    binding.createtripETName.setText(trip.name)
                    binding.createtripETDescription.setText(trip.description)
                    startDateTimestamp = trip.startDate
                    endDateTimestamp = trip.endDate
                    existingCoverImageUrl = trip.coverImageUrl
                    existingInviteCode = trip.inviteCode
                    existingOwnerId = trip.ownerId
                    existingCreatedAt = trip.createdAt

                    binding.createtripETStartdate.setText(DateUtils.formatDate(trip.startDate))
                    binding.createtripETEnddate.setText(DateUtils.formatDate(trip.endDate))

                    if (trip.coverImageUrl.isNotEmpty()) {
                        ImageLoader.getInstance().loadImage(
                            trip.coverImageUrl,
                            binding.createtripIMGCover
                        )
                    }
                }
            },
            { error ->
                SignalManager.getInstance().toast("Failed to load trip")
                finish()
            }
        )
    }

    private fun openImagePicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showStartDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select start date")
            .setSelection(
                if (startDateTimestamp > 0) startDateTimestamp
                else MaterialDatePicker.todayInUtcMilliseconds()
            )
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            startDateTimestamp = selection
            binding.createtripETStartdate.setText(DateUtils.formatDate(selection))

            if (endDateTimestamp > 0 && endDateTimestamp < startDateTimestamp) {
                endDateTimestamp = 0L
                binding.createtripETEnddate.text?.clear()
                SignalManager.getInstance().toast("End date cleared - must be after start date")
            }
        }

        datePicker.show(supportFragmentManager, "START_DATE_PICKER")
    }

    private fun showEndDatePicker() {
        if (startDateTimestamp == 0L) {
            SignalManager.getInstance().toast("Please select start date first")
            return
        }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select end date")
            .setSelection(
                if (endDateTimestamp > 0) endDateTimestamp
                else startDateTimestamp
            )
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            if (selection < startDateTimestamp) {
                SignalManager.getInstance().toast("End date must be after start date")
                return@addOnPositiveButtonClickListener
            }
            endDateTimestamp = selection
            binding.createtripETEnddate.setText(DateUtils.formatDate(selection))
        }

        datePicker.show(supportFragmentManager, "END_DATE_PICKER")
    }

    private fun createTrip() {
        val name = binding.createtripETName.text.toString().trim()
        val description = binding.createtripETDescription.text.toString().trim()

        if (name.isEmpty()) {
            SignalManager.getInstance().toast("Please enter trip name")
            SignalManager.getInstance().vibrate()
            return
        }

        if (startDateTimestamp == 0L) {
            SignalManager.getInstance().toast("Please select start date")
            SignalManager.getInstance().vibrate()
            return
        }

        if (endDateTimestamp == 0L) {
            SignalManager.getInstance().toast("Please select end date")
            SignalManager.getInstance().vibrate()
            return
        }

        binding.createtripBTNCreate.isEnabled = false
        binding.createtripBTNCreate.text = if (isEditMode) "Updating..." else "Creating..."

        if (selectedImageUri != null) {
            uploadImageAndCreateTrip(name, description)
        } else {
            createTripWithoutImage(name, description, existingCoverImageUrl)
        }
    }

    private fun uploadImageAndCreateTrip(name: String, description: String) {
        val uri = selectedImageUri ?: return

        // Pre-generate tripId in create mode so StorageManager can use it for the path
        val preGeneratedTripId = if (isEditMode) {
            tripId
        } else {
            FirebaseDatabase.getInstance()
                .getReference(Constants.DB.TRIPS_REF)
                .push().key ?: return
        }

        StorageManager.getInstance().uploadTripCover(
            preGeneratedTripId,
            uri,
            { downloadUrl ->
                createTripWithoutImage(name, description, downloadUrl, preGeneratedTripId)
            },
            { error ->
                binding.createtripBTNCreate.isEnabled = true
                binding.createtripBTNCreate.text = if (isEditMode) "Update Trip" else "Create Trip"
                SignalManager.getInstance().toast("Failed to upload image")
                Log.w("CreateTrip", "Image upload failed", error)
            }
        )
    }

    private fun createTripWithoutImage(
        name: String,
        description: String,
        coverImageUrl: String,
        preGeneratedTripId: String = tripId
    ) {
        val inviteCode = if (isEditMode) existingInviteCode else InviteCodeGenerator.generateCode()

        val tripBuilder = Trip.Builder()
            .id(preGeneratedTripId)
            .name(name)
            .description(description)
            .coverImageUrl(coverImageUrl)
            .startDate(startDateTimestamp)
            .endDate(endDateTimestamp)
            .inviteCode(inviteCode)

        if (isEditMode) {
            tripBuilder
                .ownerId(existingOwnerId)
                .createdAt(existingCreatedAt)
        }

        val trip = tripBuilder.build()

        if (isEditMode) {
            FirebaseManager.getInstance().updateTrip(
                trip,
                onSuccess = {
                    SignalManager.getInstance().toast("Trip updated successfully!")
                    finish()
                },
                onFailure = { exception ->
                    binding.createtripBTNCreate.isEnabled = true
                    binding.createtripBTNCreate.text = "Update Trip"
                    SignalManager.getInstance().toast("Failed to update trip")
                    Log.w("CreateTrip", "Trip update failed", exception)
                }
            )
        } else {
            FirebaseManager.getInstance().createTrip(
                trip,
                onSuccess = { createdTripId ->
                    SignalManager.getInstance().toast("Trip created successfully!")
                    finish()
                },
                onFailure = { exception ->
                    binding.createtripBTNCreate.isEnabled = true
                    binding.createtripBTNCreate.text = "Create Trip"
                    SignalManager.getInstance().toast("Failed to create trip")
                    Log.w("CreateTrip", "Trip creation failed", exception)
                }
            )
        }
    }
}
