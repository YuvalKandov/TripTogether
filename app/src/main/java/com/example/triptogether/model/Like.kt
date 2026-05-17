package com.example.triptogether.model

data class Like private constructor(
    val id: String,
    val memoryId: String,
    val userId: String,
    val createdAt: Long
) {
    // No-arg constructor for Firebase deserialization
    constructor() : this(
        id = "",
        memoryId = "",
        userId = "",
        createdAt = 0L
    )

    class Builder(
        var id: String = "",
        var memoryId: String = "",
        var userId: String = "",
        var createdAt: Long = 0L
    ) {
        fun id(id: String) = apply { this.id = id }
        fun memoryId(memoryId: String) = apply { this.memoryId = memoryId }
        fun userId(userId: String) = apply { this.userId = userId }
        fun createdAt(createdAt: Long) = apply { this.createdAt = createdAt }

        fun build() = Like(id, memoryId, userId, createdAt)
    }
}
