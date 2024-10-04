package com.en_trega.desarrollo.snackbar

import android.view.View
import com.google.android.material.snackbar.Snackbar

object GreenSnackbar {
    fun show(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.view.setBackgroundColor(view.context.getColor(android.R.color.holo_green_light))
        snackbar.show()
    }
}