package cc.yuukisama.wmcamera.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import cc.yuukisama.wmcamera.MainActivity

class Permission {
    companion object {
        val REQUIRED_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        const val REQUEST_CODE_PERMISSIONS = 10

        @JvmStatic
        fun allPermissionGranted(context:Context) = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                context, it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}