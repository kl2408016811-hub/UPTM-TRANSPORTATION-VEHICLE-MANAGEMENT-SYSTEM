package com.example.uptmtransportation

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.uptmtransportation.utils.FirebaseHelper
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var tilPassword: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loadingDialog = LoadingDialog(this)
        initViews()
        setupListeners()
        setupPasswordToggle()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        tilPassword = findViewById(R.id.tilPassword)
    }

    private fun setupPasswordToggle() {
        // Set end icon mode to password toggle
        tilPassword.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        // Set icon color to dark
        tilPassword.setEndIconDrawable(ContextCompat.getDrawable(this, R.drawable.ic_eye))
        tilPassword.setEndIconTintList(ContextCompat.getColorStateList(this, R.color.black))
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Please enter email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Please enter password"
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        loadingDialog.show()

        FirebaseHelper.login(email, password) { success, error ->
            loadingDialog.dismiss()

            if (success) {
                // Check user role
                FirebaseHelper.getUser { user, userSuccess, _ ->
                    if (userSuccess && user != null) {
                        val intent = if (user.role == "admin") {
                            Intent(this, com.example.uptmtransportation.admin.AdminMainActivity::class.java)
                        } else {
                            Intent(this, com.example.uptmtransportation.student.StudentMainActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to get user data", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, error ?: "Login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}