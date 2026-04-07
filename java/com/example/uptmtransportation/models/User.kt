package com.example.uptmtransportation.models

import java.io.Serializable

data class User(
    val id: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val gender: String = "",
    val password: String = "",
    val role: String = "student",
    val profileImage: String = "",
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable