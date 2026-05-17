package com.example.triptogether.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.R
import com.example.triptogether.databinding.MemberItemBinding
import com.example.triptogether.model.TripMember
import com.example.triptogether.utilities.ImageLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MembersAdapter(
    var members: List<TripMember> = listOf()
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = MemberItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        with(holder) {
            with(getItem(position)) {
                // Set name
                binding.memberLBLName.text = displayName.ifEmpty { "Unknown User" }

                // Format joined date
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val joinedDate = Date(joinedAt)
                binding.memberLBLJoined.text = "Joined ${dateFormat.format(joinedDate)}"

                // Show owner badge chip
                binding.memberCHIPRole.isVisible = role == TripMember.ROLE_OWNER

                // Load photo with placeholder handling
                if (photoUrl.isNotEmpty()) {
                    // Clear tint to show actual photo
                    binding.memberIMGPhoto.imageTintList = null
                    ImageLoader.getInstance().loadImage(
                        photoUrl,
                        binding.memberIMGPhoto
                    )
                } else {
                    // Reset to default placeholder with tint
                    binding.memberIMGPhoto.setImageResource(R.drawable.ic_profile)
                    binding.memberIMGPhoto.imageTintList =
                        binding.root.context.getColorStateList(R.color.primary_400)
                }
            }
        }
    }

    override fun getItemCount(): Int = members.size

    fun getItem(position: Int): TripMember = members[position]

    inner class MemberViewHolder(val binding: MemberItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
