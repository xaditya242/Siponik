package com.panik.siponik

import NotificationModel
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class NotificationActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private lateinit var notifList: MutableList<NotificationModel>
    private lateinit var database: DatabaseReference
    private lateinit var btnClear: LinearLayout
    private lateinit var back: ImageView
    private lateinit var badgeNotif: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        recyclerView = findViewById(R.id.recyclerView)
        btnClear = findViewById(R.id.clearButton)
        notifList = mutableListOf()
        adapter = NotificationAdapter(notifList)
        back = findViewById(R.id.backBt)
        badgeNotif = findViewById(R.id.badge_notif)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val espId = getEspIdFromSession()
        Log.d("DEBUG", "ID ESP dari session: $espId")

        database = FirebaseDatabase.getInstance().getReference("Siponik/$espId/Notifikasi")

        // Ambil data dari Firebase
        database.orderByKey().addValueEventListener(object : ValueEventListener {
            var previousCount = 0
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentCount = snapshot.childrenCount.toInt()
                notifList.clear()

                val reversedChildren = snapshot.children.reversed()
                for (notifSnapshot in reversedChildren) {
                    val notif = notifSnapshot.getValue(NotificationModel::class.java)
                    notif?.let {
                        notifList.add(it)
                    }
                }

                adapter.notifyDataSetChanged()
                updateBadge()

                // Jika jumlah notifikasi bertambah, tampilkan notifikasi di status bar
                if (notifList.isNotEmpty()) {
                    val latestSnapshot = snapshot.children.last() // ambil key terakhir (yang baru)
                    val latestKey = latestSnapshot.key

                    val lastShownKey = getLastNotifiedKey()
                    if (latestKey != null && latestKey != lastShownKey) {
                        val latestNotif = latestSnapshot.getValue(NotificationModel::class.java)
                        latestNotif?.let {
                            showNotification(it.title ?: "Notifikasi", it.message ?: "")
                            setLastNotifiedKey(latestKey)
                        }
                    }
                }
                previousCount = currentCount // Simpan count sekarang untuk perbandingan berikutnya
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@NotificationActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // Hapus semua notifikasi
        btnClear.setOnClickListener {
            database.removeValue().addOnSuccessListener {
                Toast.makeText(this, "Semua notifikasi dibersihkan", Toast.LENGTH_SHORT).show()
                notifList.clear()
                adapter.notifyDataSetChanged()
                updateBadge()
            }
        }

        back.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    private fun getLastNotifiedKey(): String? {
        val pref = getSharedPreferences("LastNotif", MODE_PRIVATE)
        return pref.getString("last_key", null)
    }

    private fun setLastNotifiedKey(key: String) {
        val pref = getSharedPreferences("LastNotif", MODE_PRIVATE)
        pref.edit().putString("last_key", key).apply()
    }

    private fun updateBadge() {
        // Hitung jumlah notifikasi yang belum dibaca (isRead = false)
        val unreadCount = notifList.count { !it.isRead }

        if (unreadCount > 0) {
            badgeNotif.visibility = View.VISIBLE
            badgeNotif.text = unreadCount.toString()
        } else {
            badgeNotif.visibility = View.GONE
        }

        // Simpan jumlah unread ke shared preferences untuk digunakan MainActivity
        val pref = getSharedPreferences("NotificationCount", MODE_PRIVATE)
        pref.edit().putInt("unread_count", unreadCount).apply()
    }

    private fun markAllAsRead() {
        database.get().addOnSuccessListener { snapshot ->
            for (notifSnapshot in snapshot.children) {
                notifSnapshot.ref.child("isRead").setValue(true)
            }

            // Update local model after marking all as read
            for (notif in notifList) {
                notif.isRead = true
            }

            adapter.notifyDataSetChanged()
            updateBadge()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal memperbarui status notifikasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getEspIdFromSession(): String {
        val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPref.getString("espId", "") ?: ""
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "notif_channel_siponik"
        val notificationId = System.currentTimeMillis().toInt()

        // Intent ke NotificationActivity saat notifikasi diklik
        val intent = Intent(this, NotificationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Buat Notification Channel untuk Android 8.0+ (Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Siponik Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi dari sistem Siponik"
            }

            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Buat notifikasi
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Ganti dengan ikon aplikasi
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Tampilkan notifikasi
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    override fun onStart() {
        super.onStart()
        markAllAsRead()
    }

}