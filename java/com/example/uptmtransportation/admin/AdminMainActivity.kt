package com.example.uptmtransportation.admin

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
import com.example.uptmtransportation.utils.FirebaseHelper

class AdminMainActivity : AppCompatActivity() {

    private lateinit var tvAdminName: TextView
    private lateinit var cardManageCalendar: CardView
    private lateinit var cardManageBooking: CardView
    private lateinit var cardAddVehicle: CardView
    private lateinit var cardVehicleCatalog: CardView
    private lateinit var cardProfile: CardView
    private lateinit var cardLogout: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_main)

        initViews()
        setupListeners()
        loadAdminName()
    }

    private fun initViews() {
        tvAdminName = findViewById(R.id.tvAdminName)
        cardManageCalendar = findViewById(R.id.cardManageCalendar)
        cardManageBooking = findViewById(R.id.cardManageBooking)
        cardAddVehicle = findViewById(R.id.cardAddVehicle)
        cardVehicleCatalog = findViewById(R.id.cardVehicleCatalog)
        cardProfile = findViewById(R.id.cardProfile)
        cardLogout = findViewById(R.id.cardLogout)
    }

    private fun setupListeners() {
        cardManageCalendar.setOnClickListener {
            startActivity(Intent(this, ManageCalendarActivity::class.java))
        }

        cardManageBooking.setOnClickListener {
            startActivity(Intent(this, ManageBookingActivity::class.java))
        }

        cardAddVehicle.setOnClickListener {
            startActivity(Intent(this, AddVehicleActivity::class.java))
        }

        cardVehicleCatalog.setOnClickListener {
            startActivity(Intent(this, AdminVehicleCatalogActivity::class.java))
        }

        cardProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("isStudent", false)
            startActivity(intent)
        }

        cardLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadAdminName() {
        FirebaseHelper.getUser { user, success, _ ->
            if (success && user != null) {
                val fullName = user.fullName
                if (fullName.isNotEmpty()) {
                    tvAdminName.text = fullName
                } else {
                    tvAdminName.text = "Admin"
                }
            } else {
                tvAdminName.text = "Admin"
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseHelper.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }
}