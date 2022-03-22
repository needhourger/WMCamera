package cc.yuukisama.wmcamera

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cc.yuukisama.wmcamera.utils.LocationController
import com.amap.api.location.AMapLocationClientOption
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var mLocationController: LocationController

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

    class Getter {
        private lateinit var mActivity: MainActivity
        fun getActivity(): MainActivity {
            return mActivity
        }

        fun setActivity(activity: MainActivity) {
            mActivity = activity
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        const val TIME_FORMAT = "yyy.MM.dd HH:mm:ss"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        val getter = Getter()
    }
}