package com.en_trega.camera

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity

class CameraActivityResultContract : ActivityResultContract<CameraParams, CameraResult?>() {
    override fun createIntent(context: Context, input: CameraParams): Intent {
        return context.createCameraIntent(input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CameraResult? {
        return if (resultCode == AppCompatActivity.RESULT_OK && intent != null) {
            CameraResult(
                photoName = intent.getStringExtra("photoName"),
                code = intent.getIntExtra("code", -1),
                photoPath = intent.getStringExtra("photoPath")
            )
        } else {
            null
        }
    }
}

data class CameraResult(
    val photoName: String? = "",
    val code: Int = -1,
    val photoPath: String? = ""
)