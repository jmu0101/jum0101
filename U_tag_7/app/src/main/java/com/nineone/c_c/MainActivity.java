package com.nineone.c_c;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nineone.c_c.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private LocationManager locationManager;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_ENABLE_BT2 = 3;

    private TextView textAcc, textGyr, textBaro;
    private  TextView tv;
    private static SensorManager mSensorManager;
    private Sensor mAccelerometer; // 가속도 센스
    private Sensor mGyroerometer; // 자이로 센스
    private Sensor mMagnetometer; // 자력계 센스
    private Sensor mBarometer;
    // Used to load the 'c_c' library on application startup.
    static {
        System.loadLibrary("c_c");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);//ble 상태 감지 필터
        registerReceiver(mBroadcastReceiver1, filter1);
        IntentFilter filter2 = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);//gps 상태감지 필터
        registerReceiver(mBroadcastReceiver1, filter2);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Example of a call to a native method
        tv = binding.sampleText;

        textAcc = findViewById(R.id.textViewAcc);
        textGyr = findViewById(R.id.textViewGyr);
        textBaro = findViewById(R.id.textViewBaro);
        Log.e("onre","onre");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//가속도
        mGyroerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);//자력계
        mBarometer = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);//기압계
        Log.e("onre1","onre1");
       // boolean chk1 = mSensorManager.registerListener(listener, mAccelerometer,SensorManager.SENSOR_DELAY_UI);
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null){
            textAcc.setText("가속도 센서를 지원하지 않습니다.");
        }
       // boolean chk2 = mSensorManager.registerListener(listener, mGyroerometer,SensorManager.SENSOR_DELAY_UI);
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null){
            textGyr.setText("자이로 센서 지원하지 않음");
        }
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) == null){
           // textBaro.setText("기압계 센서 지원하지 않음");
        }
        for(int a=0; a<6;a++){
            arrayfloat[a]=1;
        }

        bluetoothCheck();
    }

    /**
     * A native method that is implemented by the 'c_c' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native String stringFromJN0(String format, String format1, String format2);
    public native float[] Utag_Arrary(float accx,float accy,float accz,float gyrox,float gyroy,float gyroz);

    // for radian -> dgree
    private double RAD2DGR = 180 / Math.PI;
    private static final float NS2S = 1.0f/1000000000.0f;
    private float[] arrayfloat=new float[6];
    float[] fasf = new float[3];
  /*  @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
          //  textAcc.setText("가속도 센서값\nx: " + String.format("%.4f", sensorEvent.values[0]) + "\ny: " + String.format("%.4f", sensorEvent.values[1])+ "\nz: " + String.format("%.4f", sensorEvent.values[2]));
            tv.setText(stringFromJN0(String.format("%.4f", sensorEvent.values[0]),String.format("%.4f", sensorEvent.values[1]),String.format("%.4f", sensorEvent.values[2])));
            arrayfloat[0]=sensorEvent.values[0];
            arrayfloat[1]=sensorEvent.values[1];
            arrayfloat[2]=sensorEvent.values[2];
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
           // textGyr.setText("\n자이로 센서값\nx: " + String.format("%.4f", sensorEvent.values[0]) + "\ny: " + String.format("%.4f", sensorEvent.values[1]) + "\nz: " + String.format("%.4f", sensorEvent.values[2])+ "\n");
            arrayfloat[3]=sensorEvent.values[0];
            arrayfloat[4]=sensorEvent.values[1];
            arrayfloat[5]=sensorEvent.values[2];
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE) {//기압
            long timestamp = sensorEvent.timestamp;
            float presure = sensorEvent.values[0];
            presure = (float) (Math.round(presure*100)/100.0); //소수점 2자리 반올림
            //기압을 바탕으로 고도를 계산(맞는거 맞아???)
            float height = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, presure);
            //textBaro.setText("기압계 센서값\nx: " + String.format("%.4f", presure) +" hPa \n 고도: "+height+"m" );

        }
        Log.e("onAccuracyChanged",arrayfloat[0]+","+ arrayfloat[1]+","+  arrayfloat[2]+","+  arrayfloat[3]+","+  arrayfloat[4]+","+  arrayfloat[5]);
        fasf = Utag_Arrary(arrayfloat[0], arrayfloat[1], arrayfloat[2], arrayfloat[3], arrayfloat[4], arrayfloat[5]);

        textBaro.setText("  "+fasf[0]+" , "+fasf[1]+" , "+fasf[2]);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
       Log.e("onAccuracyChanged","onAccuracyChanged");
    }*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    private void bluetoothCheck(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 지원하지 않는다면 어플을 종료시킨다.
        if(mBluetoothAdapter == null){
            Toast.makeText(this, "이 기기는 블루투스 기능을 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if(!mBluetoothAdapter.isEnabled()){
            Log.e("BLE1245", "124");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }else{
            ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo  service  : manager.getRunningServices(Integer.MAX_VALUE)) {
                Log.e("onResume", service.service.getClassName());
                if (!"com.nineone.c_c.background_Service".equals(service.service.getClassName())) {
                    Log.e("onResume", "onResume2");
                    startService();
                }else{
                    startForeground=true;
                }
            }
            startService();
        }
        Log.e("BLE1245", "130");
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                Log.e("BLE1355", "355");
                if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                    Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();
                    ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                    for (ActivityManager.RunningServiceInfo  service  : manager.getRunningServices(Integer.MAX_VALUE)) {
                        Log.e("onResume", service.service.getClassName());
                        if (!"com.nineone.c_c.background_Service".equals(service.service.getClassName())) {
                            Log.e("onResume", "onResume2");
                            startService();
                        }else{
                            startForeground=true;
                        }
                    }
                    startService();
                    //         mblecheck = true;
                } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                    //       mblecheck=false;
                    Toast.makeText(this, "블루투스를 활성화 하여 주세요 ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_ENABLE_BT2:
                Log.e("BLE1366", "366");
                if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                    Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();
                } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                    Toast.makeText(this, "블루투스를 활성화 하여 주세요 ", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private boolean startForeground = false;
    @Override
    protected void onResume() {
        super.onResume();
        Log.e("onResume", "onResume");
        String target = "c_c" + "." + background_Service.class;
    //    mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
     //   mSensorManager.registerListener(this, mGyroerometer, SensorManager.SENSOR_DELAY_UI);
     //   mSensorManager.registerListener(this, mBarometer, SensorManager.SENSOR_DELAY_NORMAL);

       // bluetoothCheck();

      //  Intent serviceIntent = new Intent(MainActivity.this, background_Service.class);
      //  serviceIntent.setAction("startForeground");
      //  stopService(serviceIntent);
        // mBluetoothLeScanner.startScan(btLeScanFilters, btLeScanSettings,leScanCallback);
    }
    @Override
    protected void onPause() {
        // stopScan();
      //  mIsScanning=false;
      //  mSensorManager.unregisterListener(this);
        super.onPause();
        Log.e("onPause","onPause");
    }
    @Override
    protected void onStop() {
        super.onStop();
      //  blesendcheck.setText("중지");
      //  stopScan();
        Log.e("onStop","onStop");
    }
    private void startService(){
        if(!startForeground){
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                startForeground=true;
                Log.e("startService", "startService");
                mIsScanning=true;
                invalidateOptionsMenu();
                Intent serviceIntent1 = new Intent(MainActivity.this, background_Service.class);
                serviceIntent1.setAction("startForeground");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent1);
                } else {
                    startService(serviceIntent1);
                }
            }else{
                Toast.makeText(getApplication(), "GPS를 활성화 하여 주세요 ", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    private void stopService(){
        if(startForeground) {
            startForeground = false;
            mIsScanning = false;
            invalidateOptionsMenu();
            Log.e("stopService", "stopService");
            Intent serviceIntent1 = new Intent(MainActivity.this, background_Service.class);
            serviceIntent1.setAction("startForeground");
            stopService(serviceIntent1);

        }
    }
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                Log.e("off1", action);
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                       // startForeground=false;
                        stopService();
                        //Intent enableIntent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
                        //startActivityForResult(enableIntent, REQUEST_ENABLE_BT3);
                      //  stopScan();
                      //  mblecheck = false;
                        Toast.makeText(getApplication(), "블루투스가 종료되었습니다.\n 블루투스를 실행시켜 주세요 ", Toast.LENGTH_SHORT).show();
                      //  blesendcheck.setText("중지 (블루투스가 종료)");
                        Log.e("off1", "off1");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.e("off2", "off2");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.e("off3", "off3");
                        startService();
                        // mblecheck = true;
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.e("off4", "off4");
                        break;
                    default:
                        Log.e("off5", String.valueOf(state));
                        break;
                }
            }
            if (action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                Log.e("off6", action + ", " + intent);

            }
            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {

                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                if (isGpsEnabled || isNetworkEnabled) {
                    startService();
                    Log.e("off7", String.valueOf(isGpsEnabled));
                } else {
                    stopService();
                  //  mblecheck = false;
                    Toast.makeText(getApplication(), "GPS가 종료되었습니다.\n GPS를 실행시켜 주세요 ", Toast.LENGTH_SHORT).show();
                   // blesendcheck.setText("중지 (GPS가 종료)");
                    Log.e("off8", String.valueOf(isGpsEnabled));
                }
            }
        }
    };
    private boolean mIsScanning = false;
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsScanning) {
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
            menu.findItem(R.id.action_clear).setVisible(false);
            menu.findItem(R.id.action_back).setVisible(false);
        } else {
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
            menu.findItem(R.id.action_clear).setVisible(false);
            menu.findItem(R.id.action_back).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }
    @SuppressWarnings("unchecked")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // ignore
            return true;
        }else if (itemId == R.id.action_scan) {

            startService();
            return true;
        } else if (itemId == R.id.action_stop) {
          stopService();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("종료 확인");
        builder.setMessage("정말로 종료하시겠습니까?");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }
}