package com.example.triptogether

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.triptogether.databinding.ActivityTripDetailBinding
import com.example.triptogether.model.Trip
import com.example.triptogether.ui.JournalFragment
import com.example.triptogether.ui.MembersFragment
import com.example.triptogether.ui.PlanFragment
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.DateUtils
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.SignalManager

class TripDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTripDetailBinding
    private var tripId: String = ""
    private var selectedDate: Long = 0L
    private var currentTrip: Trip? = null

    private var planFragment: PlanFragment? = null
    private var journalFragment: JournalFragment? = null
    private var membersFragment: MembersFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        getTripId()
        initToolbar()
        initBottomNavigation()
        loadTrip()
    }

    private fun getTripId() {
        val bundle: Bundle? = intent.extras
        tripId = bundle?.getString(Constants.BundleKeys.TRIP_ID, "") ?: ""
        selectedDate = bundle?.getLong(Constants.BundleKeys.SELECTED_DATE, 0L) ?: 0L
    }

    private fun initToolbar() {
        setSupportActionBar(binding.tripdetailTOOLBARToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        binding.tripdetailTOOLBARToolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initBottomNavigation() {
        binding.tripdetailBOTTOMNAVNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_plan -> {
                    showPlanFragment()
                    true
                }
                R.id.nav_journal -> {
                    showJournalFragment()
                    true
                }
                R.id.nav_members -> {
                    showMembersFragment()
                    true
                }
                else -> false
            }
        }
    }

    private fun showPlanFragment() {
        val trip = currentTrip ?: return
        if (planFragment == null) {
            planFragment = PlanFragment.newInstance(tripId, trip.startDate, trip.endDate, selectedDate)
        }
        replaceFragment(planFragment!!)
    }

    private fun showJournalFragment() {
        if (journalFragment == null) {
            journalFragment = JournalFragment.newInstance(tripId)
        }
        replaceFragment(journalFragment!!)
    }

    private fun showMembersFragment() {
        if (membersFragment == null) {
            membersFragment = MembersFragment.newInstance(tripId)
        }
        replaceFragment(membersFragment!!)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.tripdetail_FRAMELAYOUT_container, fragment)
            .commit()
    }

    private fun loadTrip() {
        FirebaseManager.getInstance().getTrip(
            tripId,
            { trip ->
                currentTrip = trip
                trip?.let {
                    updateToolbar(it)
                    // Show Plan fragment by default
                    binding.tripdetailBOTTOMNAVNavigation.selectedItemId = R.id.nav_plan
                }
            },
            { error ->
                SignalManager.getInstance().toast("Failed to load trip")
            }
        )
    }

    private fun updateToolbar(trip: Trip) {
        binding.tripdetailTOOLBARToolbar.title = trip.name
        binding.tripdetailTOOLBARToolbar.subtitle = formatDateRange(trip.startDate, trip.endDate)
    }

    private fun formatDateRange(startDate: Long, endDate: Long): String {
        val startFormatted = DateUtils.formatDate(startDate, "MMM dd")
        val endFormatted = DateUtils.formatDate(endDate, "MMM dd")
        return "$startFormatted - $endFormatted"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_trip_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_trip -> {
                editTrip()
                true
            }
            R.id.action_delete_trip -> {
                showDeleteTripConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun editTrip() {
        val intent = Intent(this, CreateTripActivity::class.java)
        intent.putExtra(Constants.BundleKeys.TRIP_ID, tripId)
        startActivity(intent)
    }

    private fun showDeleteTripConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete this trip? This will delete all activities and memories. This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTrip()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTrip() {
        val trip = currentTrip
        if (trip == null) {
            SignalManager.getInstance().toast("Unable to delete trip")
            return
        }

        FirebaseManager.getInstance().deleteTrip(
            tripId,
            trip.inviteCode,
            {
                SignalManager.getInstance().toast("Trip deleted")
                SignalManager.getInstance().vibrate()
                finish()
            },
            { error ->
                SignalManager.getInstance().toast("Failed to delete trip")
            }
        )
    }
}
