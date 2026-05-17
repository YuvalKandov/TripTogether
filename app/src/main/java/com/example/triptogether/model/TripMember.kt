package com.example.triptogether.model

data class TripMember private constructor(
    val id: String,
    val displayName: String,
    val photoUrl: String,
    val role: String,
    val joinedAt: Long
) {
    // No-arg constructor for Firebase
    constructor(): this(
        id = "",
        displayName = "",
        photoUrl = "",
        role = "member",
        joinedAt = 0L
    )

    class Builder(
        var id: String = "",
        var displayName: String = "",
        var photoUrl: String = "",
        var role: String = "member",
        var joinedAt: Long = System.currentTimeMillis()
    ) {
        fun id(id: String) = apply { this.id = id }
        fun displayName(displayName: String) = apply { this.displayName = displayName }
        fun photoUrl(photoUrl: String) = apply { this.photoUrl = photoUrl }
        fun role(role: String) = apply { this.role = role }
        fun joinedAt(joinedAt: Long) = apply { this.joinedAt = joinedAt }

        fun build() = TripMember(id, displayName, photoUrl, role, joinedAt)
    }

    companion object {
        const val ROLE_OWNER: String = "owner"
        const val ROLE_MEMBER: String = "member"
    }
}
