package cc.yuukisama.wmcamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
    private lateinit var mCamera: Camera
    private lateinit var previewView:Preview

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

            previewView = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            orientationEventListener.enable()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                mCamera = cameraProvider.bindToLifecycle(
                    MainActivity.getter.getActivity(),
                    cameraSelector,
                    previewView,
                    imageCapture
                )
                viewFinder.setOnTouchListener(object : OnTouchListener {
                    override fun onTouch(view: View?, pos: MotionEvent?): Boolean {
                        val factory = viewFinder.meteringPointFactory
                        val point = pos?.let { factory.createPoint(it.x, pos.y) }
                        val action = point?.let { FocusMeteringAction.Builder(it).build() }
                        if (action != null) {
                            startFocusAnim(pos)
                            mCamera.cameraControl.startFocusAndMetering(action)
                        }
                        return true
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "startCamera: binding lifecycle failed", e)
            }
        }, ContextCompat.getMainExecutor(mContext))
    }

    private val dismissFocusAnim: () -> Unit = { focus_image.visibility = GONE }
    private fun startFocusAnim(pos: MotionEvent) {
        if (focus_image.isVisible) {
            handler.removeCallbacks(dismissFocusAnim)
        }
        focus_image.x = pos.x - focus_image.width / 2
        focus_image.y = pos.y - focus_image.height / 2
        focus_image.visibility = VISIBLE
        handler.postDelayed(dismissFocusAnim, 1600)
    }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(mContext) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture?.targetRotation = rotation
            }
        }
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