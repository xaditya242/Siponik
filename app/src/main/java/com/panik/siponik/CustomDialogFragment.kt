package com.panik.siponik

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class CustomDialogFragment(
    private val title: String,
    private val message: String,
    private val showInputField: Boolean = false,
    private val onContinue: ((String?) -> Unit)? = null,
    private val onCancel: (() -> Unit)? = null
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate layout
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_custom, null)

        // Pastikan tidak ada parent sebelumnya
        (view.parent as? ViewGroup)?.removeView(view)

        // Inisialisasi UI
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnNext = view.findViewById<LinearLayout>(R.id.btnDialogNext)
        val btnNo = view.findViewById<LinearLayout>(R.id.btnDialogNo)
        val etPassword = view.findViewById<EditText>(R.id.etPasswordInput)

        // Set data dari parameter
        tvTitle.text = title
        tvMessage.text = message

        if (showInputField) {
            etPassword.visibility = View.VISIBLE
        } else {
            etPassword.visibility = View.GONE
        }

        btnNext.setOnClickListener {
            if (showInputField) {
                val password = etPassword.text.toString()
                if (password.isEmpty()) {
                    etPassword.error = "Password tidak boleh kosong"
                    return@setOnClickListener
                } else {
                    onContinue?.invoke(password)
                    dismiss()
                }
            } else {
                onContinue?.invoke(null)
                dismiss()
            }
        }


        btnNo.setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }

        // Gunakan AlertDialog agar lebih fleksibel
        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }
}
