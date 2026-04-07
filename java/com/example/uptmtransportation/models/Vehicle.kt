package com.example.uptmtransportation.models

import java.io.Serializable
import java.util.Date

data class Vehicle(
    val id: String = "",
    val type: String = "",
    val brand: String = "",
    val plateNumber: String = "",
    val seatCapacity: Int = 0,
    val seatLayout: List<Seat> = listOf(),
    val pricePerKm: Double = 0.0,
    val images: List<String> = listOf(),
    val isAvailable: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Date? = Date()
) : Serializable {

    fun getMainImage(): String = images.firstOrNull() ?: ""
}

data class Seat(
    val seatNumber: String = "",
    val status: String = "available",
    val bookedBy: String = "",
    val gender: String = ""
) : Serializable