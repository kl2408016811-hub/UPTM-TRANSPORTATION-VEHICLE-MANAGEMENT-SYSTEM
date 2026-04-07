package com.example.uptmtransportation.admin

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uptmtransportation.R
import com.example.uptmtransportation.adapters.BookingAdapter
import com.example.uptmtransportation.models.Booking
import com.example.uptmtransportation.utils.FirebaseHelper
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore

class ManageBookingActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var rvBookings: RecyclerView
    private lateinit var adapter: BookingAdapter
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var tvTotalBookings: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvApprovedCount: TextView
    private lateinit var tvRejectedCount: TextView
    private lateinit var layoutEmpty: LinearLayout

    private val bookingList = mutableListOf<Booking>()
    private var currentFilter = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_booking)

        try {
            initViews()
            setupToolbar()

            loadingDialog = LoadingDialog(this)
            loadBookingsFromFirebase()
        } catch (e: Exception) {
            Log.e("ManageBooking", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        rvBookings = findViewById(R.id.rvBookings)
        tvTotalBookings = findViewById(R.id.tvTotalBookings)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvApprovedCount = findViewById(R.id.tvApprovedCount)
        tvRejectedCount = findViewById(R.id.tvRejectedCount)
        layoutEmpty = findViewById(R.id.layoutEmpty)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Bookings"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadBookingsFromFirebase() {
        if (!loadingDialog.isShowing()) loadingDialog.show()

        FirebaseHelper.getAllBookings { bookings, success, error ->
            loadingDialog.dismiss()

            if (success) {
                bookingList.clear()
                bookingList.addAll(bookings)
                updateStats()
                setupRecyclerView()
                setupTabs()
                if (bookingList.isEmpty()) showEmptyState() else hideEmptyState()
            } else {
                Toast.makeText(this, error ?: "Failed to load bookings", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
        }
    }

    private fun updateStats() {
        tvTotalBookings.text = bookingList.size.toString()
        tvPendingCount.text = bookingList.count { it.status == "pending" }.toString()
        tvApprovedCount.text = bookingList.count { it.status == "approved" }.toString()
        tvRejectedCount.text = bookingList.count { it.status == "rejected" }.toString()
    }

    private fun showEmptyState() {
        layoutEmpty.visibility = android.view.View.VISIBLE
        rvBookings.visibility = android.view.View.GONE
    }

    private fun hideEmptyState() {
        layoutEmpty.visibility = android.view.View.GONE
        rvBookings.visibility = android.view.View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = BookingAdapter(
            bookings = filterBookings(currentFilter),
            onItemClick = { booking -> 
                // Could show detailed view here if needed
            },
            onApprove = { booking -> approveBooking(booking) },
            onReject = { booking -> rejectBooking(booking) },
            onCancel = { booking -> cancelBooking(booking) },
            isAdmin = true
        )
        rvBookings.layoutManager = LinearLayoutManager(this)
        rvBookings.adapter = adapter
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> "pending"
                    1 -> "approved"
                    2 -> "rejected"
                    else -> "all"
                }
                val filteredList = filterBookings(currentFilter)
                adapter.updateList(filteredList)
                if (filteredList.isEmpty()) showEmptyState() else hideEmptyState()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun filterBookings(status: String): List<Booking> =
        if (status == "all") bookingList else bookingList.filter { it.status == status }

    private fun approveBooking(booking: Booking) {
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Approve this booking for ${booking.userName}?")
            .setPositiveButton("Yes") { _, _ ->
                loadingDialog.show()

                // Pass user gender to updateSeatStatus
                updateSeatStatus(booking.vehicleId, booking.selectedSeats, "booked", booking.userGender)

                val updatedBooking = booking.copy(status = "approved")
                FirebaseHelper.updateBooking(updatedBooking) { success, error ->
                    loadingDialog.dismiss()
                    if (success) {
                        val index = bookingList.indexOfFirst { it.id == booking.id }
                        if (index != -1) {
                            bookingList[index] = updatedBooking
                            adapter.updateList(filterBookings(currentFilter))
                            updateStats()
                            saveNotification(booking.userId, updatedBooking.vehicleName, "approved", booking.id)
                            Toast.makeText(this, "Booking approved", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, error ?: "Failed to approve", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun rejectBooking(booking: Booking) {
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Reject this booking for ${booking.userName}?")
            .setPositiveButton("Yes") { _, _ ->
                loadingDialog.show()

                updateSeatStatus(booking.vehicleId, booking.selectedSeats, "available", "")

                val updatedBooking = booking.copy(status = "rejected")
                FirebaseHelper.updateBooking(updatedBooking) { success, error ->
                    loadingDialog.dismiss()
                    if (success) {
                        val index = bookingList.indexOfFirst { it.id == booking.id }
                        if (index != -1) {
                            bookingList[index] = updatedBooking
                            adapter.updateList(filterBookings(currentFilter))
                            updateStats()
                            saveNotification(booking.userId, updatedBooking.vehicleName, "rejected", booking.id)
                            Toast.makeText(this, "Booking rejected", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, error ?: "Failed to reject", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelBooking(booking: Booking) {
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Cancel this booking for ${booking.userName}?")
            .setPositiveButton("Yes") { _, _ ->
                loadingDialog.show()

                updateSeatStatus(booking.vehicleId, booking.selectedSeats, "available", "")

                val updatedBooking = booking.copy(status = "cancelled")
                FirebaseHelper.updateBooking(updatedBooking) { success, error ->
                    loadingDialog.dismiss()
                    if (success) {
                        val index = bookingList.indexOfFirst { it.id == booking.id }
                        if (index != -1) {
                            bookingList[index] = updatedBooking
                            adapter.updateList(filterBookings(currentFilter))
                            updateStats()
                            saveNotification(booking.userId, updatedBooking.vehicleName, "cancelled", booking.id)
                            Toast.makeText(this, "Booking cancelled", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, error ?: "Failed to cancel", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateSeatStatus(vehicleId: String, seatNumbers: List<String>, newStatus: String, gender: String) {
        if (seatNumbers.isEmpty()) return

        FirebaseHelper.getVehicleById(vehicleId) { vehicle, success, _ ->
            if (success && vehicle != null) {
                val updatedSeats = vehicle.seatLayout.map { seat ->
                    if (seatNumbers.contains(seat.seatNumber)) {
                        seat.copy(status = newStatus, gender = gender)
                    } else {
                        seat
                    }
                }
                FirebaseHelper.updateVehicle(vehicle.copy(seatLayout = updatedSeats)) { _, _ -> }
            }
        }
    }

    private fun saveNotification(userId: String, vehicleName: String, type: String, bookingId: String) {
        val title = when(type) {
            "approved" -> "Booking Approved! 🎉"
            "rejected" -> "Booking Rejected ❌"
            "cancelled" -> "Booking Cancelled"
            else -> "Booking Updated"
        }
        val message = when(type) {
            "approved" -> "Your booking for $vehicleName has been approved."
            "rejected" -> "Your booking for $vehicleName has been rejected."
            "cancelled" -> "Your booking for $vehicleName has been cancelled by admin."
            else -> "Your booking status has been updated."
        }

        val notification = hashMapOf(
            "userId" to userId,
            "title" to title,
            "message" to message,
            "type" to type,
            "bookingId" to bookingId,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )

        FirebaseFirestore.getInstance().collection("notifications").add(notification)
    }
}
