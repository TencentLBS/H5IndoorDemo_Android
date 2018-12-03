package com.example.h5indoormapdemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private Button indoor;
    private BluetoothAdapter mBluetoothAdapter;
    private int errMsg = H5IndoorActivity.ERROR_OK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        indoor = (Button)findViewById(R.id.indoor_open);
        indoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runtimePermissionCheck();
                openBluetooth();
                goToH5IndoorActivity();
            }
        });
    }

    /**
     * 运行时权限检查，Android 6.0及以上
     */
    private void runtimePermissionCheck(){
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_PHONE_STATE);
            }if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(
                        new String[permissions.size()]), 0);
            }
        }
    }

    /**
     * 开启蓝牙
     */
    private void openBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null){
            if (!mBluetoothAdapter.isEnabled()){
                mBluetoothAdapter.enable();
            }
        }
        if (!mBluetoothAdapter.isEnabled()){
            errMsg = H5IndoorActivity.ERROR_NO_BLUETOOTH;
        }
    }

    /**
     * 跳转到H5室内图
     */
    private void goToH5IndoorActivity(){
        Intent intent = new Intent(MainActivity.this, H5IndoorActivity.class);
        intent.putExtra(H5IndoorActivity.ErrMsg, errMsg);
        startActivity(intent);
    }
}
