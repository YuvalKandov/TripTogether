package com.example.triptogether.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.R
import com.example.triptogether.databinding.TripItemBinding
import com.example.triptogether.interfaces.TripCallback
import com.example.triptogether.model.Trip
import com.example.triptogether.utilities.DateUtils
import com.example.triptogether.utilities.ImageLoader

class TripsAdapter(
    var trips: List<Trip> = emptyList()
) : RecyclerView.Adapter<TripsAdapter.TripViewHolder>() {

    var tripCallback: TripCallback? = null

    // Map to store member counts for each trip
    private val memberCounts: MutableMap<String, Int> = mutableMapOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = TripItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        with(holder) {
            with(getItem(position)) {
                // Trip name
                binding.tripLBLName.text = name

                // Dates
                binding.tripLBLDates.text = DateUtils.formatDateRange(startDate, endDate)

                // Members count
                val memberCount = memberCounts[id] ?: 1
                binding.tripLBLMembers.text = if (memberCount == 1) "1 member" else "$memberCount members"

                // Status chip
                val status = getTripStatus(startDate, endDate)
                binding.tripCHIPStatus.text = status.displayName
                binding.tripCHIPStatus.setChipBackgroundColorResource(status.colorRes)
                binding.tripCHIPStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))

                // Cover image
                if (coverImageUrl.isNotEmpty()) {
                    ImageLoader.getInstance().loadImage(
                        coverImageUrl,
                        binding.tripIMGCover
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int = trips.size

    fun getItem(position: Int): Trip = trips[position]

    fun updateMemberCount(tripId: String, count: Int) {
        memberCounts[tripId] = count
        val position = trips.indexOfFirst { it.id == tripId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    private fun getTripStatus(startDate: Long, endDate: Long): TripStatus {
        val now = System.currentTimeMillis()
        return when {
            now < startDate -> TripStatus.PLANNING
            now > endDate -> TripStatus.COMPLETED
            else -> TripStatus.ONGOING
        }
    }

    enum class TripStatus(val displayName: String, val colorRes: Int) {
        PLANNING("Planning", R.color.status_planning),
        ONGOING("Ongoing", R.color.status_ongoing),
        COMPLETED("Completed", R.color.status_completed)
    }

    inner class TripViewHolder(val binding: TripItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.tripCVCard.setOnClickListener {
                tripCallback?.onTripClicked(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
        }
    }
}
