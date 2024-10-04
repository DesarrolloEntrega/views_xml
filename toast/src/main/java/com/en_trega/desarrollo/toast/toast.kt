package com.en_trega.desarrollo.toast

import android.content.Context
import android.widget.Toast

object CustomToast {
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
}