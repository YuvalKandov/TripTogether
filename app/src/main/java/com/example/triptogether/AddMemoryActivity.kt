package com.example.triptogether

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.triptogether.databinding.ActivityAddMemoryBinding
import com.example.triptogether.model.Memory
import com.example.triptogether.model.TripActivity
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.SignalManager
import com.example.triptogether.utilities.StorageManager
import com.google.firebase.auth.FirebaseAuth

class AddMemoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMemoryBinding
    private var tripId: String = ""
    private var memoryId: String = ""
    private var activityId: String = ""
    private var isEditMode: Boolean = false
    private var existingMemory: Memory? = null
    private var activities: List<TripActivity> = emptyList()
    private var selectedActivity: TripActivity? = null
    private val selectedPhotoUris = mutableListOf<Uri>()
    private val existingPhotoUrls = mutableListOf<String>()
    private val maxPhotos = 3

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxPhotos)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedPhotoUris.clear()
            selectedPhotoUris.addAll(uris.take(maxPhotos))
            displaySelectedPhotos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMemoryBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        getBundleData()
        initViews()
        loadActivities()
    }

    private fun getBundleData() {
        val bundle: Bundle? = intent.extras
        tripId = bundle?.getString(Constants.BundleKeys.TRIP_ID, "") ?: ""
        memoryId = bundle?.getString(Constants.BundleKeys.MEMORY_ID, "") ?: ""
        activityId = bundle?.getString(Constants.BundleKeys.ACTIVITY_ID, "") ?: ""
        isEditMode = memoryId.isNotEmpty()
    }

    private fun initViews() {
        binding.addmemoryTOOLBARToolbar.title = if (isEditMode) "Edit Memory" else "Add Memory"
        binding.addmemoryTOOLBARToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.addmemoryBTNAddPhoto.setOnClickListener {
            openPhotoPicker()
        }

        binding.addmemoryBTNSave.setOnClickListener {
            saveMemory()
        }
    }

    private fun loadActivities() {
        FirebaseManager.getInstance().getActivitiesForTripOnce(
            tripId,
            { loadedActivities ->
                activities = loadedActivities
                if (activities.isEmpty()) {
                    SignalManager.getInstance().toast("No activities found. Please add activities first.")
                    finish()
                } else {
                    setupActivitySpinner()

                    // Load existing memory if in edit mode
                    if (isEditMode) {
                        loadExistingMemory()
                    }
                }
            },
            { error ->
                SignalManager.getInstance().toast("Failed to load activities")
                finish()
            }
        )
    }

    private fun loadExistingMemory() {
        FirebaseManager.getInstance().getMemory(
            activityId,
            memoryId,
            { memory ->
                if (memory != null) {
                    existingMemory = memory
                    binding.addmemoryETText.setText(memory.text)

                    // Select the correct activity in spinner
                    val activityIndex = activities.indexOfFirst { it.id == memory.activityId }
                    if (activityIndex >= 0) {
                        binding.addmemorySPINNERActivity.setSelection(activityIndex)
                    }

                    // Load existing photos
                    existingPhotoUrls.clear()
                    existingPhotoUrls.addAll(memory.photoUrls)
                    displayExistingPhotos()
                }
            },
            { error ->
                SignalManager.getInstance().toast("Failed to load memory")
            }
        )
    }

    private fun displayExistingPhotos() {
        binding.addmemoryLAYOUTPhotos.removeAllViews()

        existingPhotoUrls.forEach { photoUrl ->
            val imageView = AppCompatImageView(this)
            val sizePx = (120 * resources.displayMetrics.density).toInt()
            val params = android.widget.LinearLayout.LayoutParams(sizePx, sizePx)
            params.marginEnd = (8 * resources.displayMetrics.density).toInt()
            imageView.layoutParams = params
            imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP

            com.example.triptogether.utilities.ImageLoader.getInstance().loadImage(
                photoUrl,
                imageView
            )

            val cardView = androidx.cardview.widget.CardView(this)
            val cardParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardParams.marginEnd = (8 * resources.displayMetrics.density).toInt()
            cardView.layoutParams = cardParams
            cardView.radius = (8 * resources.displayMetrics.density)
            cardView.addView(imageView)

            binding.addmemoryLAYOUTPhotos.addView(cardView)
        }
    }

    private fun setupActivitySpinner() {
        val activityNames = activities.map { "${it.title} - ${it.startTime}" }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            activityNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.addmemorySPINNERActivity.adapter = adapter

        binding.addmemorySPINNERActivity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedActivity = activities[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedActivity = null
            }
        }
    }

    private fun openPhotoPicker() {
        if (selectedPhotoUris.size >= maxPhotos) {
            SignalManager.getInstance().toast("Maximum $maxPhotos photos allowed")
            return
        }
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun displaySelectedPhotos() {
        binding.addmemoryLAYOUTPhotos.removeAllViews()

        selectedPhotoUris.forEach { uri ->
            val imageView = AppCompatImageView(this)
            val sizePx = (120 * resources.displayMetrics.density).toInt()
            val params = android.widget.LinearLayout.LayoutParams(sizePx, sizePx)
            params.marginEnd = (8 * resources.displayMetrics.density).toInt()
            imageView.layoutParams = params
            imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            imageView.setImageURI(uri)

            val cardView = androidx.cardview.widget.CardView(this)
            val cardParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardParams.marginEnd = (8 * resources.displayMetrics.density).toInt()
            cardView.layoutParams = cardParams
            cardView.radius = (8 * resources.displayMetrics.density)
            cardView.addView(imageView)

            binding.addmemoryLAYOUTPhotos.addView(cardView)
        }
    }

    private fun saveMemory() {
        val text = binding.addmemoryETText.text.toString().trim()

        if (selectedActivity == null) {
            SignalManager.getInstance().toast("Please select an activity")
            return
        }

        // Validation: need either text or photos (new or existing)
        if (text.isEmpty() && selectedPhotoUris.isEmpty() && existingPhotoUrls.isEmpty()) {
            SignalManager.getInstance().toast("Please add text or photos")
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            SignalManager.getInstance().toast("User not authenticated")
            return
        }

        binding.addmemoryBTNSave.isEnabled = false
        binding.addmemoryBTNSave.text = "Saving..."

        // Determine which photos to use
        if (selectedPhotoUris.isEmpty()) {
            // No new photos selected
            if (isEditMode) {
                // Keep existing photos in edit mode
                saveMemoryWithPhotos(text, existingPhotoUrls, currentUser.uid)
            } else {
                // No photos in create mode
                saveMemoryWithPhotos(text, emptyList(), currentUser.uid)
            }
        } else {
            // New photos selected - upload them
            uploadPhotos(text, currentUser.uid)
        }
    }

    private fun uploadPhotos(text: String, userId: String) {
        val uploadedUrls = mutableListOf<String>()
        var photosUploaded = 0

        selectedPhotoUris.forEach { uri ->
            StorageManager.getInstance().uploadMemoryPhoto(
                tripId,
                selectedActivity?.id ?: "",
                uri,
                { downloadUrl ->
                    uploadedUrls.add(downloadUrl)
                    photosUploaded++

                    if (photosUploaded == selectedPhotoUris.size) {
                        saveMemoryWithPhotos(text, uploadedUrls, userId)
                    }
                },
                { error ->
                    SignalManager.getInstance().toast("Failed to upload photo")
                    binding.addmemoryBTNSave.isEnabled = true
                    binding.addmemoryBTNSave.text = "Save Memory"
                }
            )
        }
    }

    private fun saveMemoryWithPhotos(text: String, photoUrls: List<String>, userId: String) {
        if (isEditMode) {
            // Update existing memory
            val memory = Memory.Builder()
                .id(memoryId)
                .activityId(selectedActivity?.id ?: "")
                .tripId(tripId)
                .text(text)
                .photoUrls(photoUrls)
                .userId(existingMemory?.userId ?: userId) // Keep original user
                .createdAt(existingMemory?.createdAt ?: System.currentTimeMillis()) // Keep original timestamp
                .build()

            FirebaseManager.getInstance().updateMemory(
                memory,
                {
                    SignalManager.getInstance().toast("Memory updated")
                    SignalManager.getInstance().vibrate()
                    finish()
                },
                { error ->
                    SignalManager.getInstance().toast("Failed to update memory")
                    binding.addmemoryBTNSave.isEnabled = true
                    binding.addmemoryBTNSave.text = "Save Memory"
                }
            )
        } else {
            // Create new memory
            val memory = Memory.Builder()
                .activityId(selectedActivity?.id ?: "")
                .tripId(tripId)
                .text(text)
                .photoUrls(photoUrls)
                .userId(userId)
                .createdAt(System.currentTimeMillis())
                .build()

            FirebaseManager.getInstance().createMemory(
                memory,
                { memoryId ->
                    SignalManager.getInstance().toast("Memory saved")
                    SignalManager.getInstance().vibrate()
                    finish()
                },
                { error ->
                    SignalManager.getInstance().toast("Failed to save memory")
                    binding.addmemoryBTNSave.isEnabled = true
                    binding.addmemoryBTNSave.text = "Save Memory"
                }
            )
        }
    }
}
