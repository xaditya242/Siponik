package com.panik.siponik

import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var wifiManager: WifiManager
    private lateinit var idESP: EditText



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

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
                val userDatabase = FirebaseDatabase.getInstance().getReference("Siponik").child(ID_ESP)

                // Langkah 1: Cek apakah ID_ESP sudah terdaftar
                userDatabase.child("UserInfo").get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        // ID ESP sudah ada → gagal
                        Toast.makeText(this, "ID ESP sudah terdaftar. Silakan login.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        // ID ESP belum ada → lanjut daftar
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                                    val dataRef = userDatabase.child("Data")
                                    val userInfoRef = userDatabase.child("UserInfo")

                                    val userData = mapOf(
                                        "email" to email,
                                        "ID ESP" to ID_ESP,
                                        "userId" to userId
                                    )

                                    val monitorData = mapOf(
                                        "Nutrisi" to 0,
                                        "pH" to 0,
                                        "Suhu" to 0,
                                        "KetinggianAir" to 0,
                                        "WifiSSID" to "-"
                                    )

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
                    Toast.makeText(this, "Gagal mengecek ID ESP: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Harap isi data dengan benar!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
