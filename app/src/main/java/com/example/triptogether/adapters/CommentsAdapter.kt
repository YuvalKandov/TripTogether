package com.example.triptogether.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.R
import com.example.triptogether.databinding.CommentItemBinding
import com.example.triptogether.model.Comment
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.ImageLoader

class CommentsAdapter(
    var comments: List<Comment> = listOf()
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = CommentItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        with(holder) {
            with(getItem(position)) {
                // Load user data
                loadUserData(userId, binding)

                // Set comment text
                binding.commentLBLText.text = text

                // Format timestamp
                binding.commentLBLTimestamp.text = formatTimeAgo(createdAt)
            }
        }
    }

    private fun loadUserData(userId: String, binding: CommentItemBinding) {
        // Reset to default state first
        binding.commentIMGUserPhoto.setImageResource(R.drawable.ic_profile)
        binding.commentIMGUserPhoto.imageTintList = binding.root.context.getColorStateList(R.color.primary_400)
        binding.commentLBLUserName.text = "Loading..."

        FirebaseManager.getInstance().getUser(userId) { user ->
            user?.let {
                binding.commentLBLUserName.text = it.displayName.ifEmpty { "Unknown User" }
                if (it.photoUrl.isNotEmpty()) {
                    binding.commentIMGUserPhoto.imageTintList = null
                    ImageLoader.getInstance().loadImage(
                        it.photoUrl,
                        binding.commentIMGUserPhoto
                    )
                }
            } ?: run {
                binding.commentLBLUserName.text = "Unknown User"
            }
        }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                dateFormat.format(java.util.Date(timestamp))
            }
        }
    }

    override fun getItemCount(): Int = comments.size

    fun getItem(position: Int): Comment = comments[position]

    inner class CommentViewHolder(val binding: CommentItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
