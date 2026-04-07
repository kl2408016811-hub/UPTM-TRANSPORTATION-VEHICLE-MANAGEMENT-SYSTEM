package com.example.uptmtransportation

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var cardStudent: CardView
    private lateinit var cardAdmin: CardView
    private lateinit var ivStudentIcon: ImageView
    private lateinit var ivAdminIcon: ImageView
    private lateinit var tvStudent: TextView
    private lateinit var tvAdmin: TextView
    private lateinit var rbStudent: RadioButton
    private lateinit var rbAdmin: RadioButton
    private lateinit var btnSignUp: MaterialButton
    private lateinit var actvGender: AutoCompleteTextView

    private var selectedRole = "student"

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val genderOptions = listOf("Male", "Female")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupToolbar()
        setupGenderSpinner()
        setupRoleSelection()
        setupPasswordValidation()
        setupSignUpButton()
        setupLoginLink()
    }

    private fun initViews() {
        cardStudent = findViewById(R.id.cardStudent)
        cardAdmin = findViewById(R.id.cardAdmin)
        ivStudentIcon = findViewById(R.id.ivStudentIcon)
        ivAdminIcon = findViewById(R.id.ivAdminIcon)
        tvStudent = findViewById(R.id.tvStudent)
        tvAdmin = findViewById(R.id.tvAdmin)
        rbStudent = findViewById(R.id.rbStudent)
        rbAdmin = findViewById(R.id.rbAdmin)
        actvGender = findViewById(R.id.actvGender)
        btnSignUp = findViewById(R.id.btnSignUp)
    }

    private fun setupToolbar() {
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }
    }

    private fun setupLoginLink() {
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupGenderSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            genderOptions
        )
        actvGender.setAdapter(adapter)
        actvGender.threshold = 1

        // Set text color for dropdown items
        actvGender.setOnItemClickListener { _, _, position, _ ->
            val selectedGender = genderOptions[position]
            actvGender.setText(selectedGender, false)
        }
    }

    private fun setupRoleSelection() {
        selectStudent()

        cardStudent.setOnClickListener {
            selectStudent()
        }

        cardAdmin.setOnClickListener {
            selectAdmin()
        }
    }

    private fun selectStudent() {
        selectedRole = "student"
        rbStudent.isChecked = true
        rbAdmin.isChecked = false

        cardStudent.setCardBackgroundColor(Color.parseColor("#FFD1DC"))
        ivStudentIcon.setColorFilter(Color.parseColor("#E31B23"))
        tvStudent.setTextColor(Color.parseColor("#E31B23"))

        cardAdmin.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
        ivAdminIcon.setColorFilter(Color.parseColor("#999999"))
        tvAdmin.setTextColor(Color.parseColor("#999999"))
    }

    private fun selectAdmin() {
        selectedRole = "admin"
        rbStudent.isChecked = false
        rbAdmin.isChecked = true

        cardAdmin.setCardBackgroundColor(Color.parseColor("#B5E2FF"))
        ivAdminIcon.setColorFilter(Color.parseColor("#0033A0"))
        tvAdmin.setTextColor(Color.parseColor("#0033A0"))

        cardStudent.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
        ivStudentIcon.setColorFilter(Color.parseColor("#999999"))
        tvStudent.setTextColor(Color.parseColor("#999999"))
    }

    private fun setupPasswordValidation() {
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val tvCriteriaMinLength = findViewById<TextView>(R.id.tvCriteriaMinLength)
        val tvCriteriaUppercase = findViewById<TextView>(R.id.tvCriteriaUppercase)
        val tvCriteriaLowercase = findViewById<TextView>(R.id.tvCriteriaLowercase)
        val tvCriteriaSymbol = findViewById<TextView>(R.id.tvCriteriaSymbol)

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()

                updateCriteria(tvCriteriaMinLength, password.length >= 6)
                updateCriteria(tvCriteriaUppercase, password.any { it.isUpperCase() })
                updateCriteria(tvCriteriaLowercase, password.any { it.isLowerCase() })
                updateCriteria(tvCriteriaSymbol, password.any { it in listOf('@', '#', '$', '%', '^') })
            }
        })
    }

    private fun updateCriteria(textView: TextView, isValid: Boolean) {
        if (isValid) {
            textView.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            textView.setTextColor(Color.parseColor("#999999"))
        }
    }

    private fun validatePassword(password: String): Boolean {
        return password.length >= 6 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it in listOf('@', '#', '$', '%', '^') }
    }

    private fun setupSignUpButton() {
        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)

        btnSignUp.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            val gender = actvGender.text.toString().trim()

            var isValid = true

            if (fullName.isEmpty()) {
                etFullName.error = "Full name required"
                isValid = false
            }
            if (username.isEmpty()) {
                etUsername.error = "Username required"
                isValid = false
            }
            if (email.isEmpty()) {
                etEmail.error = "Email required"
                isValid = false
            }
            if (phone.isEmpty()) {
                etPhone.error = "Phone number required"
                isValid = false
            }
            if (gender.isEmpty()) {
                actvGender.error = "Please select gender"
                isValid = false
            }
            if (password.isEmpty()) {
                etPassword.error = "Password required"
                isValid = false
            } else if (!validatePassword(password)) {
                etPassword.error = "Password must have: 6+ chars, 1 uppercase, 1 lowercase, 1 symbol (@#$%^)"
                isValid = false
            }
            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                isValid = false
            }

            if (isValid) {
                // Show loading
                btnSignUp.isEnabled = false
                btnSignUp.text = "Creating Account..."

                // Register with Firebase
                registerUser(email, password, fullName, username, phone, gender)
            }
        }
    }

    private fun registerUser(email: String, password: String, fullName: String, username: String, phone: String, gender: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""

                    // Create user object
                    val user = hashMapOf(
                        "id" to userId,
                        "fullName" to fullName,
                        "username" to username,
                        "email" to email,
                        "phone" to phone,
                        "gender" to gender.lowercase(),
                        "role" to selectedRole,
                        "createdAt" to System.currentTimeMillis()
                    )

                    // Save to Firestore
                    db.collection("users").document(userId).set(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                            // Go to login
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error saving user: ${e.message}", Toast.LENGTH_SHORT).show()
                            btnSignUp.isEnabled = true
                            btnSignUp.text = "CREATE ACCOUNT"
                        }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    btnSignUp.isEnabled = true
                    btnSignUp.text = "CREATE ACCOUNT"
                }
            }
    }
}