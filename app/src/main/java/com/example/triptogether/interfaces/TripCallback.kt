package com.example.triptogether.interfaces

import com.example.triptogether.model.Trip

interface TripCallback {
    fun onTripClicked(trip: Trip, position: Int)
}
