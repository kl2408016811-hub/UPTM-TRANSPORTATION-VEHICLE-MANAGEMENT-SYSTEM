package com.example.uptmtransportation.student

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
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

class MyBookingActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var rvBookings: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var loadingDialog: LoadingDialog

    // Stats Views
    private lateinit var tvTotalBookings: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvApprovedCount: TextView
    private lateinit var tvRejectedCount: TextView

    private lateinit var adapter: BookingAdapter
    private val allBookings = mutableListOf<Booking>()
    private var currentUserId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_booking)

        loadingDialog = LoadingDialog(this)
        initViews()
        setupToolbar()
        setupTabLayout()
        setupRecyclerView()

        loadCurrentUser()
    }

    override fun onResume() {
        super.onResume()
        if (currentUserId.isNotEmpty()) {
            loadBookings()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        rvBookings = findViewById(R.id.rvBookings)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        tvTotalBookings = findViewById(R.id.tvTotalBookings)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvApprovedCount = findViewById(R.id.tvApprovedCount)
        tvRejectedCount = findViewById(R.id.tvRejectedCount)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupTabLayout() {
        // Clear any existing tabs first (prevent duplicate)
        tabLayout.removeAllTabs()

        // Add tabs - sesuai dengan TabItem dalam XML
        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("Pending"))
        tabLayout.addTab(tabLayout.newTab().setText("Approved"))
        tabLayout.addTab(tabLayout.newTab().setText("Rejected"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterBookings(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = BookingAdapter(
            bookings = allBookings,
            onItemClick = { booking ->
                // Optional: Show booking details dialog
                showBookingDetails(booking)
            },
            onApprove = { },
            onReject = { },
            onCancel = { booking ->
                cancelBooking(booking)
            },
            isAdmin = false
        )
        rvBookings.layoutManager = LinearLayoutManager(this)
        rvBookings.adapter = adapter
    }

    private fun showBookingDetails(booking: Booking) {
        val message = buildString {
            append("Vehicle: ${booking.vehicleName}\n")
            append("Type: ${booking.vehicleType}\n")
            append("From: ${booking.fromLocation}\n")
            append("To: ${booking.toLocation}\n")
            append("Date: ${android.text.format.DateFormat.format("dd MMM yyyy", booking.tripDate)}\n")
            append("Time: ${android.text.format.DateFormat.format("hh:mm a", booking.tripDate)}\n")
            if (booking.selectedSeats.isNotEmpty()) {
                append("Seats: ${booking.selectedSeats.joinToString()}\n")
            }
            append("Price: RM ${String.format("%.2f", booking.price)}")
        }

        AlertDialog.Builder(this)
            .setTitle("Booking Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadCurrentUser() {
        FirebaseHelper.getUser { user, success, _ ->
            if (success && user != null) {
                currentUserId = user.id
                Log.d("MyBooking", "Current user loaded: ${user.fullName}")
                loadBookings()
            } else {
                Log.e("MyBooking", "Failed to load current user")
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadBookings() {
        if (currentUserId.isEmpty()) return

        loadingDialog.show()
        layoutEmpty.visibility = View.GONE
        rvBookings.visibility = View.GONE

        Log.d("MyBooking", "Loading bookings for user: $currentUserId")

        FirebaseHelper.getUserBookings(currentUserId) { bookings, success, error ->
            loadingDialog.dismiss()

            if (success) {
                allBookings.clear()
                allBookings.addAll(bookings)
                allBookings.sortByDescending { it.bookingDate }

                updateStats()
                updateUI()

                Log.d("MyBooking", "Loaded ${bookings.size} bookings")
            } else {
                Log.e("MyBooking", "Error loading bookings: $error")
                Toast.makeText(this, error ?: "Failed to load bookings", Toast.LENGTH_SHORT).show()
                updateUI()
            }
        }
    }

    private fun updateStats() {
        val total = allBookings.size
        val pending = allBookings.count { it.status == "pending" }
        val approved = allBookings.count { it.status == "confirmed" || it.status == "approved" }
        val rejected = allBookings.count { it.status == "cancelled" || it.status == "rejected" }

        tvTotalBookings.text = total.toString()
        tvPendingCount.text = pending.toString()
        tvApprovedCount.text = approved.toString()
        tvRejectedCount.text = rejected.toString()
    }

    private fun filterBookings(position: Int) {
        val filtered = when (position) {
            1 -> allBookings.filter { it.status == "pending" }
            2 -> allBookings.filter { it.status == "confirmed" || it.status == "approved" }
            3 -> allBookings.filter { it.status == "cancelled" || it.status == "rejected" }
            else -> allBookings
        }
        adapter.updateList(filtered)

        if (filtered.isEmpty()) {
            rvBookings.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvBookings.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun updateUI() {
        val currentTab = tabLayout.selectedTabPosition
        filterBookings(currentTab)
    }

    private fun cancelBooking(booking: Booking) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setPositiveButton("YES") { _, _ ->
                loadingDialog.show()
                val updatedBooking = booking.copy(status = "cancelled")
                FirebaseHelper.updateBooking(updatedBooking) { success, error ->
                    loadingDialog.dismiss()
                    if (success) {
                        Toast.makeText(this, "Booking cancelled", Toast.LENGTH_SHORT).show()
                        loadBookings()
                    } else {
                        Toast.makeText(this, error ?: "Failed to cancel", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("NO", null)
            .show()
    }
}