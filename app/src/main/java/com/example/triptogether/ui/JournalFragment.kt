package com.example.triptogether.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptogether.AddMemoryActivity
import com.example.triptogether.R
import com.example.triptogether.adapters.MemoriesAdapter
import com.example.triptogether.databinding.FragmentJournalBinding
import com.example.triptogether.interfaces.MemoryCallback
import com.example.triptogether.model.Memory
import com.example.triptogether.model.TripActivity
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.SignalManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener

class JournalFragment : Fragment() {
    private var binding: FragmentJournalBinding? = null
    private var tripId: String = ""
    private var memoriesAdapter: MemoriesAdapter = MemoriesAdapter()

    // Store listeners for cleanup
    private val likeListeners: MutableMap<String, ValueEventListener> = mutableMapOf()
    private val commentListeners: MutableMap<String, ValueEventListener> = mutableMapOf()

    companion object {
        private const val ARG_TRIP_ID = "trip_id"

        fun newInstance(tripId: String): JournalFragment {
            val fragment = JournalFragment()
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
        binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        loadMemories()
    }

    private fun initViews() {
        memoriesAdapter.memoryCallback = object : MemoryCallback {
            override fun onMemoryClicked(memory: Memory, position: Int) {
                // Could open detail view if needed
            }

            override fun onMemoryLongClicked(memory: Memory, position: Int, anchorView: View) {
                showMemoryOptionsPopup(memory, anchorView)
            }

            override fun onPhotoClicked(photoUrl: String) {
                showPhotoViewer(photoUrl)
            }

            override fun onMoreClicked(memory: Memory, position: Int, anchorView: View) {
                showMemoryOptionsPopup(memory, anchorView)
            }

            override fun onLikeClicked(memory: Memory, position: Int) {
                toggleLike(memory)
            }

            override fun onCommentClicked(memory: Memory, position: Int) {
                showCommentsDialog(memory)
            }
        }

        binding?.journalRVMemories?.layoutManager = LinearLayoutManager(requireContext())
        binding?.journalRVMemories?.adapter = memoriesAdapter

        binding?.journalFABAdd?.setOnClickListener {
            val intent = Intent(requireContext(), AddMemoryActivity::class.java)
            intent.putExtra(Constants.BundleKeys.TRIP_ID, tripId)
            startActivity(intent)
        }
    }

    private fun showPhotoViewer(photoUrl: String) {
        val dialog = PhotoViewerDialogFragment.newInstance(photoUrl)
        dialog.show(childFragmentManager, "photo_viewer")
    }

    private fun showMemoryOptionsPopup(memory: Memory, anchorView: View) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Only show edit/delete options if the user is the owner of the memory
        if (memory.userId != currentUserId) {
            SignalManager.getInstance().toast("You can only edit your own memories")
            return
        }

        val popup = PopupMenu(requireContext(), anchorView)
        popup.menuInflater.inflate(R.menu.menu_memory_options, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_memory -> {
                    editMemory(memory)
                    true
                }
                R.id.action_delete_memory -> {
                    confirmDeleteMemory(memory)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun editMemory(memory: Memory) {
        val intent = Intent(requireContext(), AddMemoryActivity::class.java)
        intent.putExtra(Constants.BundleKeys.TRIP_ID, tripId)
        intent.putExtra(Constants.BundleKeys.MEMORY_ID, memory.id)
        intent.putExtra(Constants.BundleKeys.ACTIVITY_ID, memory.activityId)
        startActivity(intent)
    }

    private fun confirmDeleteMemory(memory: Memory) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Memory")
            .setMessage("Are you sure you want to delete this memory? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteMemory(memory)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMemory(memory: Memory) {
        FirebaseManager.getInstance().deleteMemory(
            memory.activityId,
            memory.id,
            {
                SignalManager.getInstance().toast("Memory deleted")
                SignalManager.getInstance().vibrate()
                loadMemories()
            },
            { error ->
                SignalManager.getInstance().toast("Failed to delete memory")
            }
        )
    }

    private fun toggleLike(memory: Memory) {
        FirebaseManager.getInstance().toggleLike(
            memory.id,
            { isLiked ->
                // Real-time listener will automatically update the UI
                SignalManager.getInstance().vibrate()
            },
            { error ->
                SignalManager.getInstance().toast("Failed to update like")
            }
        )
    }

    private fun loadActivityCache(memories: List<Memory>) {
        val uniqueActivityIds = memories.map { it.activityId }.distinct()
        val activityMap = mutableMapOf<String, TripActivity>()
        var fetched = 0

        uniqueActivityIds.forEach { activityId ->
            val tripIdForActivity = memories.first { it.activityId == activityId }.tripId
            FirebaseManager.getInstance().getActivity(
                tripIdForActivity,
                activityId,
                { activity ->
                    activity?.let { activityMap[activityId] = it }
                    fetched++
                    if (fetched == uniqueActivityIds.size) {
                        memoriesAdapter.setActivityCache(activityMap)
                        memoriesAdapter.notifyDataSetChanged()
                    }
                },
                { error ->
                    fetched++
                    if (fetched == uniqueActivityIds.size) {
                        memoriesAdapter.setActivityCache(activityMap)
                        memoriesAdapter.notifyDataSetChanged()
                    }
                }
            )
        }
    }

    private fun setupRealtimeListeners(memories: List<Memory>) {
        // Clear old listeners first
        removeAllListeners()

        // Set up real-time listeners for each memory
        memories.forEach { memory ->
            // Like listener
            val likeListener = FirebaseManager.getInstance().listenToLikes(memory.id) { count, isLikedByUser ->
                memoriesAdapter.updateLikeCount(memory.id, count, isLikedByUser)
            }
            likeListeners[memory.id] = likeListener

            // Comment count listener
            val commentListener = FirebaseManager.getInstance().listenToCommentCount(memory.id) { count ->
                memoriesAdapter.updateCommentCount(memory.id, count)
            }
            commentListeners[memory.id] = commentListener
        }
    }

    private fun removeAllListeners() {
        // Remove all like listeners
        likeListeners.forEach { (memoryId, listener) ->
            FirebaseManager.getInstance().removeLikeListener(memoryId, listener)
        }
        likeListeners.clear()

        // Remove all comment listeners
        commentListeners.forEach { (memoryId, listener) ->
            FirebaseManager.getInstance().removeCommentCountListener(memoryId, listener)
        }
        commentListeners.clear()
    }

    private fun showCommentsDialog(memory: Memory) {
        val dialog = CommentsDialogFragment.newInstance(memory.id)
        dialog.show(childFragmentManager, "comments_dialog")
    }

    private fun loadMemories() {
        if (tripId.isEmpty()) {
            Log.w("JournalFragment", "Trip ID is empty")
            return
        }

        binding?.journalPBLoading?.isVisible = true
        binding?.journalRVMemories?.isVisible = false
        binding?.journalLLEmpty?.isVisible = false

        FirebaseManager.getInstance().getMemoriesForTrip(
            tripId,
            { memories ->
                binding?.journalPBLoading?.isVisible = false
                if (memories.isEmpty()) {
                    binding?.journalRVMemories?.isVisible = false
                    binding?.journalLLEmpty?.isVisible = true
                    removeAllListeners()
                } else {
                    binding?.journalRVMemories?.isVisible = true
                    binding?.journalLLEmpty?.isVisible = false
                    memoriesAdapter.memories = memories
                    memoriesAdapter.notifyDataSetChanged()

                    // Pre-fetch activity data for all memories to seed the adapter cache
                    loadActivityCache(memories)

                    // Set up real-time listeners for likes and comments
                    setupRealtimeListeners(memories)
                }
            },
            { error ->
                binding?.journalPBLoading?.isVisible = false
                binding?.journalRVMemories?.isVisible = false
                binding?.journalLLEmpty?.isVisible = true
                Log.w("JournalFragment", "Failed to load memories", error.toException())
            }
        )
    }

    override fun onResume() {
        super.onResume()
        loadMemories()
    }

    override fun onPause() {
        super.onPause()
        // Remove listeners when fragment is not visible to save resources
        removeAllListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeAllListeners()
        binding = null
    }
}
