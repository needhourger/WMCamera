package com.example.wmcamera.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.example.wmcamera.R
import java.text.SimpleDateFormat
import java.util.*

class AMapUtils(private val context: Activity): AMapLocationListener {
    private var instance: AMapLocationClient
    private var currentPostion:AMapLocation? = null

    init {
        val option = AMapLocationClientOption()
        option.locationPurpose = AMapLocationClientOption.AMapLocationPurpose.SignIn
        option.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        option.isOnceLocationLatest = true
        option.isNeedAddress = true
        option.isSensorEnable = true
        option.httpTimeOut = 20000

        instance = AMapLocationClient(context)
        instance.setLocationListener(this)
        instance.setLocationOption(option)
        instance.stopLocation()
        instance.startLocation()
    }

    fun getOnceLocationString():String {
        instance.startLocation()
        if (currentPostion != null && currentPostion!!.errorCode==0) {
            if (currentPostion!!.errorCode == 0) {
                return context.resources.getString(R.string.water_mark_title)+
                        "${currentPostion!!.address}\n" +
                        generateCoordinates(currentPostion!!) +
                        "${currentPostion!!.country}${currentPostion!!.province}${currentPostion!!.city}" +
                        "${currentPostion!!.street}${currentPostion!!.streetNum}\n" +
                        context.resources.getString(R.string.bearing)+" ${currentPostion!!.bearing}\n" +
                        context.resources.getString(R.string.altitude)+" ${currentPostion!!.altitude}\n"+
                        "${SimpleDateFormat(TIME_FORMAT, Locale.CHINA).format(Date())}"
            } else {
                Log.e(TAG,"location Error, ErrCode:"
                        + currentPostion!!.errorCode + ", errInfo:"
                        + currentPostion!!.errorInfo);
            }
        }
        return context.resources.getString(R.string.no_position_result)
    }

    override fun onLocationChanged(position: AMapLocation?) {
        Log.d(TAG, "onLocationChanged: $position")
        currentPostion =  position
    }

    private fun generateCoordinates(position: AMapLocation): String {
        val longitude = position.longitude
        val latitude = position.latitude
        var ret = ""
        ret += if (longitude<0) "$longitude W - "
        else "$longitude E - "
        ret += if (latitude<0) "$latitude S\n"
        else "$latitude N\n"
        return ret
    }

    fun destroy() {
        instance.onDestroy()
    }

    companion object {
        const val TAG = "AmapLoaction"
        val PERMISSION_REQUEST_CODE = 2
        val Permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
        )
        const val TIME_FORMAT = "yyy.MM.dd HH:mm:ss"

        @JvmStatic
        fun checkPermission(context: Context): Boolean {
            for (p in Permissions) {
                if (ContextCompat.checkSelfPermission(context,p)!=PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

        @JvmStatic
        fun requestPermission(context: Activity) {
            if (!checkPermission(context)) {
                AMapLocationClient.updatePrivacyShow(context,true,true)
                AMapLocationClient.updatePrivacyAgree(context,true)
                ActivityCompat.requestPermissions(context, Permissions, PERMISSION_REQUEST_CODE)
            }
        }
    }
}