package com.panik.siponik

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val emailField = findViewById<EditText>(R.id.emailField)
        val btnResetPassword = findViewById<LinearLayout>(R.id.btnResetPassword)

        btnResetPassword.setOnClickListener {
            val email = emailField.text.toString().trim()

            if (email.isEmpty()) {
                emailField.error = "Email harus diisi"
                emailField.requestFocus()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.error = "Email tidak valid"
                emailField.requestFocus()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Silahkan Cek email untuk reset password", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val Intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
        startActivity(Intent)
        finish()
    }
}