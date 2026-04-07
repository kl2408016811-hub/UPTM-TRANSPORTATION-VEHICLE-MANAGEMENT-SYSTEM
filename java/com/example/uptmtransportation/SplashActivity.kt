package com.example.uptmtransportation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView  // ← TAMBAH IMPORT INI
import androidx.appcompat.app.AppCompatActivity
import com.example.uptmtransportation.admin.AdminMainActivity  // ← IMPORT ADMIN
import com.example.uptmtransportation.student.StudentMainActivity  // ← IMPORT STUDENT
import com.example.uptmtransportation.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Set logo image
        val ivLogo: ImageView = findViewById(R.id.ivLogo)
        ivLogo.setImageResource(R.drawable.bus)

        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, 2000)
    }

    private fun checkLoginStatus() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            // User already logged in, check role
            FirebaseHelper.getUser { user, success, _ ->
                if (success && user != null) {
                    val intent = if (user.role == "admin") {
                        Intent(this, AdminMainActivity::class.java)
                    } else {
                        Intent(this, StudentMainActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}