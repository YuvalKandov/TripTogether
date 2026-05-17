package com.example.triptogether.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.R
import com.example.triptogether.databinding.MemoryItemBinding
import com.example.triptogether.interfaces.MemoryCallback
import com.example.triptogether.model.Memory
import com.example.triptogether.model.TripActivity
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.ImageLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MemoriesAdapter(
    var memories: List<Memory> = listOf()
) : RecyclerView.Adapter<MemoriesAdapter.MemoryViewHolder>() {

    var memoryCallback: MemoryCallback? = null

    // Cache for activity data (activityId -> TripActivity)
    private val activityCache: MutableMap<String, TripActivity> = mutableMapOf()

    // Cache for like counts (memoryId -> count)
    private val likeCounts: MutableMap<String, Int> = mutableMapOf()

    // Cache for comment counts (memoryId -> count)
    private val commentCounts: MutableMap<String, Int> = mutableMapOf()

    // Cache for user's liked status (memoryId -> isLiked)
    private val userLikedStatus: MutableMap<String, Boolean> = mutableMapOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val binding = MemoryItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        with(holder) {
            with(getItem(position)) {
                // Load activity data for header
                loadActivityData(tripId, activityId, binding)

                // Set memory text
                if (text.isNotEmpty()) {
                    binding.memoryLBLText.text = text
                    binding.memoryLBLText.visibility = View.VISIBLE
                } else {
                    binding.memoryLBLText.visibility = View.GONE
                }

                // Format timestamp (just time for compact display)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val memoryDate = Date(createdAt)
                binding.memoryLBLTimestamp.text = timeFormat.format(memoryDate)

                // Load user data from Firebase
                loadUserData(userId, binding)

                // Load photos in grid
                loadPhotosGrid(photoUrls, binding)

                // Set like count and status
                val likeCount = likeCounts[id] ?: 0
                binding.memoryLBLLikeCount.text = likeCount.toString()

                val isLiked = userLikedStatus[id] ?: false
                binding.memoryBTNLike.setImageResource(
                    if (isLiked) R.drawable.heart else R.drawable.empty_heart
                )

                // Set comment count
                val commentCount = commentCounts[id] ?: 0
                binding.memoryLBLCommentCount.text = commentCount.toString()
            }
        }
    }

    private fun loadActivityData(tripId: String, activityId: String, binding: MemoryItemBinding) {
        // Check cache first
        val cachedActivity = activityCache[activityId]
        if (cachedActivity != null) {
            binding.memoryLBLActivityName.text = cachedActivity.title
            binding.memoryLBLActivityEmoji.text = getTagEmoji(cachedActivity.tag)
            return
        }

        // Set loading state
        binding.memoryLBLActivityName.text = "Loading..."
        binding.memoryLBLActivityEmoji.text = "📝"

        // Fetch from Firebase
        FirebaseManager.getInstance().getActivity(
            tripId,
            activityId,
            { activity ->
                activity?.let {
                    activityCache[activityId] = it
                    binding.memoryLBLActivityName.text = it.title
                    binding.memoryLBLActivityEmoji.text = getTagEmoji(it.tag)
                } ?: run {
                    binding.memoryLBLActivityName.text = "Unknown Activity"
                    binding.memoryLBLActivityEmoji.text = "📝"
                }
            },
            { error ->
                binding.memoryLBLActivityName.text = "Unknown Activity"
                binding.memoryLBLActivityEmoji.text = "📝"
            }
        )
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

    private fun loadUserData(userId: String, binding: MemoryItemBinding) {
        // Reset to default state first
        binding.memoryIMGUserPhoto.setImageResource(R.drawable.ic_profile)
        binding.memoryIMGUserPhoto.imageTintList = binding.root.context.getColorStateList(R.color.primary_400)
        binding.memoryLBLUserName.text = "Loading..."

        FirebaseManager.getInstance().getUser(userId) { user ->
            user?.let {
                binding.memoryLBLUserName.text = it.displayName.ifEmpty { "Unknown User" }
                if (it.photoUrl.isNotEmpty()) {
                    binding.memoryIMGUserPhoto.imageTintList = null
                    ImageLoader.getInstance().loadImage(
                        it.photoUrl,
                        binding.memoryIMGUserPhoto
                    )
                }
            } ?: run {
                binding.memoryLBLUserName.text = "Unknown User"
            }
        }
    }

    private fun loadPhotosGrid(photoUrls: List<String>, binding: MemoryItemBinding) {
        if (photoUrls.isEmpty()) {
            binding.memoryLLPhotosContainer.isVisible = false
            return
        }

        binding.memoryLLPhotosContainer.isVisible = true

        // Photo 1 - always show if there are photos
        ImageLoader.getInstance().loadImage(
            photoUrls[0],
            binding.memoryIMGPhoto1
        )

        // Set click listener for photo 1
        binding.memoryCVPhoto1.setOnClickListener {
            memoryCallback?.onPhotoClicked(photoUrls[0])
        }

        // Photo 2 - show if there are 2+ photos
        if (photoUrls.size >= 2) {
            binding.memoryCVPhoto2.isVisible = true
            ImageLoader.getInstance().loadImage(
                photoUrls[1],
                binding.memoryIMGPhoto2
            )

            // Set click listener for photo 2
            binding.memoryCVPhoto2.setOnClickListener {
                memoryCallback?.onPhotoClicked(photoUrls[1])
            }

            // Show "+X" badge if there are 3+ photos
            if (photoUrls.size > 2) {
                binding.memoryCVMorePhotos.isVisible = true
                binding.memoryLBLMorePhotos.text = "+${photoUrls.size - 2}"
            } else {
                binding.memoryCVMorePhotos.isVisible = false
            }
        } else {
            // Only 1 photo - hide photo 2 and adjust photo 1 to take full width
            binding.memoryCVPhoto2.isVisible = false
            binding.memoryCVMorePhotos.isVisible = false
        }
    }

    override fun getItemCount(): Int = memories.size

    fun getItem(position: Int): Memory = memories[position]

    fun updateLikeCount(memoryId: String, count: Int, isLikedByUser: Boolean) {
        likeCounts[memoryId] = count
        userLikedStatus[memoryId] = isLikedByUser
        val position = memories.indexOfFirst { it.id == memoryId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun updateCommentCount(memoryId: String, count: Int) {
        commentCounts[memoryId] = count
        val position = memories.indexOfFirst { it.id == memoryId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun setActivityCache(activities: Map<String, TripActivity>) {
        activityCache.clear()
        activityCache.putAll(activities)
    }

    inner class MemoryViewHolder(val binding: MemoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.memoryCVCard.setOnClickListener {
                memoryCallback?.onMemoryClicked(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
            binding.memoryCVCard.setOnLongClickListener { view ->
                memoryCallback?.onMemoryLongClicked(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition,
                    binding.memoryBTNMore  // Use the "more" button as anchor for consistency
                )
                true
            }
            binding.memoryBTNMore.setOnClickListener { view ->
                memoryCallback?.onMoreClicked(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition,
                    view
                )
            }
            binding.memoryBTNLike.setOnClickListener {
                memoryCallback?.onLikeClicked(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
            binding.memoryBTNComment.setOnClickListener {
                memoryCallback?.onCommentClicked(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
        }
    }
}
