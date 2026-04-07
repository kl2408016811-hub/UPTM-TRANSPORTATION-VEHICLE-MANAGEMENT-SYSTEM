package com.example.uptmtransportation.models

import java.io.Serializable
import java.util.Date

data class EventTimeSlot(
    val id: String = "",
    val eventId: String = "",           // Event ID
    val type: String = "departure",     // "departure" or "return"
    val time: String = "",              // e.g., "09:00", "17:00"
    val quota: Int = 40,                // Max seats (follow vehicle capacity)
    val bookedSeats: List<String> = listOf(),  // List of seat numbers booked
    val bookedCount: Int = 0,           // Total booked (calculated from bookedSeats.size)
    val isAvailable: Boolean = true,
    val createdAt: Date = Date()
) : Serializable {

    // Helper function to check if slot is full
    fun isFull(): Boolean = bookedSeats.size >= quota

    // Helper to get available seats count
    fun availableSeats(): Int = quota - bookedSeats.size

    // Helper to add seats to this slot
    fun addSeats(seatNumbers: List<String>): EventTimeSlot {
        val newBookedSeats = bookedSeats.toMutableList()
        newBookedSeats.addAll(seatNumbers)
        return this.copy(
            bookedSeats = newBookedSeats,
            bookedCount = newBookedSeats.size,
            isAvailable = newBookedSeats.size < quota
        )
    }

    // Helper to check if seats are available
    fun hasAvailableSeats(seatCount: Int): Boolean = (quota - bookedSeats.size) >= seatCount
}