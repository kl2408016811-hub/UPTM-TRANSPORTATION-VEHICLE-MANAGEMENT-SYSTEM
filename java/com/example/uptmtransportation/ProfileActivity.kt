package com.example.uptmtransportation

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.uptmtransportation.models.User
import com.example.uptmtransportation.utils.FirebaseHelper
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvProfileEmoji: TextView
    private lateinit var tvChangePhoto: TextView
    private lateinit var etFullName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var tvRole: TextView
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var loadingDialog: LoadingDialog

    private var isStudent = true
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        loadingDialog = LoadingDialog(this)
        isStudent = intent.getBooleanExtra("isStudent", true)

        initViews()
        setupToolbar()
        loadUserData()
        setupListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvProfileEmoji = findViewById(R.id.tvProfileEmoji)
        tvChangePhoto = findViewById(R.id.tvChangePhoto)
        etFullName = findViewById(R.id.etFullName)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        tvRole = findViewById(R.id.tvRole)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadUserData() {
        loadingDialog.show()

        FirebaseHelper.getUser { user, success, error ->
            loadingDialog.dismiss()
            if (success && user != null) {
                currentUser = user
                displayUserData(user)
            } else {
                Toast.makeText(this, error ?: "Failed to load user", Toast.LENGTH_SHORT).show()
                loadDummyData()
            }
        }
    }

    private fun displayUserData(user: User) {
        tvRole.text = if (user.role == "admin") "Admin" else "Student"

        etFullName.setText(user.fullName)
        etUsername.setText(user.username)
        etEmail.setText(user.email)
        etPhone.setText(user.phone)

        // Set emoji based on gender
        val emoji = when (user.gender.lowercase()) {
            "male" -> "👨"
            "female" -> "👩"
            else -> "👤"
        }
        tvProfileEmoji.text = emoji

        // Set emoji color based on role
        val emojiColor = if (user.role == "admin") R.color.uptm_blue else R.color.uptm_red
        tvProfileEmoji.setTextColor(getColor(emojiColor))
    }

    private fun loadDummyData() {
        tvRole.text = if (isStudent) "Student" else "Admin"

        val emoji = if (isStudent) "👩" else "👨"
        tvProfileEmoji.text = emoji
        tvProfileEmoji.setTextColor(getColor(R.color.uptm_blue))

        etFullName.setText("Ahmad Student")
        etUsername.setText("ahmad_student")
        etEmail.setText("ahmad@student.uptm.edu.my")
        etPhone.setText("012-3456789")
    }

    private fun setupListeners() {
        tvChangePhoto.setOnClickListener {
            Toast.makeText(this, "Photo change feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        if (etFullName.text.toString().trim().isEmpty()) {
            etFullName.error = "Full name required"
            return
        }

        if (etUsername.text.toString().trim().isEmpty()) {
            etUsername.error = "Username required"
            return
        }

        if (etEmail.text.toString().trim().isEmpty()) {
            etEmail.error = "Email required"
            return
        }

        val currentPass = etCurrentPassword.text.toString()
        val newPass = etNewPassword.text.toString()
        val confirmPass = etConfirmPassword.text.toString()

        if (currentPass.isNotEmpty() || newPass.isNotEmpty() || confirmPass.isNotEmpty()) {
            if (currentPass.isEmpty()) {
                etCurrentPassword.error = "Current password required"
                return
            }

            if (newPass.isEmpty()) {
                etNewPassword.error = "New password required"
                return
            }

            if (newPass.length < 6) {
                etNewPassword.error = "Password must be at least 6 characters"
                return
            }

            if (newPass != confirmPass) {
                etConfirmPassword.error = "Passwords do not match"
                return
            }
        }

        updateProfile()
    }

    private fun updateProfile() {
        loadingDialog.show()
        saveProfileToFirestore()
    }

    private fun saveProfileToFirestore() {
        val userId = FirebaseHelper.getCurrentUserId() ?: run {
            loadingDialog.dismiss()
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedUser = User(
            id = userId,
            fullName = etFullName.text.toString().trim(),
            username = etUsername.text.toString().trim(),
            email = etEmail.text.toString().trim(),
            phone = etPhone.text.toString().trim(),
            gender = currentUser?.gender ?: "",
            password = "",
            role = currentUser?.role ?: "student",
            profileImage = currentUser?.profileImage ?: "",
            createdAt = currentUser?.createdAt ?: System.currentTimeMillis()
        )

        FirebaseHelper.updateUser(updatedUser) { success, error ->
            loadingDialog.dismiss()
            if (success) {
                showSuccessDialog()
            } else {
                Toast.makeText(this@ProfileActivity, error ?: "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("Profile updated successfully!")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .show()
    }
}