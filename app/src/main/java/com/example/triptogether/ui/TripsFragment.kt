package com.example.triptogether.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptogether.CreateTripActivity
import com.example.triptogether.JoinTripActivity
import com.example.triptogether.ProfileActivity
import com.example.triptogether.TripDetailActivity
import com.example.triptogether.adapters.TripsAdapter
import com.example.triptogether.databinding.FragmentTripsBinding
import com.example.triptogether.interfaces.TripCallback
import com.example.triptogether.model.Trip
import com.example.triptogether.model.TripActivity
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.ImageLoader
import com.example.triptogether.utilities.SignalManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TripsFragment : Fragment() {
    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!
    private var activeTripsAdapter: TripsAdapter = TripsAdapter()
    private var pastTripsAdapter: TripsAdapter = TripsAdapter()
    private var todayCardTripId: String? = null
    private var tripsListener: ValueEventListener? = null
    private val todayActivityListeners: MutableMap<String, ValueEventListener> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        loadTrips()
    }

    private fun initViews() {
        // Active Trips adapter setup
        activeTripsAdapter.tripCallback = object : TripCallback {
            override fun onTripClicked(trip: Trip, position: Int) {
                navigateToTripDetail(trip.id)
            }
        }
        binding.tripsRVList.adapter = activeTripsAdapter
        binding.tripsRVList.layoutManager = LinearLayoutManager(requireContext())

        // Past Trips adapter setup
        pastTripsAdapter.tripCallback = object : TripCallback {
            override fun onTripClicked(trip: Trip, position: Int) {
                navigateToTripDetail(trip.id)
            }
        }
        binding.tripsRVPast.adapter = pastTripsAdapter
        binding.tripsRVPast.layoutManager = LinearLayoutManager(requireContext())

        binding.tripsFABAdd.setOnClickListener {
            val intent = Intent(requireContext(), CreateTripActivity::class.java)
            startActivity(intent)
        }

        binding.tripsFABJoin.setOnClickListener {
            val intent = Intent(requireContext(), JoinTripActivity::class.java)
            startActivity(intent)
        }

        // Profile avatar click - navigate to ProfileActivity
        binding.tripsCVProfile.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        // Load user profile photo
        loadProfilePhoto()

        // Today Card click listener - navigate to today's date in the plan
        binding.tripsCVToday.setOnClickListener {
            todayCardTripId?.let { tripId ->
                val today = getStartOfDay(System.currentTimeMillis())
                navigateToTripDetail(tripId, today)
            }
        }
    }

    private fun navigateToTripDetail(tripId: String, selectedDate: Long? = null) {
        val intent = Intent(requireContext(), TripDetailActivity::class.java)
        intent.putExtra(Constants.BundleKeys.TRIP_ID, tripId)
        selectedDate?.let {
            intent.putExtra(Constants.BundleKeys.SELECTED_DATE, it)
        }
        startActivity(intent)
    }

    private fun loadTrips() {
        if (_binding == null || FirebaseAuth.getInstance().currentUser == null) return

        removeTripsListener()
        removeTodayActivityListeners()

        tripsListener = FirebaseManager.getInstance().getUserTrips(
            onDataChange = { trips ->
                if (_binding != null) {
                    val now = System.currentTimeMillis()

                    // Separate into active (Planning + Ongoing) and past (Completed) trips
                    val activeTrips = trips.filter { it.endDate >= now }
                        .sortedBy { it.startDate }  // Soonest first
                    val pastTrips = trips.filter { it.endDate < now }
                        .sortedByDescending { it.endDate }  // Most recent first

                    // Update active trips adapter
                    activeTripsAdapter.trips = activeTrips
                    activeTripsAdapter.notifyDataSetChanged()

                    // Update past trips adapter
                    pastTripsAdapter.trips = pastTrips
                    pastTripsAdapter.notifyDataSetChanged()

                    // Update visibility states
                    updateEmptyState(activeTrips.isEmpty(), pastTrips.isEmpty())
                    updatePastTripsSection(pastTrips.isNotEmpty())

                    // Today card only shows for active trips
                    findTodayActivity(trips)

                    // Load member counts for both lists
                    loadMemberCounts(activeTrips, activeTripsAdapter)
                    loadMemberCounts(pastTrips, pastTripsAdapter)
                }
            },
            onError = { error ->
                if (_binding != null) {
                    Log.w("TripsFragment", "Failed to load trips", error.toException())
                    SignalManager.getInstance().toast("Failed to load trips")
                    updateEmptyState(isEmpty = true, hasPastTrips = false)
                }
            }
        )
    }

    private fun loadMemberCounts(trips: List<Trip>, adapter: TripsAdapter) {
        for (trip in trips) {
            FirebaseManager.getInstance().getMemberCount(
                tripId = trip.id,
                onSuccess = { count ->
                    if (_binding != null) {
                        adapter.updateMemberCount(trip.id, count)
                    }
                },
                onError = { error ->
                    Log.w("TripsFragment", "Failed to load member count for trip ${trip.id}", error.toException())
                }
            )
        }
    }

    private fun loadProfilePhoto() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.photoUrl?.let { photoUrl ->
            ImageLoader.getInstance().loadImage(
                photoUrl.toString(),
                binding.tripsIMGProfile
            )
        }
    }

    private fun updateEmptyState(isEmpty: Boolean, hasPastTrips: Boolean = false) {
        val currentBinding = _binding ?: return

        // Show empty state only when there are no trips at all (neither active nor past)
        val noTripsAtAll = isEmpty && !hasPastTrips
        currentBinding.tripsLLEmpty.isVisible = noTripsAtAll

        // Show active trips section if there are active trips OR if we have past trips
        // (to maintain the "Your Trips" header even when no active trips but past exists)
        currentBinding.tripsLBLSection.isVisible = !noTripsAtAll
        currentBinding.tripsRVList.isVisible = !isEmpty
    }

    private fun updatePastTripsSection(hasPastTrips: Boolean) {
        val currentBinding = _binding ?: return

        currentBinding.tripsLBLPastSection.isVisible = hasPastTrips
        currentBinding.tripsRVPast.isVisible = hasPastTrips
    }

    private fun findTodayActivity(trips: List<Trip>) {
        if (_binding == null) return

        removeTodayActivityListeners()

        val today = getStartOfDay(System.currentTimeMillis())

        // Find ongoing trips (today is within trip date range)
        val ongoingTrips = trips.filter { trip ->
            val tripStart = getStartOfDay(trip.startDate)
            val tripEnd = getStartOfDay(trip.endDate)
            today in tripStart..tripEnd
        }

        if (ongoingTrips.isEmpty()) {
            hideTodayCard()
            return
        }

        // Check each ongoing trip for today's activities
        var foundActivity = false
        var checkedTrips = 0

        for (trip in ongoingTrips) {
            val listener = FirebaseManager.getInstance().getActivitiesForTrip(
                tripId = trip.id,
                onDataChange = { activities ->
                    if (_binding != null) {
                        checkedTrips++

                        if (!foundActivity) {
                            val nextActivity = findNextActivityForToday(activities, today)
                            if (nextActivity != null) {
                                foundActivity = true
                                showTodayCard(nextActivity, trip, activities)
                            } else if (checkedTrips >= ongoingTrips.size) {
                                // All trips checked, no activity found
                                hideTodayCard()
                            }
                        }
                    }
                },
                onError = { error ->
                    if (_binding != null) {
                        checkedTrips++
                        Log.w("TripsFragment", "Failed to load activities for trip ${trip.id}", error.toException())
                        if (checkedTrips >= ongoingTrips.size && !foundActivity) {
                            hideTodayCard()
                        }
                    }
                }
            )
            todayActivityListeners[trip.id] = listener
        }
    }

    private fun findNextActivityForToday(activities: List<TripActivity>, today: Long): TripActivity? {
        // Filter activities for today only
        val todayActivities = activities.filter { activity ->
            getStartOfDay(activity.date) == today
        }

        if (todayActivities.isEmpty()) return null

        // Get current time in HH:mm format
        val currentTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = currentTimeFormat.format(Date())

        // Find the next uncompleted activity that hasn't started yet or started recently (within 30 min)
        val currentMinutes = timeToMinutes(currentTime)

        val upcomingActivity = todayActivities
            .filter { activity ->
                if (activity.isCompleted) return@filter false
                val activityMinutes = timeToMinutes(activity.startTime)
                // Show if activity is upcoming OR started within last 30 minutes
                activityMinutes >= currentMinutes - 30
            }
            .minByOrNull { it.startTime }

        return upcomingActivity
    }

    private fun countValidActivitiesForToday(activities: List<TripActivity>, today: Long): Int {
        val currentTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = currentTimeFormat.format(Date())
        val currentMinutes = timeToMinutes(currentTime)

        return activities.count { activity ->
            if (getStartOfDay(activity.date) != today) return@count false
            if (activity.isCompleted) return@count false
            val activityMinutes = timeToMinutes(activity.startTime)
            // Count only activities that are upcoming or started within last 30 minutes
            activityMinutes >= currentMinutes - 30
        }
    }

    private fun calculateDayOfTrip(trip: Trip, today: Long): Pair<Int, Int> {
        val tripStart = getStartOfDay(trip.startDate)
        val tripEnd = getStartOfDay(trip.endDate)

        val totalDays = ((tripEnd - tripStart) / (24 * 60 * 60 * 1000L)).toInt() + 1
        val currentDay = ((today - tripStart) / (24 * 60 * 60 * 1000L)).toInt() + 1

        return Pair(currentDay, totalDays)
    }

    private fun showTodayCard(activity: TripActivity, trip: Trip, activities: List<TripActivity>) {
        if (_binding == null) return

        val today = getStartOfDay(System.currentTimeMillis())
        todayCardTripId = trip.id

        binding.tripsCVToday.isVisible = true

        // Trip name
        binding.tripsLBLTodayTripName.text = trip.name

        // Day X of Y
        val (currentDay, totalDays) = calculateDayOfTrip(trip, today)
        binding.tripsLBLTodayDay.text = "Day $currentDay of $totalDays"

        // Activities count
        val validActivitiesCount = countValidActivitiesForToday(activities, today)
        val activitiesText = if (validActivitiesCount == 1) {
            "1 activity today"
        } else {
            "$validActivitiesCount activities today"
        }
        binding.tripsLBLTodayActivitiesCount.text = activitiesText

        // Next activity
        val nextText = formatNextActivityText(activity)
        binding.tripsLBLTodayNext.text = nextText
    }

    private fun formatNextActivityText(activity: TripActivity): String {
        return "Next: ${activity.title} at ${activity.startTime}"
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun hideTodayCard() {
        if (_binding == null) return
        binding.tripsCVToday.isVisible = false
        todayCardTripId = null
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

    override fun onResume() {
        super.onResume()
        // Reload to refresh Today Card
        loadTrips()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeTripsListener()
        removeTodayActivityListeners()
        _binding = null
    }

    private fun removeTripsListener() {
        tripsListener?.let {
            FirebaseManager.getInstance().removeUserTripsListener(it)
        }
        tripsListener = null
    }

    private fun removeTodayActivityListeners() {
        todayActivityListeners.forEach { (tripId, listener) ->
            FirebaseManager.getInstance().removeActivitiesForTripListener(tripId, listener)
        }
        todayActivityListeners.clear()
    }
}
