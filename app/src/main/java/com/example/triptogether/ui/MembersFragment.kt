package com.example.triptogether.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptogether.adapters.MembersAdapter
import com.example.triptogether.databinding.FragmentMembersBinding
import com.example.triptogether.model.Trip
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.SignalManager
import com.google.firebase.database.ValueEventListener

class MembersFragment : Fragment() {
    private var binding: FragmentMembersBinding? = null
    private var tripId: String = ""
    private var membersAdapter: MembersAdapter = MembersAdapter()
    private var currentTrip: Trip? = null
    private var membersListener: ValueEventListener? = null

    companion object {
        private const val ARG_TRIP_ID = "trip_id"

        fun newInstance(tripId: String): MembersFragment {
            val fragment = MembersFragment()
            val args = Bundle()
            args.putString(ARG_TRIP_ID, tripId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tripId = it.getString(ARG_TRIP_ID, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        loadTripData()
        loadMembers()
    }

    private fun initViews() {
        binding?.membersRVList?.adapter = membersAdapter
        binding?.membersRVList?.layoutManager = LinearLayoutManager(requireContext())

        binding?.membersFABInvite?.setOnClickListener {
            showInviteCodeDialog()
        }
    }

    private fun loadTripData() {
        FirebaseManager.getInstance().getTrip(
            tripId = tripId,
            onDataChange = { trip ->
                currentTrip = trip
            },
            onError = { error ->
                Log.w("MembersFragment", "Failed to load trip data.", error.toException())
            }
        )
    }

    private fun loadMembers() {
        removeMembersListener()

        membersListener = FirebaseManager.getInstance().getTripMembers(
            tripId = tripId,
            onDataChange = { members ->
                if (binding != null) {
                    membersAdapter.members = members
                    membersAdapter.notifyDataSetChanged()
                    updateUI(members.size)
                }
            },
            onError = { error ->
                if (binding != null) {
                    Log.w("MembersFragment", "Failed to load members.", error.toException())
                    SignalManager.getInstance().toast("Failed to load members")
                    updateUI(0)
                }
            }
        )
    }

    private fun updateUI(memberCount: Int) {
        // Update member count label
        val countText = if (memberCount == 1) "(1 person)" else "($memberCount people)"
        binding?.membersLBLCount?.text = countText

        // Show/hide empty state
        binding?.membersLAYOUTEmpty?.isVisible = memberCount == 0
        binding?.membersRVList?.isVisible = memberCount > 0
    }

    private fun showInviteCodeDialog() {
        val trip = currentTrip
        if (trip != null) {
            val dialog = InviteCodeDialogFragment.newInstance(
                inviteCode = trip.inviteCode,
                tripName = trip.name
            )
            dialog.show(childFragmentManager, "InviteCodeDialog")
        } else {
            SignalManager.getInstance().toast("Unable to load invite code")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeMembersListener()
        binding = null
    }

    private fun removeMembersListener() {
        membersListener?.let {
            FirebaseManager.getInstance().removeTripMembersListener(tripId, it)
        }
        membersListener = null
    }
}
