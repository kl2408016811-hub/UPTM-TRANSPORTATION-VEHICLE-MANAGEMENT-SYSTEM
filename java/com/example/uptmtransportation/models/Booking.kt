package com.example.uptmtransportation.models

import java.io.Serializable
import java.util.Date

data class Booking(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userGender: String = "",
    val vehicleId: String = "",
    val vehicleName: String = "",
    val vehicleType: String = "",
    val fromLocation: String = "",
    val toLocation: String = "",
    val distance: Double = 0.0,
    val price: Double = 0.0,
    val bookingDate: Date = Date(),
    val tripDate: Date = Date(),
    val returnDate: Date? = null,
    val status: String = "pending",
    val selectedSeats: List<String> = listOf()
) : Serializable