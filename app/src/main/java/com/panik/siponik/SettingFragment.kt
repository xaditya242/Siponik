package com.panik.siponik

import FirebaseRefreshable
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
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingFragment : Fragment(), FirebaseRefreshable  {
    private lateinit var auth: FirebaseAuth
    private lateinit var btnLogout: TextView
    private lateinit var btnDelete: TextView

    private lateinit var tvIDESP: TextView
    private lateinit var tvUserID: TextView
    private lateinit var tvSSID: TextView
    private lateinit var dataUser: String
    private lateinit var dataEmail: String
    private lateinit var dataSSID: String

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "AppPrefs"

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val wifiScanResults = mutableListOf<ScanResult>()
    private lateinit var linearWifi: LinearLayout


    private lateinit var progressBarWifi: ProgressBar


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
        progressBarWifi.visibility = View.VISIBLE
        requireActivity().registerReceiver(
            wifiScanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
        wifiManager.startScan()
        Toast.makeText(requireContext(), "Memindai jaringan WiFi...", Toast.LENGTH_SHORT).show()
    }

    private fun scanSuccess() {
        requireActivity().unregisterReceiver(wifiScanReceiver)
        progressBarWifi.visibility = View.GONE
        val results = wifiManager?.scanResults ?: emptyList()
        wifiScanResults.addAll(results)
        val ssids = results.map { it.SSID }
        adapter.addAll(ssids)
        adapter.notifyDataSetChanged()
    }

    private fun scanFailure() {
        requireActivity().unregisterReceiver(wifiScanReceiver)
        progressBarWifi.visibility = View.GONE
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
        btnDelete = view.findViewById(R.id.btnDelete)
        tvIDESP = view.findViewById(R.id.tvIDESP)
        tvUserID = view.findViewById(R.id.tvUserID)
        tvSSID = view.findViewById(R.id.tvSSID)

        linearWifi = view.findViewById(R.id.linearWifi)
        wifiManager = requireActivity().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiList = view.findViewById(R.id.wifiList)

        progressBarWifi = view.findViewById(R.id.progressBarWifi)

        adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_wifi_list,
            R.id.textItem,
            mutableListOf()
        )

        wifiList.adapter = adapter

        changeWifiButton = view.findViewById(R.id.changeWifiButton)
        changeWifiButton.setOnClickListener {
            if (!wifiManager.isWifiEnabled) {
                Toast.makeText(requireContext(), "WiFi tidak aktif. Silakan aktifkan WiFi terlebih dahulu.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

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

        refreshFirebaseData()

        btnDelete.setOnClickListener{
            val dialog = CustomDialogFragment(
                "Hapus Akun",
                "Apakah kamu yakin untuk \nMENGHAPUS AKUN?",
                onContinue = {
                    deleteAccountAndDatabaseNode(this.requireContext(), parentFragmentManager,
                        onSuccess = {
                            Toast.makeText(this.requireContext(), "Akun berhasil dihapus", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this.requireContext(), LoginActivity::class.java))
                            requireActivity().finish()
                        },
                        onFailure = { errorMsg ->
                            Toast.makeText(this.requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onCancel = {

                }
            )
            dialog.show(parentFragmentManager, "CustomDialog")
        }
        // Logout
        btnLogout.setOnClickListener{
            val dialog = CustomDialogFragment(
                "Logout",
                "Apakah kamu yakin untuk Log Out?",
                showInputField = false,
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

    override fun refreshFirebaseData() {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid
        val databaseReference = FirebaseDatabase.getInstance().getReference("Siponik")

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val userIdFromDb = child.child("UserInfo/userId").value.toString()
                    if (userIdFromDb == userId) {
                        val userPath = child.key // Dapatkan key dari user yang sesuai

                        if (userPath != null) {
                            dataUser = child.child("UserInfo/ID ESP").value.toString()
                            dataEmail = child.child("UserInfo/email").value.toString()
                            dataSSID = child.child("Data/WifiSSID").value.toString()
                            tvIDESP.text = dataUser
                            tvUserID.text = dataEmail
                            tvSSID.text = dataSSID
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
    }

    fun deleteAccountAndDatabaseNode(
        context: Context,
        fragmentManager: FragmentManager,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        val database = FirebaseDatabase.getInstance().reference

        if (user == null) {
            onFailure("User tidak ditemukan.")
            return
        }

        // Tampilkan dialog input password dulu
        val dialog = CustomDialogFragment(
            title = "Masukkan Password",
            message = "Masukkan password Anda untuk konfirmasi.",
            showInputField = true,
            onContinue = { password ->

                val credential = EmailAuthProvider.getCredential(user.email ?: "", password.toString()
                )

                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        // Jika reauth sukses, hapus data database terlebih dahulu
                        database.child("Siponik").get().addOnSuccessListener { snapshot ->
                            var found = false

                            for (espSnapshot in snapshot.children) {
                                val userIdInDb = espSnapshot.child("UserInfo/userId").value?.toString()
                                if (userIdInDb == user.uid) {
                                    found = true

                                    espSnapshot.ref.removeValue().addOnCompleteListener { removeTask ->
                                        if (removeTask.isSuccessful) {
                                            // Setelah hapus database, hapus user auth
                                            user.delete()
                                                .addOnSuccessListener { onSuccess() }
                                                .addOnFailureListener { e -> onFailure("Gagal hapus akun: ${e.message}") }
                                        } else {
                                            onFailure("Gagal menghapus data di database.")
                                        }
                                    }
                                    break
                                }
                            }

                            if (!found) {
                                onFailure("Data pengguna tidak ditemukan di database.")
                            }
                        }.addOnFailureListener {
                            onFailure("Gagal membaca database: ${it.message}")
                        }
                    }
                    .addOnFailureListener {
                        onFailure("Re-authentication gagal: ${it.message}")
                    }
            },
            onCancel = {
                onFailure("Penghapusan akun dibatalkan oleh pengguna.")
            }
        )
        dialog.show(fragmentManager, "CustomDialog")
    }



    fun reauthenticateIfNeeded(
        fragmentManager: FragmentManager,
        user: FirebaseUser,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val providerId = user.providerData[1].providerId
        if (providerId == "password") {
            showPasswordInputDialog(fragmentManager) { inputPassword ->
                val credential = EmailAuthProvider.getCredential(user.email ?: "", inputPassword)
                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        user.delete()
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> onFailure("Gagal hapus akun: ${e.message}") }
                    }
                    .addOnFailureListener { e ->
                        onFailure("Re-authentication gagal: ${e.message}")
                    }
            }
        } else {
            user.delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure("Gagal hapus akun: ${e.message}") }
        }
    }


    fun showPasswordInputDialog(fragmentManager: FragmentManager, onPasswordEntered: (String) -> Unit) {
        val dialog = CustomDialogFragment(
            title = "Masukkan Password",
            message = "Silakan masukkan password akun Anda untuk verifikasi.",
            showInputField = true,
            onContinue = { password ->
                if (!password.isNullOrEmpty()) {
                    onPasswordEntered(password)
                } else {
                    Toast.makeText(
                        fragmentManager.fragmentFactory.instantiate(
                            ClassLoader.getSystemClassLoader(),
                            CustomDialogFragment::class.java.name
                        ).requireContext(),
                        "Password tidak boleh kosong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onCancel = {
                // Bisa kosong atau berikan logika lain jika dibutuhkan
            }
        )
        dialog.show(fragmentManager, "PasswordDialog")
    }
}
