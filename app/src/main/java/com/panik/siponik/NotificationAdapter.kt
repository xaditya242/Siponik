package com.panik.siponik

import NotificationModel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class NotificationAdapter(
    private val notifList: MutableList<NotificationModel>
): RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.judulNotif)
        val message: TextView = view.findViewById(R.id.detailNotif)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false) // Pakai custom XML
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = notifList[position]
        holder.title.text = notif.title
        holder.message.text = notif.message

        if (!notif.isRead) {
//            holder.indicator.visibility = View.VISIBLE
            // Ubah style untuk notifikasi yang belum dibaca - misalnya warna teks lebih tebal
            holder.title.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
//            holder.indicator.visibility = View.GONE
            holder.title.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Event jika notifikasi diklik
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            Toast.makeText(context, "Notif: ${notif.title}", Toast.LENGTH_SHORT).show()
            holder.title.setTypeface(null, android.graphics.Typeface.NORMAL)
            // Tandai notifikasi sebagai telah dibaca
            val positionKey = holder.adapterPosition.toString()
            val sharedPref = context.getSharedPreferences("UserSession", AppCompatActivity.MODE_PRIVATE)
            val espId = sharedPref.getString("espId", "") ?: ""
            val database = FirebaseDatabase.getInstance().getReference("Siponik/$espId/Notifikasi")

            database.child(positionKey).child("isRead").setValue(true)
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            Toast.makeText(context, "Notif: ${notif.title}", Toast.LENGTH_SHORT).show()

            // Tandai notifikasi sebagai telah dibaca dalam Firebase
            val sharedPref = context.getSharedPreferences("UserSession", AppCompatActivity.MODE_PRIVATE)
            val espId = sharedPref.getString("espId", "") ?: ""

            // Cari key notifikasi yang tepat di Firebase
            val database = FirebaseDatabase.getInstance().getReference("Siponik/$espId/Notifikasi")

            // Buat query untuk menemukan notifikasi dengan judul dan pesan yang sama
            database.orderByChild("title").equalTo(notif.title).get().addOnSuccessListener { dataSnapshot ->
                for (childSnapshot in dataSnapshot.children) {
                    val message = childSnapshot.child("message").getValue(String::class.java)
                    if (message == notif.message) {
                        // Tandai notifikasi yang ditemukan sebagai telah dibaca
                        childSnapshot.ref.child("isRead").setValue(true)

                        // Perbarui model data lokal juga
                        notif.isRead = true
                        notifyItemChanged(position)
                        break
                    }
                }
            }
        }

    }

    override fun getItemCount() = notifList.size

    fun updateList(newList: List<NotificationModel>) {
        notifList.clear()
        notifList.addAll(newList)
        notifyDataSetChanged()
    }
}
