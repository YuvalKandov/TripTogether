package com.example.triptogether.model

data class TripActivity private constructor(
    val id: String,
    val tripId: String,
    val title: String,
    val description: String,
    val date: Long,  // Day timestamp
    val startTime: String,  // "HH:mm" format
    val endTime: String,
    val location: String,
    val tag: String,  // food|transport|hotel|activity|other
    val order: Int,
    var isCompleted: Boolean = false,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    // No-arg constructor for Firebase deserialization
    constructor() : this(
        id = "",
        tripId = "",
        title = "",
        description = "",
        date = 0L,
        startTime = "",
        endTime = "",
        location = "",
        tag = Tags.OTHER,
        order = 0,
        isCompleted = false,
        createdBy = "",
        createdAt = 0L,
        updatedAt = 0L
    )

    fun toggleCompleted() = apply { isCompleted = !isCompleted }

    object Tags {
        const val FOOD: String = "food"
        const val TRANSPORT: String = "transport"
        const val HOTEL: String = "hotel"
        const val ACTIVITY: String = "activity"
        const val OTHER: String = "other"
    }

    class Builder(
        var id: String = "",
        var tripId: String = "",
        var title: String = "",
        var description: String = "",
        var date: Long = 0L,
        var startTime: String = "",
        var endTime: String = "",
        var location: String = "",
        var tag: String = Tags.OTHER,
        var order: Int = 0,
        var isCompleted: Boolean = false,
        var createdBy: String = "",
        var createdAt: Long = System.currentTimeMillis(),
        var updatedAt: Long = System.currentTimeMillis()
    ) {
        fun id(id: String) = apply { this.id = id }
        fun tripId(tripId: String) = apply { this.tripId = tripId }
        fun title(title: String) = apply { this.title = title }
        fun description(description: String) = apply { this.description = description }
        fun date(date: Long) = apply { this.date = date }
        fun startTime(startTime: String) = apply { this.startTime = startTime }
        fun endTime(endTime: String) = apply { this.endTime = endTime }
        fun location(location: String) = apply { this.location = location }
        fun tag(tag: String) = apply { this.tag = tag }
        fun order(order: Int) = apply { this.order = order }
        fun isCompleted(isCompleted: Boolean) = apply { this.isCompleted = isCompleted }
        fun createdBy(createdBy: String) = apply { this.createdBy = createdBy }
        fun createdAt(createdAt: Long) = apply { this.createdAt = createdAt }
        fun updatedAt(updatedAt: Long) = apply { this.updatedAt = updatedAt }

        fun build() = TripActivity(
            id, tripId, title, description, date, startTime, endTime,
            location, tag, order, isCompleted, createdBy, createdAt, updatedAt
        )
    }
}
