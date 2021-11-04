package cc.yuukisama.wmcamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.camera_view.view.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val mContext: Context = context

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var mLocationController: LocationController

    init {
        initView()
    }

    fun setLocationController(locationController: LocationController) {
        mLocationController = locationController
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(mContext)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(MainActivity.getter.getActivity(), cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "startCamera: binding lifecycle failed", e)
            }
        }, ContextCompat.getMainExecutor(mContext))
    }

    private fun initView() {
        View.inflate(mContext, R.layout.camera_view, this)
        camera_capture_button.setOnClickListener { takePhoto() }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        startCaptureAnim()
        val imageCapture = imageCapture ?: return
        Log.d(TAG, "takePhoto: called")
        val filename = SimpleDateFormat(
            FILENAME_FORMAT,
            Locale.CHINA
        ).format(System.currentTimeMillis()) + ".jpg"

        val photoFile = File(
            outputDirectory,
            filename
        )
        Log.d(TAG, "takePhoto: photoFile=" + photoFile.absolutePath);

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(mContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    stopCaptureAnim()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    val resBitmap = ImageUtils.drawTextLeftBottom(
                        mContext,
                        photoFile,
                        mLocationController.getWaterMarkText(),
                        30,
                        5,
                        R.color.watermark_white
                    )
                    if (resBitmap != null) {
                        saveBitmap(resBitmap, filename)
                    }
                    stopCaptureAnim()
                }
            })
    }

    private fun saveBitmap(bitmap: Bitmap, url: String) {
        try {
            val photoFile = File(
                outputDirectory,
                "wm_$url"
            )
            Log.d(TAG, "saveBitmap to path: " + photoFile.absolutePath)
            val fileOutputStream = FileOutputStream(photoFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            mContext.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://" + photoFile.parent)
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = mContext.externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else mContext.filesDir
    }

    private fun startCaptureAnim() {
        camera_capture_button.isEnabled = false
        camera_capture_button.alpha = 0.5f
    }

    private fun stopCaptureAnim() {
        camera_capture_button.isEnabled = true
        camera_capture_button.alpha = 1f
    }

    fun shutdown() {
        cameraExecutor.shutdown();
    }

    companion object {
        private const val TAG = "CameraXBaisc"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val TIME_FORMAT = "yyy.MM.dd HH:mm:ss"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}