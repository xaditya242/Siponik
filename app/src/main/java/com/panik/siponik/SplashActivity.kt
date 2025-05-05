package com.panik.siponik

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SplashActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)

        if (!isFirstRun) {
            // Jika sudah pernah dibuka, langsung ke MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_splash)

        val startButton: CardView = findViewById(R.id.startButton)
        startButton.setOnClickListener {
            // Simpan preferensi bahwa aplikasi sudah pernah dibuka
            sharedPreferences.edit().putBoolean("isFirstRun", false).apply()

            // Pindah ke MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
