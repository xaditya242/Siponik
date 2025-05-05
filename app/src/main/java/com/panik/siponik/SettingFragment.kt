package com.panik.siponik

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
                TODO("Not yet implemented")
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
