package com.example.uptmtransportation.student

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.uptmtransportation.LoginActivity
import com.example.uptmtransportation.ProfileActivity
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Booking
import com.example.uptmtransportation.utils.FirebaseHelper
import com.google.firebase.firestore.FirebaseFirestore

class StudentMainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var badgePendingBookings: TextView
    private lateinit var cardCalendar: CardView
    private lateinit var cardVehicleCatalog: CardView
    private lateinit var cardMakeBooking: CardView
    private lateinit var cardMyBooking: CardView
    private lateinit var cardProfile: CardView
    private lateinit var cardLogout: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_main)

        initViews()
        setupListeners()
        loadStudentData()
        loadPendingBookingCount()
        checkInAppNotifications()
    }

    override fun onResume() {
        super.onResume()
        checkInAppNotifications()
        loadPendingBookingCount()
    }

    private fun checkInAppNotifications() {
        val userId = FirebaseHelper.getCurrentUserId() ?: return

        FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result.documents) {
                    val title = doc.getString("title") ?: "Notification"
                    val message = doc.getString("message") ?: ""
                    val type = doc.getString("type") ?: ""

                    // Show in-app popup dialog
                    AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("OK") { _, _ ->
                            doc.reference.update("read", true)
                        }
                        .setNeutralButton("View Booking") { _, _ ->
                            doc.reference.update("read", true)
                            // Navigate to My Booking activity
                            val intent = Intent(this, MyBookingActivity::class.java)
                            intent.putExtra("notification_type", type)
                            startActivity(intent)
                        }
                        .setCancelable(false)
                        .show()
                }
            }
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        badgePendingBookings = findViewById(R.id.badgePendingBookings)
        cardCalendar = findViewById(R.id.cardCalendar)
        cardVehicleCatalog = findViewById(R.id.cardVehicleCatalog)
        cardMakeBooking = findViewById(R.id.cardMakeBooking)
        cardMyBooking = findViewById(R.id.cardMyBooking)
        cardProfile = findViewById(R.id.cardProfile)
        cardLogout = findViewById(R.id.cardLogout)
    }

    private fun setupListeners() {
        cardCalendar.setOnClickListener {
            try {
                startActivity(Intent(this, CalendarActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        cardVehicleCatalog.setOnClickListener {
            try {
                startActivity(Intent(this, VehicleCatalogActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        cardMakeBooking.setOnClickListener {
            try {
                startActivity(Intent(this, MakeBookingActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        cardMyBooking.setOnClickListener {
            try {
                startActivity(Intent(this, MyBookingActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        cardProfile.setOnClickListener {
            try {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("isStudent", true)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        cardLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadStudentData() {
        FirebaseHelper.getUser { user, success, _ ->
            if (success && user != null) {
                val username = user.fullName.split(" ")[0]
                tvWelcome.text = "Welcome back, $username"

                val sharedPref = getSharedPreferences("MyApp", MODE_PRIVATE)
                sharedPref.edit().putString("username", username).apply()
                sharedPref.edit().putString("userId", user.id).apply()
            } else {
                val sharedPref = getSharedPreferences("MyApp", MODE_PRIVATE)
                val username = sharedPref.getString("username", "Student") ?: "Student"
                tvWelcome.text = "Welcome back, $username"
            }
        }
    }

    private fun loadPendingBookingCount() {
        val userId = FirebaseHelper.getCurrentUserId() ?: return

        FirebaseHelper.getUserBookings(userId) { bookings, success, _ ->
            if (success) {
                val pendingCount = bookings.count { it.status == "pending" }
                if (pendingCount > 0) {
                    badgePendingBookings.text = pendingCount.toString()
                    badgePendingBookings.visibility = android.view.View.VISIBLE
                } else {
                    badgePendingBookings.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout_title))
            .setMessage(getString(R.string.logout_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                logout()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun logout() {
        FirebaseHelper.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show()
    }
}
