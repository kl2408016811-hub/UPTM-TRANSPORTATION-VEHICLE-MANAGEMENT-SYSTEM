package com.example.uptmtransportation.models

import java.io.Serializable
import java.util.Date

data class Event(
    val id: String = "",
    val title: String = "",
    val date: Date? = null,
    val time: String = "",        // Start time (e.g., "14:00")
    val endTime: String = "",     // End time (e.g., "18:00")
    val location: String = "",
    val description: String = "",
    val createdBy: String = "",
    val createdAt: Date? = Date(),

    // Time slot settings for auto-generation
    val departureHoursBefore: Int = 3,
    val returnHoursAfter: Int = 3,
    val slotIntervalMinutes: Int = 60,
    val departureQuota: Int = 34,
    val returnQuota: Int = 34
) : Serializable