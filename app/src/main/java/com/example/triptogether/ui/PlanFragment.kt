package com.example.triptogether.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptogether.AddEditActivityActivity
import com.example.triptogether.R
import com.example.triptogether.adapters.ActivitiesAdapter
import com.example.triptogether.databinding.FragmentPlanBinding
import com.example.triptogether.interfaces.ActivityCallback
import com.example.triptogether.model.TripActivity
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.DateUtils
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.SignalManager
import com.google.android.material.chip.Chip
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class PlanFragment : Fragment() {
    private var binding: FragmentPlanBinding? = null
    private var tripId: String = ""
    private var tripStartDate: Long = 0L
    private var tripEndDate: Long = 0L
    private var initialSelectedDate: Long = 0L
    private var activitiesAdapter: ActivitiesAdapter = ActivitiesAdapter()

    private var allActivities: List<TripActivity> = emptyList()
    private var selectedDayTimestamp: Long = 0L
    private var totalDays: Int = 0
    private var activitiesListener: ValueEventListener? = null

    companion object {
        private const val ARG_TRIP_ID: String = "trip_id"
        private const val ARG_START_DATE: String = "start_date"
        private const val ARG_END_DATE: String = "end_date"
        private const val ARG_SELECTED_DATE: String = "selected_date"

        fun newInstance(tripId: String, startDate: Long, endDate: Long, selectedDate: Long = 0L): PlanFragment {
            val fragment = PlanFragment()
            val args = Bundle()
            args.putString(ARG_TRIP_ID, tripId)
            args.putLong(ARG_START_DATE, startDate)
            args.putLong(ARG_END_DATE, endDate)
            args.putLong(ARG_SELECTED_DATE, selectedDate)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tripId = it.getString(ARG_TRIP_ID, "")
            tripStartDate = it.getLong(ARG_START_DATE, 0L)
            tripEndDate = it.getLong(ARG_END_DATE, 0L)
            initialSelectedDate = it.getLong(ARG_SELECTED_DATE, 0L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPlanBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupDaySelector()
        loadActivities()
    }

    private fun initViews() {
        activitiesAdapter.activityCallback = object : ActivityCallback {
            override fun onActivityClicked(activity: TripActivity, position: Int) {
                val intent = Intent(requireContext(), AddEditActivityActivity::class.java)
                intent.putExtra(Constants.BundleKeys.TRIP_ID, tripId)
                intent.putExtra(Constants.BundleKeys.ACTIVITY_ID, activity.id)
                intent.putExtra(Constants.BundleKeys.START_DATE, tripStartDate)
                intent.putExtra(Constants.BundleKeys.END_DATE, tripEndDate)
                startActivity(intent)
            }

            override fun onActivityCompletedToggle(activity: TripActivity, position: Int) {
                FirebaseManager.getInstance().updateActivity(
                    activity,
                    {
                        SignalManager.getInstance().vibrate()
                    },
                    { error ->
                        SignalManager.getInstance().toast("Failed to update activity")
                        activitiesAdapter.notifyItemChanged(position)
                    }
                )
            }
        }

        binding?.planRVActivities?.adapter = activitiesAdapter
        binding?.planRVActivities?.layoutManager = LinearLayoutManager(requireContext())

        binding?.planFABAdd?.setOnClickListener {
            val intent = Intent(requireContext(), AddEditActivityActivity::class.java)
            intent.putExtra(Constants.BundleKeys.TRIP_ID, tripId)
            intent.putExtra(Constants.BundleKeys.SELECTED_DATE, selectedDayTimestamp)
            intent.putExtra(Constants.BundleKeys.START_DATE, tripStartDate)
            intent.putExtra(Constants.BundleKeys.END_DATE, tripEndDate)
            startActivity(intent)
        }
    }

    private fun setupDaySelector() {
        if (tripStartDate == 0L || tripEndDate == 0L) return

        totalDays = DateUtils.getDaysBetween(tripStartDate, tripEndDate)

        // Determine which day to select initially
        val initialDayIndex = if (initialSelectedDate > 0L) {
            // Normalize both dates to start of day to ensure accurate comparison
            val normalizedStart = getStartOfDay(tripStartDate)
            val normalizedSelected = getStartOfDay(initialSelectedDate)
            // Calculate day index from the initial selected date
            val daysDiff = ((normalizedSelected - normalizedStart) / (24 * 60 * 60 * 1000L)).toInt()
            // Ensure it's within bounds
            daysDiff.coerceIn(0, totalDays - 1)
        } else {
            0
        }

        selectedDayTimestamp = tripStartDate + (initialDayIndex * 24 * 60 * 60 * 1000L)

        binding?.planCHIPGROUPDays?.removeAllViews()

        for (dayIndex in 0 until totalDays) {
            val dayTimestamp = tripStartDate + (dayIndex * 24 * 60 * 60 * 1000L)
            val chip = Chip(requireContext()).apply {
                text = "Day ${dayIndex + 1}"
                tag = dayTimestamp
                isCheckable = true
                isChecked = dayIndex == initialDayIndex
                setChipBackgroundColorResource(R.color.chip_day_background)
                setTextColor(resources.getColorStateList(R.color.chip_day_text, null))
                chipCornerRadius = resources.getDimension(R.dimen.day_chip_corner_radius)
                chipMinHeight = resources.getDimension(R.dimen.day_chip_height)
                setOnClickListener {
                    selectedDayTimestamp = dayTimestamp
                    updateSelectedDayLabel(dayIndex)
                    filterActivitiesByDay()
                }
            }
            binding?.planCHIPGROUPDays?.addView(chip)
        }

        updateSelectedDayLabel(initialDayIndex)
    }

    private fun updateSelectedDayLabel(dayIndex: Int) {
        val dayTimestamp = tripStartDate + (dayIndex * 24 * 60 * 60 * 1000L)
        val formattedDate = DateUtils.formatDate(dayTimestamp)
        binding?.planLBLSelectedDay?.text = "Day ${dayIndex + 1} - $formattedDate"
    }

    private fun loadActivities() {
        if (binding == null) return

        removeActivitiesListener()

        activitiesListener = FirebaseManager.getInstance().getActivitiesForTrip(
            tripId,
            { activities ->
                if (binding != null) {
                    allActivities = activities
                    filterActivitiesByDay()

                    // Load memory count badges for all activities
                    allActivities.forEach { activity ->
                        FirebaseManager.getInstance().getMemoryCountForActivity(activity.id) { count ->
                            if (binding != null) {
                                activitiesAdapter.updateMemoryCount(activity.id, count)
                            }
                        }
                    }
                }
            },
            { error ->
                if (binding != null) {
                    SignalManager.getInstance().toast("Failed to load activities")
                }
            }
        )
    }

    private fun filterActivitiesByDay() {
        val filteredActivities = allActivities.filter { activity ->
            isSameDay(activity.date, selectedDayTimestamp)
        }.sortedBy { it.startTime }

        activitiesAdapter.activities = filteredActivities
        activitiesAdapter.notifyDataSetChanged()
        updateEmptyState(filteredActivities.isEmpty())
        updateTimelineVisibility(filteredActivities.size)
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val day1 = timestamp1 / (24 * 60 * 60 * 1000L)
        val day2 = timestamp2 / (24 * 60 * 60 * 1000L)
        return day1 == day2
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding?.planLLEmpty?.isVisible = isEmpty
        binding?.planRVActivities?.isVisible = !isEmpty
    }

    private fun updateTimelineVisibility(itemCount: Int) {
        activitiesAdapter.updateTimelineVisibility(itemCount)
    }

    override fun onResume() {
        super.onResume()
        loadActivities()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeActivitiesListener()
        binding = null
    }

    private fun removeActivitiesListener() {
        activitiesListener?.let {
            FirebaseManager.getInstance().removeActivitiesForTripListener(tripId, it)
        }
        activitiesListener = null
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
