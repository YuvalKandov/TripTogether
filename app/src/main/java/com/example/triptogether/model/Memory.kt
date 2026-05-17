package com.example.triptogether.model

data class Memory private constructor(
    val id: String,
    val activityId: String,
    val tripId: String,
    val text: String,
    val photoUrls: List<String>,
    val userId: String,
    val createdAt: Long
) {
    // No-arg constructor for Firebase deserialization
    constructor(): this(
        id = "",
        activityId = "",
        tripId = "",
        text = "",
        photoUrls = emptyList(),
        userId = "",
        createdAt = 0L
    )

    class Builder(
        var id: String = "",
        var activityId: String = "",
        var tripId: String = "",
        var text: String = "",
        var photoUrls: List<String> = emptyList(),
        var userId: String = "",
        var createdAt: Long = 0L
    ) {
        fun id(id: String) = apply { this.id = id }
        fun activityId(activityId: String) = apply { this.activityId = activityId }
        fun tripId(tripId: String) = apply { this.tripId = tripId }
        fun text(text: String) = apply { this.text = text }
        fun photoUrls(photoUrls: List<String>) = apply { this.photoUrls = photoUrls }
        fun userId(userId: String) = apply { this.userId = userId }
        fun createdAt(createdAt: Long) = apply { this.createdAt = createdAt }

        fun build() = Memory(id, activityId, tripId, text, photoUrls, userId, createdAt)
    }
}
