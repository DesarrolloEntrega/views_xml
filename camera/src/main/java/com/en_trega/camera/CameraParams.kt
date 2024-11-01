package com.en_trega.camera

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Camera parameters
 *
 * @param code Request code, used to identify the request, it will be returned in the result
 * @param photoName Name of the photo
 * @param photoPath Path of the photo
 * @param quality Must be between 0 and 100, (0 meaning compress for small size, 100 meaning compress for max quality)
 */
@Parcelize
data class CameraParams(
    val code: Int = 0,
    val photoName: String?,
    val photoPath: String?,
    val quality: Int = 100
) : Parcelable