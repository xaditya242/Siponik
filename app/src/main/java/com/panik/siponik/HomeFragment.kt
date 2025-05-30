package com.panik.siponik

import FirebaseRefreshable
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment(), FirebaseRefreshable  {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvNutrisi: TextView
    private lateinit var tvpH: TextView
    private lateinit var tvTinggi: TextView
    private lateinit var waveView: CardView
    private lateinit var cardView: CardView
    private var nutrisi: Float = 0f
    private var pH: Float = 0f
    private var suhuRuangValue: Float = 0f
    private var suhuAirValue: Float = 0f
    private var ketinggianAir: Float = 0f
    private lateinit var arcProgress: ArcProgressView

    private lateinit var suhuAir: CardView
    private lateinit var suhuRuang: CardView

    private lateinit var databaseReference: DatabaseReference
    private var dataListener: ValueEventListener? = null

    //alert
    fun showCustomAlert(context: Context, iconRes: Int, title: String, message: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_alert, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        val iconImage = dialogView.findViewById<ImageView>(R.id.iconImage)
        val titleText = dialogView.findViewById<TextView>(R.id.alertTitle)
        val messageText = dialogView.findViewById<TextView>(R.id.alertMessage)
        val closeButton = dialogView.findViewById<ImageView>(R.id.closeButton)

        iconImage.setImageResource(iconRes)
        titleText.text = title
        messageText.text = message

        closeButton.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // Set ukuran dialog ke 250dp x 250dp
        val width = context.resources.displayMetrics.density * 250
        val height = context.resources.displayMetrics.density * 250
        dialog.window?.setLayout(width.toInt(), height.toInt())
    }




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

    fun setCardViewColorsByWidth(
        cardView: CardView,
        @ColorRes baseColorRes: Int,
        @ColorRes overlayColorRes: Int,
        overlayPercentage: Float
    ) {
        val context = cardView.context
        val baseColor = ContextCompat.getColor(context, baseColorRes)
        val overlayColor = ContextCompat.getColor(context, overlayColorRes)

        // Batasi maksimum overlay sampai 60%
        val clampedOverlayPercent = overlayPercentage.coerceIn(0f, 60f)

        // Hapus semua view sebelumnya
        cardView.removeAllViews()

        val container = ConstraintLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(baseColor) // warna dasar
        }

        val overlayView = View(context).apply {
            id = View.generateViewId()
            setBackgroundColor(overlayColor)
            visibility = if (clampedOverlayPercent == 0f) View.GONE else View.VISIBLE
        }

        // Tambahkan overlay dari kiri ke kanan
        if (clampedOverlayPercent > 0f) {
            container.addView(overlayView, ConstraintLayout.LayoutParams(0, 0).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToStart = ConstraintLayout.LayoutParams.UNSET
                matchConstraintPercentWidth = clampedOverlayPercent / 100f
            })
        }

        // Tambahkan container ke dalam CardView
        cardView.addView(container)
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        tvNutrisi = view.findViewById(R.id.tvNutrisi)
        tvpH = view.findViewById(R.id.tvpH)
        tvTinggi = view.findViewById(R.id.tvTinggi)
        waveView = view.findViewById(R.id.waterLevel)
        suhuAir = view.findViewById(R.id.suhuAir)
        suhuRuang = view.findViewById(R.id.suhuRuang)
        cardView = view.findViewById(R.id.cardViewContainer)

//        arcProgress = view.findViewById(R.id.arcProgress)

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
        databaseReference = FirebaseDatabase.getInstance().getReference("Siponik")
        refreshFirebaseData()

        val cardnutrisi = view.findViewById<CardView>(R.id.cardnutrisi)

        //alert
        cardnutrisi.setOnClickListener {
            showCustomAlert(
                requireContext(), // atau requireActivity()
                R.drawable.nutrisi_icon,
                "Informasi Kadar Nutrisi",
                "Kadar nutrisi berada di angka 100-150 ppm"
            )
    }
        val cardph = view.findViewById<CardView>(R.id.cardph)

        cardph.setOnClickListener {
            showCustomAlert(
                requireContext(),
                R.drawable.ph_icon,
                "Informasi Nilai pH",
                "pH harus berada di angka 6-7"
            )
        }

        val cardsuhu = view.findViewById<CardView>(R.id.cardsuhu)

        cardsuhu.setOnClickListener {
            showCustomAlert(
                requireContext(),
                R.drawable.nutrisi_icon,
                "Informasi Suhu",
                "Suhu harus berada di angka 15-25 C"
            )
        }
        val cardair = view.findViewById<CardView>(R.id.cardair)

        cardair.setOnClickListener {
            showCustomAlert(
                requireContext(),
                R.drawable.air_icon,
                "Informasi Stok Air",
                "Suhu harus berada di angka 15-25 C"
            )
        }



    }

    override fun refreshFirebaseData() {
        // detach listener lama
        dataListener?.let { databaseReference.removeEventListener(it) }

        // re-attach listener baru
        val userId = auth.currentUser?.uid ?: return
        dataListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val userIdFromDb = child.child("UserInfo/userId").value.toString()
                    if (userIdFromDb == userId) {
                        val dataNutrisi = child.child("Data/Nutrisi").value.toString()
                        val datapH = child.child("Data/pH").value.toString()
                        val dataSuhuAir = child.child("Data/SuhuAir").value.toString()
                        val dataSuhuRuang = child.child("Data/SuhuRuang").value.toString()
                        val dataKetinggianAir = child.child("Data/KetinggianAir").value.toString()

                        nutrisi = dataNutrisi.toFloat()
                        pH = datapH.toFloat()
                        suhuRuangValue = dataSuhuRuang.toFloat()
                        suhuAirValue = dataSuhuAir.toFloat()
                        ketinggianAir = dataKetinggianAir.toFloat()

                        view!!.findViewById<TextView>(R.id.tvSuhuAir).text = "$dataSuhuAir°C"
                        view!!.findViewById<TextView>(R.id.tvSuhuRuang).text = "$dataSuhuRuang°C"

                        setCardViewColors(cardView, R.color.hijau_muda, R.color.hijau_pastel, ketinggianAir)
                        setCardViewColorsByWidth(suhuAir, R.color.hijau_muda, R.color.hijau_pastel, suhuAirValue)
                        setCardViewColorsByWidth(suhuRuang, R.color.hijau_muda, R.color.hijau_pastel, suhuRuangValue)

                        tvNutrisi.text = dataNutrisi
                        tvpH.text = datapH
                        tvTinggi.text = " $dataKetinggianAir %"

//                        context?.let { ctx ->
//                            val baseColor = ContextCompat.getColor(ctx, R.color.hijau_muda)
//                            val progressColor = ContextCompat.getColor(ctx, R.color.hijau_tua)
//                            val capColor = ContextCompat.getColor(ctx, R.color.hijau_tua)
//                            val colorText = ContextCompat.getColor(ctx, R.color.hijau_muda)
//
//                            arcProgress.apply {
//                                baseArcColor = baseColor
//                                baseArcWidth = 28f
//
//                                progressArcColor = progressColor
//                                progressArcWidth = 15f
//
//                                capCircleColor = capColor
//                                capCircleRadius = 25f
//
//                                textColor = colorText
//                                textSize = 40f
//
//                                textFont = ResourcesCompat.getFont(ctx, R.font.poppins_bold)
//
//                                setMaxValue(50f)
//                                setProgressValue(suhu)
//                            }
//                        }

                        break
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        databaseReference.addValueEventListener(dataListener as ValueEventListener)
    }
}
