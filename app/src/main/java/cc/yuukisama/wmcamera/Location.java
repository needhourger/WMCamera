package cc.yuukisama.wmcamera;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationListener;


/*
* 时间
* 天气
* 地点
* 方位角
* 经纬度
* */
public class Location implements AMapLocationListener {

    private static final String TAG = "Location";
    private static final int ID = 1001;

    private Context mContext;

    private AMapLocationClient mLocationClient;
    private AMapLocation mlocation;

    public Location(Context context) {
        mContext = context;
        mLocationClient = new AMapLocationClient(mContext);

        mLocationClient.setLocationListener(this);
        mLocationClient.enableBackgroundLocation(ID,buildNotification());

        mLocationClient.startLocation();
    }

    public AMapLocation getOriginalLocation(){
        return mlocation;
    }


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                Log.d(TAG, "onLocationChanged: " + aMapLocation.getAddress());
                mlocation = aMapLocation;
                return;
            }
            switch (aMapLocation.getErrorCode()){
                case 1:
                    Log.i(TAG, "onLocationChanged: missing params");
                    break;
                case 2:
                    Log.i(TAG, "onLocationChanged: single wifi detected and no more information");
                    break;
                case 12:
                    Log.i(TAG, "onLocationChanged: no location permission");
                default:
                    Log.i(TAG, "onLocationChanged: errcode = "+aMapLocation.getErrorCode());
            }
        }
    }


    private static final String NOTIFICATION_CHANNEL_NAME = "BackgroundLocation";
    private NotificationManager notificationManager = null;
    boolean isCreateChannel = false;

    public void stopLocation(){
        mLocationClient.disableBackgroundLocation(true);
    }

    @SuppressLint("NewApi")
    private Notification buildNotification() {

        Notification.Builder builder = null;
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            //Android O上对Notification进行了修改，如果设置的targetSDKVersion>=26建议使用此种方式创建通知栏
            if (null == notificationManager) {
                notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            String channelId = mContext.getPackageName();
            if (!isCreateChannel) {
                NotificationChannel notificationChannel = new NotificationChannel(channelId,
                        NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.enableLights(true);//是否在桌面icon右上角展示小圆点
                notificationChannel.setLightColor(Color.BLUE); //小圆点颜色
                notificationChannel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
                notificationManager.createNotificationChannel(notificationChannel);
                isCreateChannel = true;
            }
            builder = new Notification.Builder(mContext, channelId);
        } else {
            builder = new Notification.Builder(mContext);
        }
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(mContext.getResources().getString(R.string.app_name))
                .setContentText("is Running background ...")
                .setWhen(System.currentTimeMillis());
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notification = builder.build();
        } else {
            return builder.getNotification();
        }
        return notification;
    }
}
