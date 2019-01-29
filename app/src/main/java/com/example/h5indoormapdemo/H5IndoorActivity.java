package com.example.h5indoormapdemo;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class H5IndoorActivity extends AppCompatActivity implements TencentLocationListener {
    final int version = Build.VERSION.SDK_INT; // Android版本变量

    private WebView webView;
    private WebSettings webSettings;

    private TencentLocationManager mLocationManager;
    private TencentLocation mLocation;

    private static final String URL = "https://apis.map.qq.com/tools/indoor?";
    private String buildingId = "110000221958"; //建筑id
    private String cityCode = "110100"; //城市id
    private String indoorMapKey = "";//请到腾讯地图官网申请有室内图权限的key，并添加在AndroidManifest.xml文件中

    public static final String ErrMsg = "errMsg";
    private int errMsg;
    public static final int ERROR_OK = 0; //定位成功
    public static final int ERROR_NO_BLUETOOTH = 1; //定位失败，无法开启蓝牙
    public static final int ERROR_NO_INDOOR_LOCATE = 2; //定位失败，当前建筑不支持腾讯的高精度室内定位
    public static final int ERROR_NO_LOCATION_INFO = 3; //定位失败，无法获取定位回调信息

    private String locationInfo;
    private Boolean pageFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h5_indoor);
        //从AndroidManifest文件获取key
        try {
            ApplicationInfo mapplicationInfo = this.getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
            if (mapplicationInfo.metaData != null){
                indoorMapKey =(String)mapplicationInfo.metaData.getString("TencentMapSDK");
            }
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }

        Intent intent = getIntent();
        errMsg = intent.getIntExtra(ErrMsg, 0);
        startLocation();
        initWebView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocation();
    }

    /**
     * 注册位置监听
     */
    private void startLocation(){
        if (mLocationManager == null){
            mLocationManager = TencentLocationManager.getInstance(this);
            TencentLocationRequest request = TencentLocationRequest.create();
            request.setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_NAME); //设置定位的请求级别，决定定位结果包含的信息
            request.setAllowCache(true);
            request.setAllowDirection(true);
            request.setInterval(1000); //设置定位周期(位置监听器回调周期), 单位为 ms (毫秒)
            int err = mLocationManager.requestLocationUpdates(request,this);
            switch (err) {
                case 0:
                    mLocationManager.startIndoorLocation();
                    break;
                default:
                    errMsg = ERROR_NO_LOCATION_INFO;
                    break;
            }
        }
    }

    /**
     * 移除位置监听
     */
    private void stopLocation() {
        mLocationManager.stopIndoorLocation();
        mLocationManager.removeUpdates(this);
        mLocationManager = null;
    }

    /**
     * 位置变化回调
     * @param tencentLocation 新的位置
     * @param error 错误码
     * @param reason 错误描述
     */
    @Override
    public void onLocationChanged(TencentLocation tencentLocation, int error, String reason) {
        if (error == TencentLocation.ERROR_OK){
            mLocation = tencentLocation;
            if (pageFinished){
                locationInfo = buildParamJson(mLocation.getLatitude(), mLocation.getLongitude(),
                        mLocation.getIndoorBuildingId(),mLocation.getIndoorBuildingFloor(), mLocation.getAccuracy(), mLocation.getDirection(), mLocation.getSpeed(), errMsg);
                sendLocationToH5(locationInfo);
            }
        }else{
            errMsg = ERROR_NO_LOCATION_INFO;
            if (pageFinished){
                locationInfo = buildParamJson(0.0, 0.0,
                        null, null, 0.0, 0.0, 0.0, errMsg);
                sendLocationToH5(locationInfo);
            }
        }
    }

    /**
     *状态变化回调
     * @param name GPS，Wi-Fi等
     * @param status 新的状态, 启用或禁用
     * @param desc 状态描述
     */
    @Override
    public void onStatusUpdate(String name, int status, String desc) {
        //TODO
    }

    /**
     * 初始化webWiew，加载H5室内图
     */
    private void initWebView(){
        webView = (WebView)findViewById(R.id.web_view);
        webSettings = webView.getSettings();
        //允许Https+Http混用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webSettings.setJavaScriptEnabled(true); //如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        webSettings.setDomStorageEnabled(true);//开启DOM缓存，关闭的话H5自身的一些操作是无效的
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);  //设置缓存模式，不使用缓存，只从网络下载

        StringBuilder urlBuilder = new StringBuilder(128);
        urlBuilder.append(URL);
        urlBuilder.append("bid=" + buildingId);
        urlBuilder.append("&cid=" + cityCode);
        urlBuilder.append("&key=" + indoorMapKey);
        urlBuilder.append("&referer=wjz");
        webView.loadUrl(urlBuilder.toString());

        //设置不用系统浏览器打开,直接显示在当前Webview
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            /**
             * H5页面加载完成
             * @param view
             * @param url
             */
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageFinished = true;
                if (errMsg == ERROR_NO_BLUETOOTH || errMsg == ERROR_NO_INDOOR_LOCATE || errMsg == ERROR_NO_LOCATION_INFO) {
                    locationInfo = buildParamJson(0.0, 0.0,
                            null, null, 0.0, 0.0, 0.0, errMsg);
                    sendLocationToH5(locationInfo);
                    return;
                }

            }
        });
    }

    /**
     * 点击返回上一H5页面而不是退出浏览器
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 销毁Webview
     */
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();

            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    /**
     * 封装定位信息
     * @param currentLat 纬度
     * @param currentLon 经度
     * @param indoorBuildingId 建筑ID
     * @param indoorBuildingFloor 楼层ID
     * @param currentAccuracy 精度
     * @param currentDirection 方向
     * @param currentSpeed 移动速度
     * @param errMsg 错误码
     * @return
     */
    private String buildParamJson(double currentLat, double currentLon, String indoorBuildingId, String indoorBuildingFloor,
                                  double currentAccuracy, double currentDirection, double currentSpeed, int errMsg) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("currentLat", currentLat);
        } catch (JSONException e) {
        }
        try {
            jsonObject.put("currentLon", currentLon);
        } catch (JSONException e) {
        }
        try {
            jsonObject.put("indoorBuildingId", indoorBuildingId);
        } catch (JSONException e) {
        }
        try {
            jsonObject.put("indoorBuildingFloor", indoorBuildingFloor);
        } catch (JSONException e) {
        }
        try {
            jsonObject.put("currentAccuracy", currentAccuracy);
        } catch (JSONException e) {
        }
        try {
            jsonObject.put("currentDirection", currentDirection);
        } catch (JSONException e) {
        }
        try {
            jsonObject.put("currentSpeed", currentSpeed);
        } catch (JSONException e) {
        }
        try {
            jsonObject.put("errMsg", errMsg);
        } catch (JSONException e) {
        }
        return jsonObject.toString();
    }

    private void sendLocationToH5(String locationInfo){
        if (version < 18){
            webView.loadUrl("javascript:processIndoorMsg(" + locationInfo + ")");
        }else{
            webView.evaluateJavascript("javascript:processIndoorMsg(" + locationInfo + ")", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                }
            });
        }
    }
}
