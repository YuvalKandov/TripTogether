package com.example.triptogether.model

data class Comment private constructor(
    val id: String,
    val memoryId: String,
    val userId: String,
    val text: String,
    val createdAt: Long
) {
    // No-arg constructor for Firebase deserialization
    constructor() : this(
        id = "",
        memoryId = "",
        userId = "",
        text = "",
        createdAt = 0L
    )

    class Builder(
        var id: String = "",
        var memoryId: String = "",
        var userId: String = "",
        var text: String = "",
        var createdAt: Long = 0L
    ) {
        fun id(id: String) = apply { this.id = id }
        fun memoryId(memoryId: String) = apply { this.memoryId = memoryId }
        fun userId(userId: String) = apply { this.userId = userId }
        fun text(text: String) = apply { this.text = text }
        fun createdAt(createdAt: Long) = apply { this.createdAt = createdAt }

        fun build() = Comment(id, memoryId, userId, text, createdAt)
    }
}
