package com.en_trega.utils

import android.view.View
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
            if (this is AppCompatSpinner) {
                requestFocusFromTouch()
            } else {
                requestFocus()
            }
        }
        ErrorSnackbar.show(rootView, message, Snackbar.LENGTH_LONG)
        return false
    }
}