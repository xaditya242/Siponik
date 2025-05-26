package com.panik.siponik

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var idESP: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val loadingOverlay = findViewById<FrameLayout>(R.id.loadingOverlay)

        auth = FirebaseAuth.getInstance()

        idESP = findViewById(R.id.espID)
        val etEmail = findViewById<EditText>(R.id.emailSignup)
        val etPassword = findViewById<EditText>(R.id.pwSignup)
        val signUpButton = findViewById<LinearLayout>(R.id.signupButton)
        val loginDirect = findViewById<TextView>(R.id.loginDirect)

        loginDirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val eyeImage = findViewById<ImageView>(R.id.eyeSignupImage)

        eyeImage.setOnClickListener {
            if (etPassword.transformationMethod is android.text.method.PasswordTransformationMethod) {
                // Tampilkan password (hilangkan mask)
                etPassword.transformationMethod = null
                eyeImage.setImageResource(R.drawable.eyeoff) // Gambar mata terbuka
            } else {
                // Sembunyikan password (mask dengan ***)
                etPassword.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                eyeImage.setImageResource(R.drawable.eye) // Gambar mata tertutup
            }
            // Pindahkan cursor ke akhir teks agar pengguna tetap nyaman
            etPassword.setSelection(etPassword.text.length)
        }

        signUpButton.setOnClickListener {
            val ID_ESP = idESP.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && ID_ESP.isNotEmpty()) {
                loadingOverlay.visibility = View.VISIBLE
                loadingOverlay.post {
                    // Delay kecil untuk memastikan overlay terlihat
                    Handler(Looper.getMainLooper()).postDelayed({
                        proceedSignUp(email, password, ID_ESP, loadingOverlay)
                    }, 50)
                }
            } else {
                Toast.makeText(this, "Harap isi data dengan benar!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun proceedSignUp(email: String, password: String, ID_ESP: String, loadingOverlay: FrameLayout) {
        val userDatabase = FirebaseDatabase.getInstance().getReference("Siponik").child(ID_ESP)

        userDatabase.child("UserInfo").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                loadingOverlay.visibility = View.GONE
                Toast.makeText(this, "ID ESP sudah terdaftar. Silakan login.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        loadingOverlay.visibility = View.GONE
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                            val userData = mapOf("email" to email, "ID ESP" to ID_ESP, "userId" to userId)
                            val monitorData = mapOf(
                                "Nutrisi" to 0, "pH" to 0, "SuhuAir" to 0,
                                "SuhuRuang" to 0, "KetinggianAir" to 0, "WifiSSID" to "-"
                            )

                            val dataRef = userDatabase.child("Data")
                            val userInfoRef = userDatabase.child("UserInfo")

                            dataRef.setValue(monitorData)
                            userInfoRef.setValue(userData).addOnCompleteListener {
                                Toast.makeText(this, "Sign Up berhasil!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }

                        } else {
                            Toast.makeText(this, "Sign Up gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }.addOnFailureListener {
            loadingOverlay.visibility = View.GONE
            Toast.makeText(this, "Gagal mengecek ID ESP: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }


    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
