package com.janoski.retroframe

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

class CameraController(private val context: Context) {
    val cameraExecutor = Executors.newSingleThreadExecutor()
    var imageCapture: ImageCapture? = null
        private set
    var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        private set

    fun bind(previewView: PreviewView, onReady: () -> Unit = {}) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture
            provider.unbindAll()
            provider.bindToLifecycle(context as androidx.lifecycle.LifecycleOwner, cameraSelector, preview, capture)
            onReady()
        }, ContextCompat.getMainExecutor(context))
    }

    fun flip(previewView: PreviewView, onReady: () -> Unit = {}) {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        bind(previewView, onReady)
    }

    fun capture(
        previewView: PreviewView,
        filter: FilmAdjustments,
        onSaved: (Uri) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("Camera is not ready"))
            return
        }
        val photoFile = File.createTempFile("retro_", ".jpg", context.cacheDir)
        val options = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                photoFile.delete()
                onError(exception)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val original = BitmapFactory.decodeFile(photoFile.absolutePath)
                        ?: error("Unable to decode captured photo")
                    val rotated = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                        android.graphics.Matrix().let { m ->
                            m.postScale(-1f, 1f)
                            android.graphics.Bitmap.createBitmap(original, 0, 0, original.width, original.height, m, true)
                        }
                    } else original
                    val filtered = FilterProcessor.apply(rotated, filter)
                    val uri = saveToGallery(filtered)
                    filtered.recycle()
                    if (rotated !== original) rotated.recycle()
                    original.recycle()
                    photoFile.delete()
                    ContextCompat.getMainExecutor(context).execute { onSaved(uri) }
                } catch (t: Throwable) {
                    photoFile.delete()
                    ContextCompat.getMainExecutor(context).execute { onError(t) }
                }
            }
        })
    }

    private fun saveToGallery(bitmap: android.graphics.Bitmap): Uri {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())
        val values = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "RetroFrame_$stamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RetroFrame")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create gallery item")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 94, output)) { "JPEG encode failed" }
            } ?: error("Could not open gallery output")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
    }
}
