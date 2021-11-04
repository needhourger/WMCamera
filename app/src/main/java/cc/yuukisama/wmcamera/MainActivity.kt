package cc.yuukisama.wmcamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocationClientOption
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var mLocationController:LocationController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getter.setActivity(this)

        if (allPermissionGranted()) {
            mCameraView.startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        mLocationController = LocationController(
            baseContext,
            AMapLocationClientOption.AMapLocationPurpose.Sport,
            AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        )
        mCameraView.setLocationController(mLocationController)
    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraView.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                mCameraView.startCamera()
            } else {
                Toast.makeText(this, "Permission not granted by the user", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    class Getter{
        private lateinit var mActivity:MainActivity
        fun getActivity():MainActivity{
            return mActivity
        }
        fun setActivity(activity: MainActivity){
            mActivity = activity
        }
    }

    companion object {
        private const val TAG = "CameraXBaisc"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val TIME_FORMAT = "yyy.MM.dd HH:mm:ss"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION)
        val getter = Getter()
    }
}