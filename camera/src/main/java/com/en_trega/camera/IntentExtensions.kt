package com.en_trega.camera

import android.content.Context
import android.content.Intent
import android.os.Build

fun Context.createCameraIntent(params: CameraParams): Intent {
    return Intent(this, CameraActivity::class.java).apply {
        putExtra(CameraActivity.EXTRA_CAMERA_PARAMS, params)
    }
}

fun Intent.getCameraParams(): CameraParams? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(CameraActivity.EXTRA_CAMERA_PARAMS, CameraParams::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(CameraActivity.EXTRA_CAMERA_PARAMS)
    }
}