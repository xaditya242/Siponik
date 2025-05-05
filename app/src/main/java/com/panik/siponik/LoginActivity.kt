package com.panik.siponik

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val googleLoginButton = findViewById<CardView>(R.id.googleSignInButton)
        googleLoginButton.setOnClickListener {
            startActivity(Intent(this, GoogleSignInActivity::class.java))
            finish()
        }

        // Jika pengguna sudah login, langsung ke MainActivity
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val emailField = findViewById<EditText>(R.id.emailInput)
        val passwordField = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<LinearLayout>(R.id.loginButton)
        val signUpButton = findViewById<TextView>(R.id.signupRedirectButton)
        val eyeImage = findViewById<ImageView>(R.id.eyeLoginImage)

        eyeImage.setOnClickListener {
            if (passwordField.transformationMethod is PasswordTransformationMethod) {
                passwordField.transformationMethod = null
                eyeImage.setImageResource(R.drawable.eyeoff)
            } else {
                passwordField.transformationMethod = PasswordTransformationMethod.getInstance()
                eyeImage.setImageResource(R.drawable.eye)
            }
            passwordField.setSelection(passwordField.text.length)
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: ""
                            Log.d("DEBUG", "User ID yang login: $userId")

                            val databaseReference = FirebaseDatabase.getInstance().getReference("Siponik")
                            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (child in snapshot.children) {
                                        val userIdFromDb = child.child("UserInfo/userId").value.toString()
                                        if (userIdFromDb == userId) {
                                            val espId = child.child("UserInfo/ID ESP").value.toString()
                                            val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
                                            sharedPref.edit().putString("espId", espId).apply()
                                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                            finish()
                                            return
                                        }
                                    }
                                    Toast.makeText(this@LoginActivity, "User belum terdaftar di database", Toast.LENGTH_LONG).show()
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(this@LoginActivity, "Database error", Toast.LENGTH_LONG).show()
                                }
                            })
                        } else {
                            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
    }
}

