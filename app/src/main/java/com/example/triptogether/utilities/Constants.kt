package com.example.triptogether.utilities

object Constants {
    object DB {
        const val USERS_REF: String = "users"
        const val TRIPS_REF: String = "trips"
        const val ACTIVITIES_REF: String = "activities"
        const val MEMORIES_REF: String = "memories"
        const val INVITE_CODES_REF: String = "inviteCodes"
        const val LIKES_REF: String = "likes"
        const val COMMENTS_REF: String = "comments"
    }

    object Activities {
        const val LOGIN: String = "LOGIN"
        const val MAIN: String = "MAIN"
    }

    object BundleKeys {
        const val TRIP_ID: String = "TRIP_ID"
        const val ACTIVITY_ID: String = "ACTIVITY_ID"
        const val TRIP_DATE: String = "TRIP_DATE"
        const val MEMORY_ID: String = "MEMORY_ID"
        const val SELECTED_DATE: String = "SELECTED_DATE"
        const val START_DATE: String = "START_DATE"
        const val END_DATE: String = "END_DATE"
    }

    object InviteCode {
        const val CODE_LENGTH: Int = 6
        const val CHARSET: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    }

    object Prefs {
        const val PREFS_NAME: String = "trip_together_prefs"
        const val KEY_DARK_MODE: String = "dark_mode"
    }

    object Storage {
        const val TRIP_COVERS_PATH: String = "trip_covers"
    }
}
