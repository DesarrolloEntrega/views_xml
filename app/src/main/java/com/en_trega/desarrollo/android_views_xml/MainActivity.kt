package com.en_trega.desarrollo.android_views_xml

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.en_trega.camera.CameraActivity
import com.en_trega.camera.CameraActivityResultContract
import com.en_trega.camera.CameraParams
import com.en_trega.desarrollo.app.R
import com.en_trega.desarrollo.app.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val cameraActivityResultLauncher = registerForActivityResult(
        CameraActivityResultContract()
    ) { result ->
        result?.let {
            Log.d("MainActivity", "Photo file name: ${it.photoName}")
            Log.d("MainActivity", "Photo code: ${it.code}")
            Log.d("MainActivity", "Photo path: ${it.photoPath}")

            val imagePath = File(filesDir, "${it.photoPath}/${it.photoName}")
            if (imagePath.exists()) {
                Log.d("MainActivity", "Photo file exists")
                val bitmap = BitmapFactory.decodeFile(imagePath.absolutePath)
                binding.photoImageView.setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.startCameraButton.setOnClickListener {
            val cameraParams = CameraParams(
                code = 1,
                photoPath = "Prueba",
                photoName = "algoMas",
                quality = 90
            )
            cameraActivityResultLauncher.launch(cameraParams)
//            CameraActivity.deletePictures(this@MainActivity)
        }

    }
}