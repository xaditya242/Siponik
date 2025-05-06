package com.panik.siponik

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.Manifest
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var btnLogout: TextView

    private lateinit var tvIDESP: TextView
    private lateinit var tvUserID: TextView

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "AppPrefs"

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val wifiScanResults = mutableListOf<ScanResult>()
    private lateinit var linearWifi: LinearLayout

    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private lateinit var changeWifiButton: CardView

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                scanSuccess()
            } else {
                scanFailure()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startWifiScan()
                } else {
                    Toast.makeText(requireContext(), "Izin lokasi diperlukan untuk memindai WiFi", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun startWifiScan() {
        adapter.clear()
        wifiScanResults.clear()
        requireActivity().registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
        Toast.makeText(requireContext(), "Memindai jaringan WiFi...", Toast.LENGTH_SHORT).show()
    }

    private fun scanSuccess() {
        requireActivity().unregisterReceiver(wifiScanReceiver)
        val results = wifiManager?.scanResults ?: emptyList()
        wifiScanResults.addAll(results)
        val ssids = results.map { it.SSID }
        adapter.addAll(ssids)
        adapter.notifyDataSetChanged()
    }

    private fun scanFailure() {
        requireActivity().unregisterReceiver(wifiScanReceiver)
        Toast.makeText(requireContext(), "Gagal memindai jaringan WiFi", Toast.LENGTH_SHORT).show()
    }

    private fun connectToESP8266AP(ssid: String) {
        val intent = Intent(requireContext(), WifiFormActivity::class.java)
        intent.putExtra("esp_ssid", ssid)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Penting untuk unregister receiver saat view dihancurkan untuk menghindari memory leaks
        try {
            requireActivity().unregisterReceiver(wifiScanReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver mungkin belum terdaftar jika pemindaian tidak pernah dimulai
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        btnLogout = view.findViewById(R.id.btnLogout)
        tvIDESP = view.findViewById(R.id.tvIDESP)
        tvUserID = view.findViewById(R.id.tvUserID)

        linearWifi = view.findViewById(R.id.linearWifi)
//        linearWifi.visibility = View.INVISIBLE
        wifiManager = requireActivity().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiList = view.findViewById(R.id.wifiList)


        adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_wifi_list,
            R.id.textItem,
            mutableListOf()
        )

        wifiList.adapter = adapter

        changeWifiButton = view.findViewById(R.id.changeWifiButton)
        changeWifiButton.setOnClickListener {
            linearWifi.visibility = View.VISIBLE
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            } else {
                startWifiScan()
            }
        }

        wifiList.setOnItemClickListener { _, _, position, _ ->
            val selectedSSID = wifiScanResults[position].SSID
            connectToESP8266AP(selectedSSID)
        }

        // Inisialisasi SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val currentUser = auth.currentUser
        // Referensi database untuk mendengarkan perubahan data secara real-time
        val userId = currentUser?.uid
        val databaseReference = FirebaseDatabase.getInstance().getReference("Siponik")

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val userIdFromDb = child.child("UserInfo/userId").value.toString()
                    if (userIdFromDb == userId) {
                        val userPath = child.key // Dapatkan key dari user yang sesuai

                        if (userPath != null) {
                            val dataUser = child.child("UserInfo/ID ESP").value.toString()
                            val dataEmail = child.child("UserInfo/email").value.toString()
                            tvIDESP.text = dataUser
                            tvUserID.text = dataEmail
                        }
                        break // Hentikan loop setelah menemukan user yang cocok
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("FirebaseError", "onCancelled: ${error.message}")


            }
        })
        // Logout
        btnLogout.setOnClickListener{
            val dialog = CustomDialogFragment(
                "Alert",
                "Are you sure you want to Log Out?",
                onContinue = {
                    auth.signOut()

                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken("352423757621-mjk9iunpmiiq4iq80v2t29160d2t8ip7.apps.googleusercontent.com")
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

                    googleSignInClient.signOut().addOnCompleteListener {
                        // 3. Hapus sesi lokal jika ada
                        val sharedPref = requireContext().getSharedPreferences("UserSession", AppCompatActivity.MODE_PRIVATE)
                        sharedPref.edit().clear().apply()

                        // 4. Arahkan ke login screen
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                },
                onCancel = {

                }
            )
            dialog.show(parentFragmentManager, "CustomDialog")
//
        }
    }
}
