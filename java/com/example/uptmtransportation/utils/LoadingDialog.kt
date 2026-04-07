package com.example.uptmtransportation.utils

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import com.example.uptmtransportation.R

class LoadingDialog(private val activity: Activity) {

    private var dialog: AlertDialog? = null

    fun show() {
        if (isShowing()) return
        
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        builder.setView(inflater.inflate(R.layout.dialog_loading, null))
        builder.setCancelable(false)
        dialog = builder.create()
        dialog?.show()
    }

    fun dismiss() {
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
        }
        dialog = null
    }

    fun isShowing(): Boolean {
        return dialog?.isShowing ?: false
    }
}