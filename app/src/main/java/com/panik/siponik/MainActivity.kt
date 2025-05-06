package com.panik.siponik

import FirebaseRefreshable
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var activeIndicator: CardView
    private lateinit var base: LinearLayout
    private lateinit var homeIcon: ImageView
    private lateinit var settingsIcon: ImageView
    private lateinit var btNotif: ImageView
    private lateinit var badgeNotifMain: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activeIndicator = findViewById(R.id.activeIndicator)
        homeIcon = findViewById(R.id.homeIcon)
        settingsIcon = findViewById(R.id.settingIcon)
        base = findViewById(R.id.Base)
        btNotif = findViewById(R.id.btn_notif)
        badgeNotifMain = findViewById(R.id.badge_notif)

        val espId = getEspIdFromSession() // Ambil ID ESP dari session
        Log.d("DEBUG", "ID ESP dari session: $espId")

        val notifRef = FirebaseDatabase.getInstance().getReference("Siponik/$espId/Notifikasi")

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(connectivityReceiver, filter)

        var currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        if (currentFragment is FirebaseRefreshable) {
            currentFragment.refreshFirebaseData()
        }

        notifRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()

                if (count > 0) {
                    badgeNotifMain.visibility = View.VISIBLE
                    badgeNotifMain.text = count.toString()
                } else {
                    badgeNotifMain.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Gagal mengambil data notifikasi", Toast.LENGTH_SHORT).show()
            }
        })


        // Load fragment pertama secara default
        if (savedInstanceState == null) {
            currentFragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.container, currentFragment!!)
                .commit()
        }

        homeIcon.setOnClickListener {
            replaceFragment(HomeFragment(), true)
            moveIndicator(true)
        }

        settingsIcon.setOnClickListener {
            replaceFragment(SettingFragment(), false)
            moveIndicator(false)
        }

        btNotif.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
            finish()
        }
    }

    private fun getEspIdFromSession(): String {
        val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPref.getString("espId", "") ?: ""
    }



    private fun moveIndicator(toHome: Boolean) {
        val targetX = if (toHome) 0f else activeIndicator.width.toFloat()

        Log.d("MainActivity", "Moving indicator to X: $toHome")
        homeIcon.setImageDrawable(null)
        settingsIcon.setImageDrawable(null)

        if (toHome) {
            homeIcon.setImageResource(R.drawable.home2)
            settingsIcon.setImageResource(R.drawable.set1)
            homeIcon.isClickable = false
            settingsIcon.isClickable = true
        } else {
            homeIcon.setImageResource(R.drawable.home1)
            settingsIcon.setImageResource(R.drawable.set2)
            homeIcon.isClickable = true
            settingsIcon.isClickable = false
        }
        // Animasi perpindahan
        ObjectAnimator.ofFloat(activeIndicator, "translationX", targetX).apply {
            duration = 50
            start()
        }
    }

    private var currentFragment: Fragment? = null

    private fun replaceFragment(fragment: Fragment, isBackToHome: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()

        if (isBackToHome) {
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
        } else {
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
        }

        transaction.replace(R.id.container, fragment)
        transaction.commit()

        currentFragment = fragment
    }

    override fun onResume() {
        super.onResume()
        // **Panggil update notifikasi saat kembali dari NotificationActivity**
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val espId = getEspIdFromSession()
        val notifRef = FirebaseDatabase.getInstance().getReference("Siponik/$espId/Notifikasi")

        notifRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                if (count > 0) {
                    badgeNotifMain.visibility = View.VISIBLE
                    badgeNotifMain.text = count.toString()
                } else {
                    badgeNotifMain.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Gagal memperbarui notifikasi", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            val isConnected = activeNetwork?.isConnectedOrConnecting == true

            if (isConnected) {
                FirebaseDatabase.getInstance().goOffline()
                FirebaseDatabase.getInstance().goOnline()
                Log.d("FirebaseReconnect", "Internet reconnected, Firebase refreshed.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(connectivityReceiver)
    }
}
