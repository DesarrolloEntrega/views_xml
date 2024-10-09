package com.en_trega.desarrollo.toast

import android.content.Context
import android.widget.Toast


//TODO: Borrar Todo el modulo toast, era solo para una prueba
object CustomToast {
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
}