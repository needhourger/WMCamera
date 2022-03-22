package cc.yuukisama.wmcamera;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/*
 * 时间
 * 天气
 * 地点
 * 方位角
 * 经纬度
 * */
public class LocationController implements AMapLocationListener {

    private static final String TAG = "Location";
    private static final int ID = 1001;

    private final Context mContext;

    private final AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private AMapLocation mlocation;

    private int preErrorCode;

    public int locationType;
    public double latitude;
    public double longtitude;
    public float accuracy;
    public String address;
    public String country;
    public String province;
    public String city;
    public String district;
    public String street;
    public String streetNum;
    public String cityCode;
    public String adCode;
    public String aoiName;
    public String buildingId;
    public String floor;
    public int gpsAccuracyStatus;
    public Date date = new Date(System.currentTimeMillis());
    public float bearing;
    public String description;
    public String detail;


    public LocationController(Context context,
                              AMapLocationClientOption.AMapLocationPurpose purpose,
                              AMapLocationClientOption.AMapLocationMode mode) throws Exception {
        mContext = context;
        mLocationOption = new AMapLocationClientOption();
        mLocationOption.setLocationPurpose(purpose);
        mLocationOption.setLocationMode(mode);
        if (purpose == AMapLocationClientOption.AMapLocationPurpose.SignIn) {
            mLocationOption.setOnceLocation(true);
            mLocationOption.setOnceLocationLatest(true);
        }
        mLocationOption.setSensorEnable(true);

        // Amp individual privacy
        AMapLocationClient.updatePrivacyShow(context,true,true);
        AMapLocationClient.updatePrivacyAgree(context,true);

        mLocationClient = new AMapLocationClient(mContext);
        mLocationClient.setLocationOption(mLocationOption);
        mLocationClient.setLocationListener(this);
        if (purpose != AMapLocationClientOption.AMapLocationPurpose.SignIn) {
            mLocationClient.enableBackgroundLocation(ID, buildNotification());
        }
        mLocationClient.startLocation();
    }

    public AMapLocation getOriginalLocation() {
        return mlocation;
    }

    public void dump() {
        @SuppressLint("DefaultLocale")
        String ret = String.format("latitude: %f longtitude: %f \n" +
                        "address: %s \n" +
                        "country: %s \n" +
                        "province: %s \n" +
                        "city: %s \n" +
                        "district: %s \n" +
                        "street: %s \n" +
                        "aoi: %s \n" +
                        "date: %s \n",
                latitude, longtitude, address, country, province, city, district, street, aoiName, date
        );
        Log.d(TAG, "dump: \n" + ret);
    }

    public String getWaterMarkText() {
        refrestOnceLocation();

        @SuppressLint("DefaultLocale")
        String ret = String.format(
                "%s \n" +
                        "经度: %f 纬度: %f \n" +
                        "地区: %s \n" +
                        "方位角: %f \n" +
                        "时间: %s \n",
                address == null ? "" : address,
                longtitude,
                latitude,
                address == null ? "" : country + province + city + street,
                bearing,
                new SimpleDateFormat(MainActivity.TIME_FORMAT, Locale.CHINA).format(date)
        );
        Log.d(TAG, "getWaterMarkText: \n" + ret);
        return ret;
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                Log.d(TAG, "onLocationChanged: " + aMapLocation.getAddress());
                mlocation = aMapLocation;

                locationType = mlocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                latitude = mlocation.getLatitude();//获取纬度
                longtitude = mlocation.getLongitude();//获取经度
                accuracy = mlocation.getAccuracy();//获取精度信息
                address = mlocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                country = mlocation.getCountry();//国家信息
                province = mlocation.getProvince();//省信息
                city = mlocation.getCity();//城市信息
                district = mlocation.getDistrict();//城区信息
                street = mlocation.getStreet();//街道信息
                streetNum = mlocation.getStreetNum();//街道门牌号信息
                cityCode = mlocation.getCityCode();//城市编码
                adCode = mlocation.getAdCode();//地区编码
                aoiName = mlocation.getAoiName();//获取当前定位点的AOI信息
                buildingId = mlocation.getBuildingId();//获取当前室内定位的建筑物Id
                floor = mlocation.getFloor();//获取当前室内定位的楼层
                gpsAccuracyStatus = mlocation.getGpsAccuracyStatus();//获取GPS的当前状态
                bearing = mlocation.getBearing();
                description = mlocation.getDescription();
                detail = mlocation.getLocationDetail();

                //获取定位时间
                date = new Date(mlocation.getTime());

                return;
            }

            switch (aMapLocation.getErrorCode()) {
                case 1:
                    Log.i(TAG, "onLocationChanged: missing params");
                    break;
                case 2:
                    Log.i(TAG, "onLocationChanged: single wifi detected and no more information");
                    break;
                case 12:
                    Log.i(TAG, "onLocationChanged: no location permission");
                default:
                    Log.i(TAG, "onLocationChanged: errcode = " + aMapLocation.getErrorCode() + "\n" + aMapLocation.getErrorInfo());
                    if (preErrorCode != aMapLocation.getErrorCode()) {
                        Toast.makeText(mContext, aMapLocation.getErrorInfo(), Toast.LENGTH_SHORT).show();
                        preErrorCode = aMapLocation.getErrorCode();
                    }

            }

        }
    }

    public void refrestOnceLocation() {
        if (mLocationOption.getLocationPurpose() != AMapLocationClientOption.AMapLocationPurpose.SignIn) {
            Log.d(TAG, "refrestOnceLocation: not in single mode");
            return;
        }
        mLocationClient.startLocation();
    }

    public void stopLocation() {
        mLocationClient.disableBackgroundLocation(true);
    }


    private static final String NOTIFICATION_CHANNEL_NAME = "BackgroundLocation";
    private NotificationManager notificationManager = null;
    boolean isCreateChannel = false;

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
