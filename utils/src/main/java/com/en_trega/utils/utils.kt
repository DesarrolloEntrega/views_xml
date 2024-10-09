package com.en_trega.utils

import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatSpinner
import com.en_trega.desarrollo.snackbar.ErrorSnackbar
import com.google.android.material.snackbar.Snackbar

object Utils {

    fun spinnerHasData(spinner: AppCompatSpinner?): Boolean {
        return spinner?.adapter != null
                && spinner.adapter.count > 0
                && spinner.selectedItemPosition > 0
    }

    fun showMissingData(message: String, viewToFocus: View?, rootView: View): Boolean {
        viewToFocus?.apply {

            when (this) {
                is AppCompatSpinner -> requestFocusFromTouch()
                is ImageButton -> {
                    isFocusableInTouchMode = true
                    requestFocus()
                    isFocusableInTouchMode = false
                }
                else -> requestFocus()
            }
        }
        ErrorSnackbar.show(rootView, message, Snackbar.LENGTH_LONG)
        return false
    }
}