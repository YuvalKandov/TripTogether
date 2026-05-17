package com.example.triptogether.model

data class Trip private constructor(
    val id: String,
    val name: String,
    val description: String,
    val coverImageUrl: String,
    val startDate: Long,
    val endDate: Long,
    val inviteCode: String,
    val ownerId: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    // No-arg constructor for Firebase
    constructor(): this(
        id = "",
        name = "",
        description = "",
        coverImageUrl = "",
        startDate = 0L,
        endDate = 0L,
        inviteCode = "",
        ownerId = "",
        createdAt = 0L,
        updatedAt = 0L
    )

    class Builder(
        var id: String = "",
        var name: String = "",
        var description: String = "",
        var coverImageUrl: String = "",
        var startDate: Long = 0L,
        var endDate: Long = 0L,
        var inviteCode: String = "",
        var ownerId: String = "",
        var createdAt: Long = System.currentTimeMillis(),
        var updatedAt: Long = System.currentTimeMillis()
    ) {
        fun id(id: String) = apply { this.id = id }
        fun name(name: String) = apply { this.name = name }
        fun description(description: String) = apply { this.description = description }
        fun coverImageUrl(coverImageUrl: String) = apply { this.coverImageUrl = coverImageUrl }
        fun startDate(startDate: Long) = apply { this.startDate = startDate }
        fun endDate(endDate: Long) = apply { this.endDate = endDate }
        fun inviteCode(inviteCode: String) = apply { this.inviteCode = inviteCode }
        fun ownerId(ownerId: String) = apply { this.ownerId = ownerId }
        fun createdAt(createdAt: Long) = apply { this.createdAt = createdAt }
        fun updatedAt(updatedAt: Long) = apply { this.updatedAt = updatedAt }

        fun build() = Trip(id, name, description, coverImageUrl, startDate, endDate, inviteCode, ownerId, createdAt, updatedAt)
    }
}
