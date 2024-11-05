package com.en_trega.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModelProvider
import com.en_trega.camera.databinding.ActivityCameraBinding
import com.en_trega.desarrollo.snackbar.ErrorSnackbar
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

//TODO: Utilizar mas el ViewModel
//FIXME: Se pierde el estado al girar la pantalla
//FIXME: Corregir errores de vista y hacerla responsive
class CameraActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CAMERA_PARAMS = "extra_camera_params"

        @Suppress("UNUSED")
        fun deletePictures(context: Context) {
            val baseDir = File(context.filesDir, "Pictures")
            if (baseDir.exists()) {
                baseDir.deleteRecursively()
            }
            baseDir.mkdirs()
        }
    }

    private lateinit var binding: ActivityCameraBinding
    private lateinit var viewModel: CameraViewModel
    private lateinit var imageCapture: ImageCapture
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var photoPreview: ImageView
    private lateinit var captureButton: ImageButton
    private lateinit var acceptButton: ImageButton
    private lateinit var retryButton: ImageButton
    private lateinit var flashButton: ImageButton
    private lateinit var cameraParams: CameraParams
    private var isFlashOn = false

    //#region Permissions

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            ErrorSnackbar.show(
                binding.root,
                "No se puede tomar fotos sin permisos de cámara",
                Snackbar.LENGTH_LONG
            )
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    //#endregion


    //#region Lifecycle methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        cameraParams = intent.getCameraParams() ?: throw IllegalArgumentException("CameraParams are required")

        viewModel = ViewModelProvider(this)[CameraViewModel::class.java]
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupListeners()

        if (allPermissionsGranted()) {
            if (savedInstanceState == null) {
                startCamera()
            } else {
                val savedPhotoPath = savedInstanceState.getString("photo_path")
                if (!savedPhotoPath.isNullOrEmpty()) {
                    viewModel.setPhotoPath(savedPhotoPath)
                    handlePictureTaken()
                }
            }
        } else {
            requestPermissions()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("photo_path", viewModel.getPhotoPath())
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteTempFile()
        cameraExecutor.shutdown()
    }

    //#endregion


    //MARK: UI setup

    private fun setupListeners() {
        photoPreview = binding.ivPhotoPreview
        captureButton = binding.ibCapture
        acceptButton = binding.ibAccept
        retryButton = binding.ibRetry
        flashButton = binding.ibFlash

        captureButton.setOnClickListener {
            takePicture()
        }

        acceptButton.setOnClickListener {
            viewModel.getPhotoPath()?.let { path ->
                val bitmap = BitmapFactory.decodeFile(path)
                val rotatedBitmap = bitmap?.let { rotateBitmapIfNeeded(it, path) }
                val imageBytesArray = rotatedBitmap?.toByteArray()
                imageBytesArray?.let { processPicture(it) }
            }
        }

        retryButton.setOnClickListener {
            photoPreview.visibility = View.GONE
            captureButton.visibility = View.VISIBLE
            acceptButton.visibility = View.GONE
            retryButton.visibility = View.GONE
            flashButton.visibility = View.VISIBLE
            startCamera()
        }

        flashButton.setOnClickListener {
            toggleFlash()
        }
    }

    //#region Camera methods

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val previewView = binding.pvViewFinder

            val preview = Preview.Builder()
                .build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            imageCapture = ImageCapture.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy(Size(3000,4000), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER))
                        .build()
                )
                .setJpegQuality(100)
                .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val rotation = previewView.display?.rotation ?: 0
            val viewport = ViewPort.Builder(Rational(3, 4), rotation).build()
            val useCaseGroup = UseCaseGroup.Builder()
                .setViewPort(viewport)
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup)
            } catch (e: Exception) {
                Log.e("Camera Entrega", "Error al iniciar la cámara", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePicture() {
        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        val captureAnimation = AnimationUtils.loadAnimation(this, R.anim.capture_animation)
        captureButton.startAnimation(captureAnimation)

        imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                handlePictureTaken()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera Entrega", "Error al capturar la imagen:", exception)
            }
        })
    }

    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        flashButton.setImageResource(if (isFlashOn) R.drawable.ic_camera_flash_on else R.drawable.ic_camera_flash_off)
        imageCapture.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    //#endregion


    //#region File handling

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = File(filesDir, "Pictures/temp")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            viewModel.setPhotoPath(absolutePath)
        }
    }

    private fun deleteTempFile() {
        val tempFile = File(filesDir, "Pictures/temp")
        if (tempFile.exists()) {
            tempFile.deleteRecursively()
        }
    }

    private fun getOutputDirectory(): File {
        val appName = applicationContext.applicationInfo.loadLabel(applicationContext.packageManager).toString()
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, appName).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun getFileName(directoryPath: String): String {
        return try {
            val baseDirectory = File(filesDir, directoryPath)
            if (!baseDirectory.exists()) {
                baseDirectory.mkdirs()
            }
            val countFiles = baseDirectory.listFiles()?.size ?: 0
            cameraParams.photoName?.trim()?.takeIf { it.isNotBlank() }?.let { photoName ->
                return "$photoName$countFiles.jpg"
            }
            "photo_$countFiles.jpg"
        } catch (e: Exception) {
            Log.e("Camera Entrega", "Error getting file count: ${e.message}")
            "photo_0.jpg"
        }
    }

    private fun handleExistingFile(directoryPath: String) {
        cameraParams.photoName?.trim()?.takeIf { it.isNotBlank() }?.let { photoName ->
            val file = File(directoryPath, photoName)
            if (file.exists() && file.delete()) {
                Log.d("CameraActivity", "File successfully deleted: $photoName")
            } else {
                Log.e("CameraActivity", "Failed to delete file or file does not exist: $photoName")
            }
        }
    }

    //#endregion


    //#region Image Processing

    private fun handlePictureTaken() {
        try {
            val bitmap = BitmapFactory.decodeFile(viewModel.getPhotoPath())
            val rotatedBitmap = bitmap?.let { rotateBitmapIfNeeded(it, viewModel.getPhotoPath()) }
            runOnUiThread {
                photoPreview.setImageBitmap(rotatedBitmap)
                photoPreview.visibility = View.VISIBLE
                captureButton.visibility = View.GONE
                acceptButton.visibility = View.VISIBLE
                retryButton.visibility = View.VISIBLE
                flashButton.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("Camera Entrega", "Error handlePictureTaken:", e)
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, photoPath: String?): Bitmap {
        val exifInterface = ExifInterface(photoPath!!)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        val quality = cameraParams.quality.takeIf { it in 0..100 } ?: 100
        compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    private fun processPicture(picture: ByteArray) {
        val directoryPath = "Pictures/${cameraParams.photoPath?.trim()}"
        val fileName = getFileName(directoryPath)

        val baseDirectory = File(filesDir, directoryPath)
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs()
        }

        handleExistingFile(fileName)

        val file = File(baseDirectory, fileName)
        file.writeBytes(picture)

        deleteTempFile()

        returnResult(fileName)
    }

    //#endregion


    //MARK: Result handling

    private fun returnResult(photoName: String) {
        val resultIntent = Intent().apply {
            putExtra("photoName", photoName)
            putExtra("code", cameraParams.code)
            putExtra("photoPath", "Pictures/${cameraParams.photoPath?.trim()}")
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}