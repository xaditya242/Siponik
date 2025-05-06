package com.panik.siponik

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvNutrisi: TextView
    private lateinit var tvpH: TextView
    private lateinit var tvTinggi: TextView
    private lateinit var waveView: CardView
    private lateinit var cardView: CardView
    private var nutrisi: Float = 0f
    private var pH: Float = 0f
    private var suhu: Float = 0f
    private var ketinggianAir: Float = 0f
    private lateinit var arcProgress: ArcProgressView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        return view
    }

    fun setCardViewColors (cardView: CardView, @ColorRes topColorRes: Int, @ColorRes bottomColorRes: Int, bottomPercentage: Float){
        val context = cardView.context
        val topColor = ContextCompat.getColor(context, topColorRes)
        val bottomColor = ContextCompat.getColor(context, bottomColorRes)

        // Pastikan persentase valid (0 - 100)
        val bottomPercent = bottomPercentage.coerceIn(0f, 100f)
        val topPercent = 100f - bottomPercent

        // Hapus semua view sebelum menambahkan ulang
        cardView.removeAllViews()

        // Buat ConstraintLayout sebagai container utama
        val container = ConstraintLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Buat View atas
        val topView = View(context).apply {
            id = View.generateViewId()
            setBackgroundColor(topColor)
            visibility = if (topPercent == 0f) View.GONE else View.VISIBLE
        }

        // Buat View bawah
        val bottomView = View(context).apply {
            id = View.generateViewId()
            setBackgroundColor(bottomColor)
            visibility = if (bottomPercent == 0f) View.GONE else View.VISIBLE
        }

        // Tambahkan ke container
        if (topPercent > 0f) {
            container.addView(topView, ConstraintLayout.LayoutParams(0, 0).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToTop = bottomView.id
                matchConstraintPercentHeight = topPercent / 100f
            })
        }

        if (bottomPercent > 0f) {
            container.addView(bottomView, ConstraintLayout.LayoutParams(0, 0).apply {
                topToBottom = if (topPercent > 0f) topView.id else ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                matchConstraintPercentHeight = bottomPercent / 100f
            })
        }

        // Tambahkan container ke CardView
        cardView.addView(container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        tvNutrisi = view.findViewById(R.id.tvNutrisi)
        tvpH = view.findViewById(R.id.tvpH)
        tvTinggi = view.findViewById(R.id.tvTinggi)
        waveView = view.findViewById(R.id.waterLevel)
        cardView = view.findViewById(R.id.cardViewContainer)

        arcProgress = view.findViewById(R.id.arcProgress)

        // Cek apakah pengguna sudah login
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Jika belum login, arahkan ke LoginActivity
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }
        // Referensi database untuk mendengarkan perubahan data secara real-time
        val userId = currentUser.uid
        val databaseReference = FirebaseDatabase.getInstance().getReference("Siponik")

//         Mendengarkan perubahan data secara real-time
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val userIdFromDb = child.child("UserInfo/userId").value.toString()
                    if (userIdFromDb == userId) {
                        val dataNutrisi = child.child("Data/Nutrisi").value.toString()
                        val datapH = child.child("Data/pH").value.toString()
                        val dataSuhu = child.child("Data/Suhu").value.toString()
                        val dataKetinggianAir = child.child("Data/KetinggianAir").value.toString()

                        nutrisi = dataNutrisi.toFloat()
                        pH = datapH.toFloat()
                        suhu = dataSuhu.toFloat()
                        ketinggianAir = dataKetinggianAir.toFloat()

                        setCardViewColors(cardView, R.color.hijau_muda, R.color.hijau_tua, ketinggianAir)

                        tvNutrisi.text = dataNutrisi
                        tvpH.text = datapH
                        tvTinggi.text = " $dataKetinggianAir %"

                        val baseColor = ContextCompat.getColor(requireContext(), R.color.hijau_muda)
                        val progressColor = ContextCompat.getColor(requireContext(), R.color.hijau_tua)
                        val capColor = ContextCompat.getColor(requireContext(), R.color.hijau_tua)
                        var colorText = ContextCompat.getColor(requireContext(), R.color.hijau_muda)

                        arcProgress.apply {
                            baseArcColor = baseColor
                            baseArcWidth = 28f

                            progressArcColor = progressColor
                            progressArcWidth = 15f

                            capCircleColor = capColor
                            capCircleRadius = 25f

                            textColor = colorText
                            textSize = 40f

                            textFont = ResourcesCompat.getFont(requireContext(), R.font.poppins_bold)

                            setMaxValue(50f)
                            setProgressValue(suhu)
                        }
                        break
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                //tvData.text = "Failed to load data: ${error.message}"
            }
        })
    }
}
