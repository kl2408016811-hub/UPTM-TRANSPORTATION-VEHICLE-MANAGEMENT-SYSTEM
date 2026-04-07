package com.example.uptmtransportation.utils

import android.net.Uri
import android.util.Log
import com.example.uptmtransportation.models.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.*

object FirebaseHelper {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    const val COLLECTION_USERS = "users"
    const val COLLECTION_VEHICLES = "vehicles"
    const val COLLECTION_BOOKINGS = "bookings"
    const val COLLECTION_EVENTS = "events"
    const val COLLECTION_BUS_SCHEDULES = "bus_schedules"

    const val COLLECTION_EVENT_TIME_SLOTS = "event_time_slots"


    // ==================== AUTH FUNCTIONS ====================

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun getEventTimeSlots(eventId: String, callback: (List<EventTimeSlot>, Boolean, String?) -> Unit) {
        Log.d("FIREBASE", "Getting time slots for eventId: $eventId")

        db.collection(COLLECTION_EVENT_TIME_SLOTS)
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { result ->
                Log.d("FIREBASE", "Query success, documents count: ${result.documents.size}")
                result.documents.forEach { doc ->
                    Log.d("FIREBASE", "Document ID: ${doc.id}, data: ${doc.data}")
                }
                val slots = result.documents.mapNotNull { doc ->
                    doc.toObject(EventTimeSlot::class.java)?.copy(id = doc.id)
                }
                Log.d("FIREBASE", "Parsed slots count: ${slots.size}")
                callback(slots, true, null)
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Query failed: ${e.message}")
                callback(emptyList(), false, e.message)
            }
    }

    fun addEventTimeSlot(slot: EventTimeSlot, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_EVENT_TIME_SLOTS).document(slot.id).set(slot)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun updateEventTimeSlot(slot: EventTimeSlot, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_EVENT_TIME_SLOTS).document(slot.id).set(slot)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun deleteEventTimeSlot(slotId: String, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_EVENT_TIME_SLOTS).document(slotId).delete()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun signUp(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun logout() {
        auth.signOut()
    }

    // ==================== USER FUNCTIONS ====================

    fun createUser(user: User, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUserId() ?: run {
            callback(false, "User not logged in")
            return
        }
        db.collection(COLLECTION_USERS).document(userId).set(user)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun getUser(callback: (User?, Boolean, String?) -> Unit) {
        val userId = getCurrentUserId() ?: run {
            callback(null, false, "User not logged in")
            return
        }
        db.collection(COLLECTION_USERS).document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    callback(user?.copy(id = doc.id), true, null)
                } else {
                    callback(null, false, "User not found")
                }
            }
            .addOnFailureListener { e -> callback(null, false, e.message) }
    }

    fun getUserById(userId: String, callback: (User?, Boolean, String?) -> Unit) {
        db.collection(COLLECTION_USERS).document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    callback(user?.copy(id = doc.id), true, null)
                } else {
                    callback(null, false, "User not found")
                }
            }
            .addOnFailureListener { e -> callback(null, false, e.message) }
    }

    fun updateUser(user: User, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUserId() ?: run {
            callback(false, "User not logged in")
            return
        }
        db.collection(COLLECTION_USERS).document(userId).set(user)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    // ==================== VEHICLE FUNCTIONS ====================

    fun addVehicle(vehicle: Vehicle, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_VEHICLES).document(vehicle.id).set(vehicle)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun getAllVehicles(callback: (List<Vehicle>, Boolean, String?) -> Unit) {
        db.collection(COLLECTION_VEHICLES)
            .get()
            .addOnSuccessListener { result ->
                val vehicles = result.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null

                        val seatLayoutRaw = data["seatLayout"] as? List<*>
                        val seatLayout = seatLayoutRaw?.mapNotNull { item ->
                            when (item) {
                                is Map<*, *> -> {
                                    Seat(
                                        seatNumber = item["seatNumber"] as? String ?: "",
                                        status = item["status"] as? String ?: "available",
                                        bookedBy = item["bookedBy"] as? String ?: "",
                                        gender = item["gender"] as? String ?: ""
                                    )
                                }
                                is String -> Seat(seatNumber = item)
                                else -> null
                            }
                        } ?: listOf<Seat>()

                        Vehicle(
                            id = doc.id,
                            type = data["type"] as? String ?: "",
                            brand = data["brand"] as? String ?: "",
                            plateNumber = data["plateNumber"] as? String ?: "",
                            seatCapacity = (data["seatCapacity"] as? Long)?.toInt() ?: 0,
                            seatLayout = seatLayout,
                            pricePerKm = (data["pricePerKm"] as? Double) ?: (data["pricePerKm"] as? Long)?.toDouble() ?: 0.0,
                            images = (data["images"] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                            isAvailable = data["isAvailable"] as? Boolean ?: true,
                            isActive = data["isActive"] as? Boolean ?: true,
                            createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date()
                        )
                    } catch (e: Exception) {
                        Log.e("FirebaseHelper", "Error parsing vehicle ${doc.id}: ${e.message}")
                        null
                    }
                }
                callback(vehicles, true, null)
            }
            .addOnFailureListener { e -> callback(emptyList(), false, e.message) }
    }

    fun getVehicleById(vehicleId: String, callback: (Vehicle?, Boolean, String?) -> Unit) {
        db.collection(COLLECTION_VEHICLES).document(vehicleId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val vehicle = doc.toObject(Vehicle::class.java)
                    callback(vehicle?.copy(id = doc.id), true, null)
                } else {
                    callback(null, false, "Vehicle not found")
                }
            }
            .addOnFailureListener { e -> callback(null, false, e.message) }
    }

    fun getAvailableVehicles(callback: (List<Vehicle>, Boolean, String?) -> Unit) {
        db.collection(COLLECTION_VEHICLES)
            .whereEqualTo("isAvailable", true)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { result ->
                val vehicles = result.documents.mapNotNull { doc ->
                    doc.toObject(Vehicle::class.java)?.copy(id = doc.id)
                }
                callback(vehicles, true, null)
            }
            .addOnFailureListener { e -> callback(emptyList(), false, e.message) }
    }

    fun updateVehicle(vehicle: Vehicle, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_VEHICLES).document(vehicle.id).set(vehicle)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun deleteVehicle(vehicleId: String, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_VEHICLES).document(vehicleId).delete()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    // ==================== BOOKING FUNCTIONS ====================

    fun createBooking(booking: Booking, callback: (Boolean, String?) -> Unit) {
        Log.d("FirebaseHelper", "Creating booking: ${booking.id}")
        db.collection(COLLECTION_BOOKINGS).document(booking.id).set(booking)
            .addOnSuccessListener {
                Log.d("FirebaseHelper", "Booking created successfully")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error creating booking", e)
                callback(false, e.message)
            }
    }

    fun getUserBookings(userId: String, callback: (List<Booking>, Boolean, String?) -> Unit) {
        Log.d("FirebaseHelper", "Getting bookings for userId: $userId")

        db.collection(COLLECTION_BOOKINGS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val bookings = result.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Booking::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("FirebaseHelper", "Error parsing booking: ${e.message}")
                        null
                    }
                }
                Log.d("FirebaseHelper", "Found ${bookings.size} bookings for user $userId")
                callback(bookings, true, null)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error: ${e.message}")
                callback(emptyList(), false, e.message)
            }
    }

    fun getAllBookings(callback: (List<Booking>, Boolean, String?) -> Unit) {
        Log.d("FirebaseHelper", "Getting all bookings")

        db.collection(COLLECTION_BOOKINGS)
            .get()
            .addOnSuccessListener { result ->
                val bookings = result.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Booking::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("FirebaseHelper", "Error parsing booking: ${e.message}")
                        null
                    }
                }
                Log.d("FirebaseHelper", "Found ${bookings.size} total bookings")
                callback(bookings, true, null)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error: ${e.message}")
                callback(emptyList(), false, e.message)
            }
    }

    fun getBookingsByStatus(status: String, callback: (List<Booking>, Boolean, String?) -> Unit) {
        db.collection(COLLECTION_BOOKINGS)
            .whereEqualTo("status", status)
            .get()
            .addOnSuccessListener { result ->
                val bookings = result.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }
                callback(bookings, true, null)
            }
            .addOnFailureListener { e ->
                callback(emptyList(), false, e.message)
            }
    }

    fun updateBooking(booking: Booking, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_BOOKINGS).document(booking.id).set(booking)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun deleteBooking(bookingId: String, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_BOOKINGS).document(bookingId).delete()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    // ==================== EVENT FUNCTIONS ====================

    fun addEvent(event: Event, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_EVENTS).document(event.id).set(event)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun getAllEvents(callback: (List<Event>, Boolean, String?) -> Unit) {
        db.collection(COLLECTION_EVENTS)
            .orderBy("date", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val events = result.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }
                callback(events, true, null)
            }
            .addOnFailureListener { e -> callback(emptyList(), false, e.message) }
    }

    fun getUpcomingEvents(callback: (List<Event>, Boolean, String?) -> Unit) {
        val currentDate = Date()
        db.collection(COLLECTION_EVENTS)
            .whereGreaterThan("date", currentDate)
            .orderBy("date", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val events = result.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }
                callback(events, true, null)
            }
            .addOnFailureListener { e -> callback(emptyList(), false, e.message) }
    }

    fun updateEvent(event: Event, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_EVENTS).document(event.id).set(event)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun deleteEvent(eventId: String, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_EVENTS).document(eventId).delete()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    // ==================== BUS SCHEDULE FUNCTIONS ====================




    // ==================== UPLOAD FUNCTIONS ====================

    fun uploadVehicleImage(imageUri: Uri, vehicleId: String, callback: (String?, Boolean, String?) -> Unit) {
        val fileName = "vehicles/$vehicleId/${System.currentTimeMillis()}.jpg"
        val ref = storage.child(fileName)
        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString(), true, null)
                }
            }
            .addOnFailureListener { e ->
                callback(null, false, e.message)
            }
    }

    fun uploadProfileImage(imageUri: Uri, userId: String, callback: (String?, Boolean, String?) -> Unit) {
        val fileName = "profiles/$userId/profile.jpg"
        val ref = storage.child(fileName)

        ref.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                Log.d("FirebaseHelper", "Image uploaded successfully for user: $userId")
                ref.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString(), true, null)
                }.addOnFailureListener { e ->
                    Log.e("FirebaseHelper", "Failed to get download URL: ${e.message}")
                    callback(null, false, e.message)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Failed to upload image: ${e.message}")
                callback(null, false, e.message)
            }
    }

    fun saveUserToken(userId: String, token: String, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_USERS).document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    // ==================== DELETE ACCOUNT ====================

    fun deleteAccount(callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        val userId = getCurrentUserId() ?: run {
            callback(false, "User not logged in")
            return
        }

        db.collection(COLLECTION_USERS).document(userId)
            .delete()
            .addOnSuccessListener {
                user?.delete()
                    ?.addOnSuccessListener {
                        callback(true, null)
                    }
                    ?.addOnFailureListener { e ->
                        callback(false, e.message)
                    }
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    // ==================== FORGOT PASSWORD FUNCTIONS ====================

    fun checkEmailExists(email: String, callback: (Boolean, String?) -> Unit) {
        Log.d("FirebaseHelper", "Checking email: $email")

        db.collection(COLLECTION_USERS)
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result ->
                if (result.documents.isNotEmpty()) {
                    Log.d("FirebaseHelper", "Email found")
                    callback(true, null)
                } else {
                    Log.d("FirebaseHelper", "Email not found")
                    callback(false, "Email not registered")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error checking email: ${e.message}")
                callback(false, e.message)
            }
    }

    fun updatePassword(email: String, newPassword: String, callback: (Boolean, String?) -> Unit) {
        Log.d("FirebaseHelper", "Updating password for: $email")

        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.email == email) {
            currentUser.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FirebaseHelper", "Password updated successfully (logged in)")
                        callback(true, null)
                    } else {
                        Log.e("FirebaseHelper", "Update failed: ${task.exception?.message}")
                        callback(false, task.exception?.message)
                    }
                }
        } else {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FirebaseHelper", "Password reset email sent")
                        callback(true, null)
                    } else {
                        Log.e("FirebaseHelper", "Failed to send reset email: ${task.exception?.message}")
                        callback(false, task.exception?.message)
                    }
                }
        }
    }

    // ==================== DATA MIGRATION ====================

    fun fixExistingEventsData() {
        Log.d("FirebaseHelper", "Starting data migration...")

        db.collection(COLLECTION_EVENTS).get().addOnSuccessListener { documents ->
            for (doc in documents) {
                val data = doc.data
                val dateRaw = data["date"]
                if (dateRaw is Long) {
                    db.collection(COLLECTION_EVENTS).document(doc.id)
                        .update("date", Timestamp(Date(dateRaw)))
                }
            }
        }
    }
}