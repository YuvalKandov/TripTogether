package com.example.triptogether.interfaces

import com.example.triptogether.model.TripActivity

interface ActivityCallback {
    fun onActivityClicked(activity: TripActivity, position: Int)
    fun onActivityCompletedToggle(activity: TripActivity, position: Int)
}
