package com.example.triptogether

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.triptogether.databinding.ActivityAddEditActivityBinding
import com.example.triptogether.model.TripActivity
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.DateUtils
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.SignalManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar

class AddEditActivityActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditActivityBinding
    private var tripId: String = ""
    private var activityId: String = ""
    private var selectedDate: Long = 0L
    private var selectedStartTime: String = ""
    private var selectedEndTime: String = ""
    private var isEditMode: Boolean = false
    private var tripStartDate: Long = 0L
    private var tripEndDate: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditActivityBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        getBundleData()
        initViews()
        loadActivity()
    }

    private fun getBundleData() {
        val bundle: Bundle? = intent.extras
        tripId = bundle?.getString(Constants.BundleKeys.TRIP_ID, "") ?: ""
        activityId = bundle?.getString(Constants.BundleKeys.ACTIVITY_ID, "") ?: ""
        selectedDate = bundle?.getLong(Constants.BundleKeys.SELECTED_DATE, 0L) ?: 0L
        tripStartDate = bundle?.getLong(Constants.BundleKeys.START_DATE, 0L) ?: 0L
        tripEndDate = bundle?.getLong(Constants.BundleKeys.END_DATE, 0L) ?: 0L

        isEditMode = activityId.isNotEmpty()
    }

    private fun initViews() {
        binding.addeditactivityTOOLBARToolbar.title = if (isEditMode) "Edit Activity" else "Add Activity"
        binding.addeditactivityTOOLBARToolbar.setNavigationOnClickListener {
            finish()
        }

        if (selectedDate != 0L) {
            binding.addeditactivityETDate.setText(DateUtils.formatDate(selectedDate))
        }

        if (isEditMode) {
            binding.addeditactivityBTNDelete.visibility = android.view.View.VISIBLE
        }

        binding.addeditactivityETDate.setOnClickListener {
            showDatePicker()
        }

        binding.addeditactivityETStarttime.setOnClickListener {
            showTimePicker(true)
        }

        binding.addeditactivityETEndtime.setOnClickListener {
            showTimePicker(false)
        }

        binding.addeditactivityBTNSave.setOnClickListener {
            saveActivity()
        }

        binding.addeditactivityBTNDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadActivity() {
        if (!isEditMode) return

        FirebaseManager.getInstance().getActivity(
            tripId,
            activityId,
            { activity ->
                if (activity != null) {
                    binding.addeditactivityETTitle.setText(activity.title)
                    binding.addeditactivityETDescription.setText(activity.description)
                    binding.addeditactivityETDate.setText(DateUtils.formatDate(activity.date))
                    binding.addeditactivityETStarttime.setText(activity.startTime)
                    binding.addeditactivityETEndtime.setText(activity.endTime)
                    binding.addeditactivityETLocation.setText(activity.location)

                    selectedDate = activity.date
                    selectedStartTime = activity.startTime
                    selectedEndTime = activity.endTime

                    // Set selected tag
                    when (activity.tag) {
                        TripActivity.Tags.FOOD -> binding.addeditactivityCHIPFood.isChecked = true
                        TripActivity.Tags.TRANSPORT -> binding.addeditactivityCHIPTransport.isChecked = true
                        TripActivity.Tags.HOTEL -> binding.addeditactivityCHIPHotel.isChecked = true
                        TripActivity.Tags.ACTIVITY -> binding.addeditactivityCHIPActivity.isChecked = true
                        else -> binding.addeditactivityCHIPOther.isChecked = true
                    }
                }
            },
            { error ->
                SignalManager.getInstance().toast("Failed to load activity")
            }
        )
    }

    private fun showDatePicker() {
        // Build calendar constraints to restrict date range to trip dates
        val constraintsBuilder = CalendarConstraints.Builder()

        if (tripStartDate != 0L && tripEndDate != 0L) {
            // Set the valid date range
            constraintsBuilder.setStart(tripStartDate)
            constraintsBuilder.setEnd(tripEndDate)

            // Create validators for min and max dates
            val validators = ArrayList<CalendarConstraints.DateValidator>()
            validators.add(DateValidatorPointForward.from(tripStartDate))
            validators.add(DateValidatorPointBackward.before(tripEndDate + 24 * 60 * 60 * 1000)) // Add one day to include end date

            val compositeValidator = CompositeDateValidator.allOf(validators)
            constraintsBuilder.setValidator(compositeValidator)
        }

        // Determine initial selection
        val initialSelection = when {
            selectedDate != 0L && isDateInRange(selectedDate) -> selectedDate
            tripStartDate != 0L -> tripStartDate
            else -> MaterialDatePicker.todayInUtcMilliseconds()
        }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(initialSelection)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate = selection
            binding.addeditactivityETDate.setText(DateUtils.formatDate(selection))
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun isDateInRange(date: Long): Boolean {
        if (tripStartDate == 0L || tripEndDate == 0L) return true
        return date >= tripStartDate && date <= tripEndDate + 24 * 60 * 60 * 1000
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText(if (isStartTime) "Select Start Time" else "Select End Time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            val timeString = String.format("%02d:%02d", hour, minute)

            if (isStartTime) {
                selectedStartTime = timeString
                binding.addeditactivityETStarttime.setText(timeString)
            } else {
                selectedEndTime = timeString
                binding.addeditactivityETEndtime.setText(timeString)
            }
        }

        timePicker.show(supportFragmentManager, "TIME_PICKER")
    }

    private fun saveActivity() {
        val title = binding.addeditactivityETTitle.text.toString().trim()
        val description = binding.addeditactivityETDescription.text.toString().trim()
        val location = binding.addeditactivityETLocation.text.toString().trim()

        if (title.isEmpty()) {
            SignalManager.getInstance().toast("Please enter a title")
            return
        }

        if (selectedDate == 0L) {
            SignalManager.getInstance().toast("Please select a date")
            return
        }

        // Validate date is within trip range
        if (!isDateInRange(selectedDate)) {
            val startDateStr = DateUtils.formatDate(tripStartDate)
            val endDateStr = DateUtils.formatDate(tripEndDate)
            SignalManager.getInstance().toast("Date must be between $startDateStr and $endDateStr")
            return
        }

        if (selectedStartTime.isEmpty()) {
            SignalManager.getInstance().toast("Please select a start time")
            return
        }

        val selectedTag = when (binding.addeditactivityCHIPGROUPTags.checkedChipId) {
            R.id.addeditactivity_CHIP_food -> TripActivity.Tags.FOOD
            R.id.addeditactivity_CHIP_transport -> TripActivity.Tags.TRANSPORT
            R.id.addeditactivity_CHIP_hotel -> TripActivity.Tags.HOTEL
            R.id.addeditactivity_CHIP_activity -> TripActivity.Tags.ACTIVITY
            else -> TripActivity.Tags.OTHER
        }

        val activity = TripActivity.Builder()
            .id(activityId)
            .tripId(tripId)
            .title(title)
            .description(description)
            .date(selectedDate)
            .startTime(selectedStartTime)
            .endTime(selectedEndTime)
            .location(location)
            .tag(selectedTag)
            .order(0)
            .build()

        if (isEditMode) {
            FirebaseManager.getInstance().updateActivity(
                activity,
                {
                    SignalManager.getInstance().toast("Activity updated")
                    SignalManager.getInstance().vibrate()
                    finish()
                },
                { error ->
                    SignalManager.getInstance().toast("Failed to update activity")
                }
            )
        } else {
            FirebaseManager.getInstance().createActivity(
                activity,
                { id ->
                    SignalManager.getInstance().toast("Activity created")
                    SignalManager.getInstance().vibrate()
                    finish()
                },
                { error ->
                    SignalManager.getInstance().toast("Failed to create activity")
                }
            )
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Activity")
            .setMessage("Are you sure you want to delete this activity? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteActivity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteActivity() {
        FirebaseManager.getInstance().deleteActivity(
            tripId,
            activityId,
            {
                SignalManager.getInstance().toast("Activity deleted")
                SignalManager.getInstance().vibrate()
                finish()
            },
            { error ->
                SignalManager.getInstance().toast("Failed to delete activity")
            }
        )
    }
}
