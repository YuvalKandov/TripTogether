package com.example.triptogether.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.utilities.SignalManager
import com.example.triptogether.databinding.ActivityItemBinding
import com.example.triptogether.interfaces.ActivityCallback
import com.example.triptogether.model.TripActivity

class ActivitiesAdapter(
    var activities: List<TripActivity> = listOf()
) : RecyclerView.Adapter<ActivitiesAdapter.ActivityViewHolder>() {

    var activityCallback: ActivityCallback? = null

    // Map to store memory counts for each activity
    private val memoryCounts: MutableMap<String, Int> = mutableMapOf()
    private var totalItemCount: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ActivityItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        with(holder) {
            with(getItem(position)) {
                // Tag emoji in timeline badge
                binding.activityLBLTag.text = getTagEmoji(tag)

                // Title
                binding.activityLBLTitle.text = title

                // Time
                val timeText = if (endTime.isNotEmpty()) {
                    "$startTime - $endTime"
                } else {
                    startTime
                }
                binding.activityLBLTime.text = timeText

                // Location
                if (location.isNotEmpty()) {
                    binding.activityLBLLocation.text = location
                    binding.activityLBLLocation.visibility = View.VISIBLE
                    binding.activityLBLLocation.setOnClickListener {
                        val encodedLocation = Uri.encode(location)
                        val geoUri = Uri.parse("geo:0,0?q=$encodedLocation")
                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                        try {
                            holder.itemView.context.startActivity(mapIntent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            SignalManager.getInstance().toast("No maps app found")
                        }
                    }
                } else {
                    binding.activityLBLLocation.visibility = View.GONE
                    binding.activityLBLLocation.setOnClickListener(null)
                }

                // Description
                if (description.isNotEmpty()) {
                    binding.activityLBLDescription.text = description
                    binding.activityLBLDescription.visibility = View.VISIBLE
                } else {
                    binding.activityLBLDescription.visibility = View.GONE
                }

                // Completed checkbox
                binding.activityCBCompleted.setOnCheckedChangeListener(null)
                binding.activityCBCompleted.isChecked = isCompleted
                binding.activityCBCompleted.setOnCheckedChangeListener { _, isChecked ->
                    if (this.isCompleted != isChecked) {
                        toggleCompleted()
                        activityCallback?.onActivityCompletedToggle(
                            this,
                            absoluteAdapterPosition
                        )
                    }
                }

                // Memory badge
                val memoryCount = memoryCounts[id] ?: 0
                if (memoryCount > 0) {
                    binding.activityCVMemoryBadge.visibility = View.VISIBLE
                    binding.activityLBLMemoryCount.text = if (memoryCount == 1) "1 photo" else "$memoryCount photos"
                } else {
                    binding.activityCVMemoryBadge.visibility = View.GONE
                }

                // Timeline visibility
                updateTimelineLines(holder, position)
            }
        }
    }

    private fun updateTimelineLines(holder: ActivityViewHolder, position: Int) {
        // Hide top line for first item
        holder.binding.activityVIEWLineTop.isVisible = position != 0

        // Hide bottom line for last item
        holder.binding.activityVIEWLineBottom.isVisible = position != totalItemCount - 1
    }

    override fun getItemCount(): Int = activities.size

    fun getItem(position: Int): TripActivity = activities[position]

    fun updateMemoryCount(activityId: String, count: Int) {
        memoryCounts[activityId] = count
        val position = activities.indexOfFirst { it.id == activityId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun updateTimelineVisibility(itemCount: Int) {
        totalItemCount = itemCount
    }

    private fun getTagEmoji(tag: String): String {
        return when (tag) {
            TripActivity.Tags.FOOD -> "🍔"
            TripActivity.Tags.TRANSPORT -> "🚗"
            TripActivity.Tags.HOTEL -> "🏨"
            TripActivity.Tags.ACTIVITY -> "🎯"
            else -> "📝"
        }
    }

    inner class ActivityViewHolder(val binding: ActivityItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.activityCVCard.setOnClickListener {
                activityCallback?.onActivityClicked(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
        }
    }
}
