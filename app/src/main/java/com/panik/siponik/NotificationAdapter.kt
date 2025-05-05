package com.panik.siponik

import NotificationModel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(private val notifList: List<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

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

        // Event jika notifikasi diklik
        holder.itemView.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Notif: ${notif.title}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = notifList.size
}
