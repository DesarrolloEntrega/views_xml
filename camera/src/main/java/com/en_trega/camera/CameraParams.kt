package com.en_trega.camera

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CameraParams(
    val code: Int = 0,
    val photoName: String?,
    val photoPath: String?
) : Parcelable