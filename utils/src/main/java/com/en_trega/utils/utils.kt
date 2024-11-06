@file:Suppress("unused")
package com.en_trega.utils

import android.content.Context
import android.util.Base64.encodeToString
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatSpinner
import com.en_trega.desarrollo.snackbar.ErrorSnackbar
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object Utils {

    /**
     * Checks if the specified AppCompatSpinner has valid data selected.
     * This function verifies that the spinner has an adapter, contains at least one item, and that the selected
     * item is not the first position (often used for "Select an option" placeholder items).
     *
     * @param spinner The AppCompatSpinner to check for data selection.
     * @return True if the spinner has a valid selection (adapter is not null, contains items, and a non-placeholder item is selected);
     *         otherwise, returns false.
     */
    fun spinnerHasData(spinner: AppCompatSpinner?): Boolean {
        return spinner?.let {
            val adapter = it.adapter
            val itemCount = adapter?.count ?: 0
            itemCount > 1 && it.selectedItemPosition > 0
        } ?: false
    }


    /**
     * Displays an error message indicating missing data and sets focus to the specified view.
     * This function shows a Snackbar with the provided message and requests focus on the given view.
     * If the view is an AppCompatSpinner, focus is requested using touch mode to open the spinner's options.
     * If the view is an ImageButton, focus is temporarily set to be touchable to grab focus.
     *
     * @param message The error message to be displayed in the Snackbar.
     * @param viewToFocus The view to request focus on; it can be an AppCompatSpinner, ImageButton, or any other view.
     * @param rootView The root view used to display the Snackbar message.
     * @return Always returns false to indicate that validation has failed due to missing data.
     */
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

    /**
     * Encodes the specified image file to a Base64 string.
     * The function reads the file located within the app's files directory, converts its content
     * to a byte array, and encodes it to a Base64 string.
     *
     * If the specified path does not point to an existing file or if the file is empty,
     * the function logs an error and returns an empty string.
     *
     * @param context The context used to access the app's files directory.
     * @param imagePath The relative path of the image file to encode.
     * @return A Base64-encoded string of the file content, or an empty string if an error occurs.
     */
    fun encodeImageToBase64(context: Context, imagePath: String): String {
        val file = File(context.filesDir, imagePath)
        if (!file.exists() || !file.isFile) {
            Log.e("encodeImageToBase64", "File does not exist or is not a file: $imagePath")
            return ""
        }

        return try {
            val fileLength = file.length().toInt()
            if (fileLength <= 0) {
                Log.e("encodeImageToBase64", "File is empty: $imagePath")
                return ""
            }

            val bytes = ByteArray(fileLength)
            val fis = FileInputStream(file)
            fis.read(bytes)
            fis.close()
            encodeToString(bytes, android.util.Base64.DEFAULT)
        } catch (e: IOException) {
            Log.e("encodeImageToBase64", "Error reading file: $imagePath", e)
            e.printStackTrace()
            ""
        }
    }


    /**
     * Deletes the specified file or directory located within the app's files directory.
     * If the path points to a file, it will be deleted. If it points to a directory, the directory
     * and all its contents (files and subdirectories) will be deleted recursively.
     *
     * @param context The context used to access the app's files directory.
     * @param path The relative path of the file or directory to delete.
     */
    fun deletePath(context: Context, path: String) {
        val tempFile = File(context.filesDir, path)
        if (tempFile.exists()) {
            tempFile.deleteRecursively()
        }
    }
}