package com.example.triptogether.utilities

import android.util.Log
import com.example.triptogether.model.Comment
import com.example.triptogether.model.Like
import com.example.triptogether.model.Memory
import com.example.triptogether.model.Trip
import com.example.triptogether.model.TripActivity
import com.example.triptogether.model.TripMember
import com.example.triptogether.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseManager private constructor() {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        @Volatile
        private var instance: FirebaseManager? = null

        fun init(): FirebaseManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseManager().also { instance = it }
            }
        }

        fun getInstance(): FirebaseManager {
            return instance ?: throw IllegalStateException(
                "FirebaseManager must be initialized by calling init() before use."
            )
        }
    }

    // Get current user ID
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Get current user display name
    fun getCurrentUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }

    // Get current user photo URL
    fun getCurrentUserPhotoUrl(): String? {
        return auth.currentUser?.photoUrl?.toString()
    }

    // Create a new trip
    fun createTrip(
        trip: Trip,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val tripId = trip.id.ifEmpty {
            database.getReference(Constants.DB.TRIPS_REF).push().key ?: return
        }

        val userId = getCurrentUserId() ?: return

        val tripWithId = Trip.Builder()
            .id(tripId)
            .name(trip.name)
            .description(trip.description)
            .coverImageUrl(trip.coverImageUrl)
            .startDate(trip.startDate)
            .endDate(trip.endDate)
            .inviteCode(trip.inviteCode)
            .ownerId(userId)
            .createdAt(trip.createdAt)
            .updatedAt(System.currentTimeMillis())
            .build()

        val tripRef = database.getReference(Constants.DB.TRIPS_REF).child(tripId)

        // Create trip owner member
        val ownerMember = TripMember.Builder()
            .id(userId)
            .displayName(getCurrentUserDisplayName() ?: "")
            .photoUrl(getCurrentUserPhotoUrl() ?: "")
            .role(TripMember.ROLE_OWNER)
            .joinedAt(System.currentTimeMillis())
            .build()

        val updates = hashMapOf<String, Any>(
            "id" to tripWithId.id,
            "name" to tripWithId.name,
            "description" to tripWithId.description,
            "coverImageUrl" to tripWithId.coverImageUrl,
            "startDate" to tripWithId.startDate,
            "endDate" to tripWithId.endDate,
            "inviteCode" to tripWithId.inviteCode,
            "ownerId" to tripWithId.ownerId,
            "createdAt" to tripWithId.createdAt,
            "updatedAt" to tripWithId.updatedAt,
            "members/${userId}" to ownerMember
        )

        tripRef.updateChildren(updates)
            .addOnSuccessListener {
                // Save invite code mapping
                database.getReference(Constants.DB.INVITE_CODES_REF)
                    .child(trip.inviteCode)
                    .setValue(tripId)
                    .addOnSuccessListener { onSuccess(tripId) }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Get all trips for current user
    fun getUserTrips(
        onDataChange: (List<Trip>) -> Unit,
        onError: (DatabaseError) -> Unit
    ): ValueEventListener? {
        val userId = getCurrentUserId() ?: return null

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trips = mutableListOf<Trip>()
                for (tripSnapshot in snapshot.children) {
                    val trip = tripSnapshot.getValue(Trip::class.java)
                    if (trip != null) {
                        // Check if user is a member
                        val isMember = tripSnapshot.child("members").child(userId).exists()
                        if (isMember) {
                            trips.add(trip)
                        }
                    }
                }
                onDataChange(trips)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FirebaseManager", "Failed to read trips.", error.toException())
                onError(error)
            }
        }

        database.getReference(Constants.DB.TRIPS_REF)
            .addValueEventListener(listener)

        return listener
    }

    fun removeUserTripsListener(listener: ValueEventListener) {
        database.getReference(Constants.DB.TRIPS_REF)
            .removeEventListener(listener)
    }

    // Get single trip by ID
    fun getTrip(
        tripId: String,
        onDataChange: (Trip?) -> Unit,
        onError: (DatabaseError) -> Unit
    ) {
        database.getReference(Constants.DB.TRIPS_REF)
            .child(tripId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val trip = snapshot.getValue(Trip::class.java)
                    onDataChange(trip)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("FirebaseManager", "Failed to read trip.", error.toException())
                    onError(error)
                }
            })
    }

    // Update trip
    fun updateTrip(
        trip: Trip,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "id" to trip.id,
            "name" to trip.name,
            "description" to trip.description,
            "coverImageUrl" to trip.coverImageUrl,
            "startDate" to trip.startDate,
            "endDate" to trip.endDate,
            "inviteCode" to trip.inviteCode,
            "updatedAt" to System.currentTimeMillis()
        )

        if (trip.ownerId.isNotEmpty()) {
            updates["ownerId"] = trip.ownerId
        }

        if (trip.createdAt > 0L) {
            updates["createdAt"] = trip.createdAt
        }

        database.getReference(Constants.DB.TRIPS_REF)
            .child(trip.id)
            .updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Delete trip
    fun deleteTrip(
        tripId: String,
        inviteCode: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Delete invite code mapping first
        database.getReference(Constants.DB.INVITE_CODES_REF)
            .child(inviteCode)
            .removeValue()
            .addOnSuccessListener {
                // Delete trip
                database.getReference(Constants.DB.TRIPS_REF)
                    .child(tripId)
                    .removeValue()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Join trip by invite code
    fun joinTripByCode(
        inviteCode: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: return

        // Find trip ID from invite code
        database.getReference(Constants.DB.INVITE_CODES_REF)
            .child(inviteCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tripId = snapshot.getValue(String::class.java)
                    if (tripId != null) {
                        // Check if user is already a member
                        database.getReference(Constants.DB.TRIPS_REF)
                            .child(tripId)
                            .child("members")
                            .child(userId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(memberSnapshot: DataSnapshot) {
                                    if (memberSnapshot.exists()) {
                                        // User is already a member
                                        onFailure(Exception("You are already a member of this trip"))
                                    } else {
                                        // Add user as member
                                        val member = TripMember.Builder()
                                            .id(userId)
                                            .displayName(getCurrentUserDisplayName() ?: "")
                                            .photoUrl(getCurrentUserPhotoUrl() ?: "")
                                            .role(TripMember.ROLE_MEMBER)
                                            .joinedAt(System.currentTimeMillis())
                                            .build()

                                        database.getReference(Constants.DB.TRIPS_REF)
                                            .child(tripId)
                                            .child("members")
                                            .child(userId)
                                            .setValue(member)
                                            .addOnSuccessListener { onSuccess(tripId) }
                                            .addOnFailureListener { onFailure(it) }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.w("FirebaseManager", "Failed to check membership.", error.toException())
                                    onFailure(error.toException())
                                }
                            })
                    } else {
                        onFailure(Exception("Invalid invite code"))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("FirebaseManager", "Failed to find invite code.", error.toException())
                    onFailure(error.toException())
                }
            })
    }

    // Get trip members
    fun getTripMembers(
        tripId: String,
        onDataChange: (List<TripMember>) -> Unit,
        onError: (DatabaseError) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val members = mutableListOf<TripMember>()
                for (memberSnapshot in snapshot.children) {
                    val member = memberSnapshot.getValue(TripMember::class.java)
                    if (member != null) {
                        members.add(member)
                    }
                }
                onDataChange(members)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FirebaseManager", "Failed to read members.", error.toException())
                onError(error)
            }
        }

        database.getReference(Constants.DB.TRIPS_REF)
            .child(tripId)
            .child("members")
            .addValueEventListener(listener)

        return listener
    }

    fun removeTripMembersListener(tripId: String, listener: ValueEventListener) {
        database.getReference(Constants.DB.TRIPS_REF)
            .child(tripId)
            .child("members")
            .removeEventListener(listener)
    }

    // Get member count for a trip (single read, no listener)
    fun getMemberCount(
        tripId: String,
        onSuccess: (Int) -> Unit,
        onError: (DatabaseError) -> Unit
    ) {
        database.getReference(Constants.DB.TRIPS_REF)
            .child(tripId)
            .child("members")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount.toInt()
                    onSuccess(count)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("FirebaseManager", "Failed to get member count.", error.toException())
                    onError(error)
                }
            })
    }

    // Create a new activity
    fun createActivity(
        activity: TripActivity,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val activityId = activity.id.ifEmpty {
            database.getReference(Constants.DB.ACTIVITIES_REF)
                .child(activity.tripId)
                .push().key ?: return
        }

        val userId = getCurrentUserId() ?: return

        val activityWithId = TripActivity.Builder()
            .id(activityId)
            .tripId(activity.tripId)
            .title(activity.title)
            .description(activity.description)
            .date(activity.date)
            .startTime(activity.startTime)
            .endTime(activity.endTime)
            .location(activity.location)
            .tag(activity.tag)
            .order(activity.order)
            .isCompleted(activity.isCompleted)
            .createdBy(userId)
            .createdAt(activity.createdAt)
            .updatedAt(System.currentTimeMillis())
            .build()

        database.getReference(Constants.DB.ACTIVITIES_REF)
            .child(activity.tripId)
            .child(activityId)
            .setValue(activityWithId)
            .addOnSuccessListener { onSuccess(activityId) }
            .addOnFailureListener { onFailure(it) }
    }

    // Get all activities for a trip
    fun getActivitiesForTrip(
        tripId: String,
        onDataChange: (List<TripActivity>) -> Unit,
        onError: (DatabaseError) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activities = mutableListOf<TripActivity>()
                for (activitySnapshot in snapshot.children) {
                    val activity = activitySnapshot.getValue(TripActivity::class.java)
                    if (activity != null) {
                        activities.add(activity)
                    }
                }
                // Sort by date and start time
                activities.sortWith(compareBy({ it.date }, { it.startTime }))
                onDataChange(activities)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FirebaseManager", "Failed to read activities.", error.toException())
                onError(error)
            }
        }

        database.getReference(Constants.DB.ACTIVITIES_REF)
            .child(tripId)
            .addValueEventListener(listener)

        return listener
    }

    fun removeActivitiesForTripListener(tripId: String, listener: ValueEventListener) {
        database.getReference(Constants.DB.ACTIVITIES_REF)
            .child(tripId)
            .removeEventListener(listener)
    }

    fun getActivitiesForTripOnce(
        tripId: String,
        onDataChange: (List<TripActivity>) -> Unit,
        onError: (DatabaseError) -> Unit
    ) {
        database.getReference(Constants.DB.ACTIVITIES_REF)
            .child(tripId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val activities = mutableListOf<TripActivity>()
                    for (activitySnapshot in snapshot.children) {
                        val activity = activitySnapshot.getValue(TripActivity::class.java)
                        if (activity != null) {
                            activities.add(activity)
                        }
                    }
                    activities.sortWith(compareBy({ it.date }, { it.startTime }))
                    onDataChange(activities)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("FirebaseManager", "Failed to read activities.", error.toException())
                    onError(error)
                }
            })
    }

    // Get single activity by ID
    fun getActivity(
        tripId: String,
        activityId: String,
        onDataChange: (TripActivity?) -> Unit,
        onError: (DatabaseError) -> Unit
    ) {
        database.getReference(Constants.DB.ACTIVITIES_REF)
            .child(tripId)
            .child(activityId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val activity = snapshot.getValue(TripActivity::class.java)
                    onDataChange(activity)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("FirebaseManager", "Failed to read activity.", error.toException())
                    onError(error)
                }
            })
    }

    // Update activity
    fun updateActivity(
        activity: TripActivity,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val activityWithUpdatedTime = TripActivity.Builder()
            .id(activity.id)
            .tripId(activity.tripId)
            .title(activity.title)
            .description(activity.description)
            .date(activity.date)
            .startTime(activity.startTime)
            .endTime(activity.endTime)
            .location(activity.location)
            .tag(activity.tag)
            .order(activity.order)
            .isCompleted(activity.isCompleted)
            .createdBy(activity.createdBy)
            .createdAt(activity.createdAt)
            .updatedAt(System.currentTimeMillis())
            .build()

        database.getReference(Constants.DB.ACTIVITIES_REF)
            .child(activity.tripId)
            .child(activity.id)
            .setValue(activityWithUpdatedTime)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Delete activity
    fun deleteActivity(
        tripId: String,
        activityId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        database.getReference(Constants.DB.ACTIVITIES_REF)
            .child(tripId)
            .child(activityId)
            .removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Save or update current user to database
    fun saveCurrentUser(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val firebaseUser = auth.currentUser ?: return

        // Use displayName if available, otherwise use email prefix as fallback
        val email = firebaseUser.email ?: ""
        val displayName = firebaseUser.displayName?.takeIf { it.isNotEmpty() }
            ?: email.substringBefore("@").takeIf { it.isNotEmpty() }
            ?: "User"

        val user = User.Builder()
            .id(firebaseUser.uid)
            .email(email)
            .displayName(displayName)
            .photoUrl(firebaseUser.photoUrl?.toString() ?: "")
            .createdAt(System.currentTimeMillis())
            .build()

        database.getReference(Constants.DB.USERS_REF)
            .child(firebaseUser.uid)
            .setValue(user)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Get user by ID
    fun getUser(
        userId: String,
        onDataChange: (User?) -> Unit
    ) {
        database.getReference(Constants.DB.USERS_REF)
            .child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    onDataChange(user)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("FirebaseManager", "Failed to read user.", error.toException())
                    onDataChange(null)
                }
            })
    }

    // Create a new memory
    fun createMemory(
        memory: Memory,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val memoryId = memory.id.ifEmpty {
            database.getReference(Constants.DB.MEMORIES_REF)
                .child(memory.activityId)
                .push().key ?: return
        }

        val userId = getCurrentUserId() ?: return

        val memoryWithId = Memory.Builder()
            .id(memoryId)
            .activityId(memory.activityId)
            .tripId(memory.tripId)
            .text(memory.text)
            .photoUrls(memory.photoUrls)
            .userId(userId)
            .createdAt(System.currentTimeMillis())
            .build()

        database.getReference(Constants.DB.MEMORIES_REF)
            .child(memory.activityId)
            .child(memoryId)
            .setValue(memoryWithId)
            .addOnSuccessListener { onSuccess(memoryId) }
            .addOnFailureListener { onFailure(it) }
    }

    // Update memory
    fun updateMemory(
        memory: Memory,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        database.getReference(Constants.DB.MEMORIES_REF)
            .child(memory.activityId)
            .child(memory.id)
            .setValue(memory)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Get single memory by ID
    fun getMemory(
        activityId: String,
        memoryId: String,
        onDataChange: (Memory?) -> Unit,
        onError: (DatabaseError) -> Unit
    ) {
        database.getReference(Constants.DB.MEMORIES_REF)
            .child(activityId)
            .child(memoryId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val memory = snapshot.getValue(Memory::class.java)
                    onDataChange(memory)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("FirebaseManager", "Failed to read memory.", error.toException())
                    onError(error)
                }
            })
    }

    // Delete memory
    fun deleteMemory(
        activityId: String,
        memoryId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        database.getReference(Constants.DB.MEMORIES_REF)
            .child(activityId)
            .child(memoryId)
            .removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Get all memories for a trip
    fun getMemoriesForTrip(
        tripId: String,
        onDataChange: (List<Memory>) -> Unit,
        onError: (DatabaseError) -> Unit
    ) {
        database.getReference(Constants.DB.ACTIVITIES_REF)
            .child(tripId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(activitiesSnapshot: DataSnapshot) {
                    val activities = mutableListOf<TripActivity>()
                    for (activitySnapshot in activitiesSnapshot.children) {
                        val activity = activitySnapshot.getValue(TripActivity::class.java)
                        if (activity != null) {
                            activities.add(activity)
                        }
                    }

                    val allMemories = mutableListOf<Memory>()
                    var activitiesProcessed = 0

                    if (activities.isEmpty()) {
                        onDataChange(emptyList())
                        return
                    }

                    activities.forEach { activity ->
                        database.getReference(Constants.DB.MEMORIES_REF)
                            .child(activity.id)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (memorySnapshot in snapshot.children) {
                                        val memory = memorySnapshot.getValue(Memory::class.java)
                                        if (memory != null) {
                                            allMemories.add(memory)
                                        }
                                    }
                                    activitiesProcessed++
                                    if (activitiesProcessed == activities.size) {
                                        // Sort by created date
                                        allMemories.sortByDescending { it.createdAt }
                                        onDataChange(allMemories)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.w("FirebaseManager", "Failed to read memories.", error.toException())
                                    activitiesProcessed++
                                    if (activitiesProcessed == activities.size) {
                                        allMemories.sortByDescending { it.createdAt }
                                        onDataChange(allMemories)
                                    }
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error)
                }
            })
    }

    // ==================== LIKES ====================

    // Toggle like on a memory (like if not liked, unlike if already liked)
    fun toggleLike(
        memoryId: String,
        onSuccess: (Boolean) -> Unit,  // Returns true if liked, false if unliked
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: return

        val likeRef = database.getReference(Constants.DB.LIKES_REF)
            .child(memoryId)
            .child(userId)

        likeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Already liked - remove the like
                    likeRef.removeValue()
                        .addOnSuccessListener { onSuccess(false) }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    // Not liked - add the like
                    val like = Like.Builder()
                        .id(userId)
                        .memoryId(memoryId)
                        .userId(userId)
                        .createdAt(System.currentTimeMillis())
                        .build()
                    likeRef.setValue(like)
                        .addOnSuccessListener { onSuccess(true) }
                        .addOnFailureListener { onFailure(it) }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.toException())
            }
        })
    }

    // Listen to like changes for a memory (real-time) - returns listener for cleanup
    fun listenToLikes(
        memoryId: String,
        onDataChange: (count: Int, isLikedByUser: Boolean) -> Unit
    ): ValueEventListener {
        val userId = getCurrentUserId()

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                val isLikedByUser = userId != null && snapshot.hasChild(userId)
                onDataChange(count, isLikedByUser)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FirebaseManager", "Failed to read likes.", error.toException())
                onDataChange(0, false)
            }
        }

        database.getReference(Constants.DB.LIKES_REF)
            .child(memoryId)
            .addValueEventListener(listener)

        return listener
    }

    // Remove like listener
    fun removeLikeListener(memoryId: String, listener: ValueEventListener) {
        database.getReference(Constants.DB.LIKES_REF)
            .child(memoryId)
            .removeEventListener(listener)
    }

    // Listen to comment count changes (real-time) - returns listener for cleanup
    fun listenToCommentCount(
        memoryId: String,
        onDataChange: (Int) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataChange(snapshot.childrenCount.toInt())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FirebaseManager", "Failed to count comments.", error.toException())
                onDataChange(0)
            }
        }

        database.getReference(Constants.DB.COMMENTS_REF)
            .child(memoryId)
            .addValueEventListener(listener)

        return listener
    }

    // Remove comment count listener
    fun removeCommentCountListener(memoryId: String, listener: ValueEventListener) {
        database.getReference(Constants.DB.COMMENTS_REF)
            .child(memoryId)
            .removeEventListener(listener)
    }

    // ==================== COMMENTS ====================

    // Add a comment to a memory
    fun addComment(
        memoryId: String,
        text: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: return

        val commentRef = database.getReference(Constants.DB.COMMENTS_REF)
            .child(memoryId)
            .push()

        val commentId = commentRef.key ?: return

        val comment = Comment.Builder()
            .id(commentId)
            .memoryId(memoryId)
            .userId(userId)
            .text(text)
            .createdAt(System.currentTimeMillis())
            .build()

        commentRef.setValue(comment)
            .addOnSuccessListener { onSuccess(commentId) }
            .addOnFailureListener { onFailure(it) }
    }

    // Get comments for a memory
    fun getComments(
        memoryId: String,
        onDataChange: (List<Comment>) -> Unit,
        onError: (DatabaseError) -> Unit
    ) {
        database.getReference(Constants.DB.COMMENTS_REF)
            .child(memoryId)
            .orderByChild("createdAt")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val comments = mutableListOf<Comment>()
                    for (commentSnapshot in snapshot.children) {
                        val comment = commentSnapshot.getValue(Comment::class.java)
                        if (comment != null) {
                            comments.add(comment)
                        }
                    }
                    onDataChange(comments)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("FirebaseManager", "Failed to read comments.", error.toException())
                    onError(error)
                }
            })
    }

    // Listen to comments for a memory (real-time) - returns listener for cleanup
    fun listenToComments(
        memoryId: String,
        onDataChange: (List<Comment>) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = mutableListOf<Comment>()
                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    if (comment != null) {
                        comments.add(comment)
                    }
                }
                onDataChange(comments)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FirebaseManager", "Failed to read comments.", error.toException())
                onDataChange(emptyList())
            }
        }

        database.getReference(Constants.DB.COMMENTS_REF)
            .child(memoryId)
            .orderByChild("createdAt")
            .addValueEventListener(listener)

        return listener
    }

    // Remove comment listener
    fun removeCommentListener(memoryId: String, listener: ValueEventListener) {
        database.getReference(Constants.DB.COMMENTS_REF)
            .child(memoryId)
            .removeEventListener(listener)
    }

    // Get memory count for a specific activity (one-shot read)
    fun getMemoryCountForActivity(
        activityId: String,
        onSuccess: (Int) -> Unit
    ) {
        database.getReference(Constants.DB.MEMORIES_REF)
            .child(activityId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onSuccess(snapshot.childrenCount.toInt())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("FirebaseManager", "Failed to get memory count.", error.toException())
                    onSuccess(0)
                }
            })
    }

    // Delete a comment
    fun deleteComment(
        memoryId: String,
        commentId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        database.getReference(Constants.DB.COMMENTS_REF)
            .child(memoryId)
            .child(commentId)
            .removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
