package com.example.mybaidulocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private LocationClient mLocationClient;
    private TextView mtextView;

    // 是否是第一次定位
    private boolean isFirstLocate = true;

    // 当前定位模式
    private MyLocationConfiguration.LocationMode locationMode;

    // 数据库相关
    private MyDataBaseHelper helper;
    private SQLiteDatabase writableDB;
    private SQLiteDatabase readableDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 数据库部分，数据库名为traffic
        helper = new MyDataBaseHelper(this,"traffic.db",null,1);
        writableDB = helper.getWritableDatabase();
        readableDB = helper.getWritableDatabase();

        // 先判断表是否为空，如果为空的话执行初始化代码，导入sql
        Cursor cursor = readableDB.query("cell",null,null,null,null,null,null);
        if(!cursor.moveToNext()){
            // 初始化时使用，将cell.sql的数据导入sqlite中
            try {
                System.out.println("执行初始化sql");
                InputStream in = getAssets().open("cell.sql");
                helper.readSqlFile(writableDB,in);
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            requestLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Without Location Permissions!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Got Location Permissions!", Toast.LENGTH_SHORT).show();
                    requestLocation();
                }
                break;
        }
    }

    private void requestLocation() {
        setContentView(R.layout.activity_main);

        //获取地图控件引用
        mMapView = findViewById(R.id.bmapView);

        //获取文本显示控件
        mtextView = findViewById(R.id.text1);

        // 得到地图
        mBaiduMap = mMapView.getMap();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        //定位初始化
        mLocationClient = new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

        //设置locationClientOption
        mLocationClient.setLocOption(option);

        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }

    // 继承抽象类BDAbstractListener并重写其onReceieveLocation方法来获取定位数据，并将其传给MapView
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }

            // 如果是第一次定位
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            if (isFirstLocate) {
                isFirstLocate = false;
                //给地图设置状态
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(ll));
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);


            //获取经纬度
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\n纬度：" + location.getLatitude());
            stringBuilder.append("\n经度："+ location.getLatitude());
            mtextView.setText(stringBuilder.toString());

            // 做查询判断是否拥堵
            // grid[0]=rowId,grid[1]=colId
            int[] grid = translation(location.getLatitude(),location.getLatitude());
            String label = checkLabel(grid[0],grid[1]);
            System.out.println(label);
            Toast.makeText(MainActivity.this,label,Toast.LENGTH_SHORT).show();

        }
    }

    // 根据栅格判断拥堵的标签
    public String checkLabel(int rowId,int colId){
        Cursor cursor = readableDB.query("cell",null,"rowid=? and colid=?",
                new String[]{String.valueOf(rowId),String.valueOf(colId)},null,null,null,null);
        cursor.moveToFirst();
        @SuppressLint("Range")
        String label = cursor.getString(cursor.getColumnIndex("label"));
        String res = null;
        if(label.equals("0"))
            res = "拥挤";
        else if(label.equals("1"))
            res = "缓行";
        else if(label.equals("2"))
            res = "畅通";
        return res;

    }

    // lat纬度，lon经度
    public static int[] translation(double lon, double lat){
        double lon_min=21.7582;
        double lon_max=23.1249;
        double lat_min=113.561;
        double lat_max=114.395;
        int accuracy  = 2000;
        double deltaLon = -(accuracy * 360 / (2 * Math.PI * 6371004 * Math.cos((lat_min + lat_max) * Math.PI / 360)));
        double deltaLat = accuracy * 360 / (2 * Math.PI * 6371004);
        int rowid= (int) Math.floor((lon - lon_min)/deltaLon);
        int colid= (int) Math.floor((lat - lat_min)/deltaLat);
//        System.out.println(rowid);
//        System.out.println(colid);
        return new int[]{rowid,colid};
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();
    }
}