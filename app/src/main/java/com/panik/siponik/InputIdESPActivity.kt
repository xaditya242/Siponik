package com.panik.siponik

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class InputIdESPActivity : AppCompatActivity() {

    private lateinit var idEspEditText: EditText
    private lateinit var submitBtn: LinearLayout
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_id_esp)

        auth = FirebaseAuth.getInstance()
        idEspEditText = findViewById(R.id.editTextIDESP)
        submitBtn = findViewById(R.id.buttonSubmitID)

        submitBtn.setOnClickListener {
            val idEsp = idEspEditText.text.toString().trim()

            if (idEsp.isEmpty()) {
                Toast.makeText(this, "Masukkan ID ESP!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbRef = FirebaseDatabase.getInstance().getReference("Siponik").child(idEsp)

            dbRef.child("UserInfo").get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // ID ESP sudah ada → hapus akun yang baru saja daftar
                    deleteGoogleAccount()
                } else {
                    // ID ESP belum ada → lanjut simpan data
                    val user = auth.currentUser
                    if (user != null) {
                        val userInfo = mapOf(
                            "userId" to user.uid,
                            "email" to user.email,
                            "ID ESP" to idEsp
                        )
                        val defaultData = mapOf(
                            "Nutrisi" to 0,
                            "pH" to 0,
                            "Suhu" to 0,
                            "KetinggianAir" to 0
                        )

                        dbRef.child("UserInfo").setValue(userInfo)
                        dbRef.child("Data").setValue(defaultData).addOnCompleteListener {
                            saveEspIdToSession(idEsp)
                            Toast.makeText(this, "Berhasil daftar dengan Google!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal mengecek ID ESP: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteGoogleAccount() {
        val user = auth.currentUser
        if (user != null) {
            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "ID ESP sudah terdaftar.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Gagal menghapus akun: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveEspIdToSession(espId: String) {
        val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
        sharedPref.edit().putString("espId", espId).apply()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}


