package com.example.triptogether.model

data class User private constructor(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String,
    val createdAt: Long
) {
    constructor(): this(
        id = "",
        email = "",
        displayName = "",
        photoUrl = "",
        createdAt = 0L
    )

    class Builder(
        var id: String = "",
        var email: String = "",
        var displayName: String = "",
        var photoUrl: String = "",
        var createdAt: Long = System.currentTimeMillis()
    ) {
        fun id(id: String) = apply { this.id = id }
        fun email(email: String) = apply { this.email = email }
        fun displayName(displayName: String) = apply { this.displayName = displayName }
        fun photoUrl(photoUrl: String) = apply { this.photoUrl = photoUrl }
        fun createdAt(createdAt: Long) = apply { this.createdAt = createdAt }

        fun build() = User(id, email, displayName, photoUrl, createdAt)
    }
}
