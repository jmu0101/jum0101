package com.ble_connect;

import static java.lang.Thread.sleep;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.nineone.s_tag_tool.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Connect_D5_Activity extends AppCompatActivity {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "Stag Main";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;

    private UartService mService = null;
    private BluetoothDevice mbluetootDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    private ArrayAdapter<String> listAdapter;

    public static String TagName, StartTime;

    String txHex = "";
    TextView vw_txtmacaddrValue;
    TextView vw_txt_tag_adress;
    EditText tag_type, tag_no;
    EditText tag_copy_type, tag_copy_no;
    Button btn_sendtotag;

    String dataToServer;
    // int delayTime;
    private int connect_fail_count = 0;
    private int rcount = 0;
    private int cetting_count = 0;
    private boolean cetting_boolean = false;
    String Saving_File_name;

    boolean connect_check_flag = false;

    private String[] permissions = {
            //Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
    };
    private static final int MULTIPLE_PERMISSIONS = 101;
    private TextView senser_tag_type_Text = null;
     private Spinner senser_tag_copy_type_Spinner = null;
    private Spinner rfPowerSpinner = null;
    private byte[] tagsend_data5 = new byte[20];
    private boolean connect_fail=false;
    int sensorType = 0;
    private String phonenumber;
    private RequestQueue requestQueue;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_d5);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("D5 ??????");
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();

            finish();
            return;
        }
        if (requestQueue == null) {
            // requestQueue ?????????
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        }


        TelephonyManager phonData = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }
        try {
            phonenumber = phonData.getLine1Number();
            if(phonenumber==null){
                phonenumber = "01000000000";
            }
        }catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

        tagsend_data5[0] = 0x02;
        tagsend_data5[1] = 0x01;
        tagsend_data5[19] = 0x03;
        for(int i=2;i<18;i++){
            tagsend_data5[i]=0x05;
        }
        service_init();
        ui_init();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {                                  // ??????????????? 6.0 ????????? ?????? ????????? ??????
            checkPermissions();
        }
        //tag type setting
        senser_tag_type_Text = findViewById(R.id.tag_type_d5_text);
       /* String[] tag_models = getResources().getStringArray(R.array.tag_type);
        ArrayAdapter<String> tag_spinner_adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.custom_spinner_list, tag_models);
        tag_spinner_adapter.setDropDownViewResource(R.layout.customer_spinner);
        senser_tag_type_Spinner.setAdapter(tag_spinner_adapter);

        senser_tag_type_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {// textView.setText(items[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {//textView.setText("???????????????");
            }
        });
        senser_tag_type_Spinner.setSelection(0);*/
        //RF setting
        rfPowerSpinner = findViewById(R.id.rfPower_d5);
        String[] models = getResources().getStringArray(R.array.my_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.custom_spinner_list, models);
        adapter.setDropDownViewResource(R.layout.customer_spinner);
        rfPowerSpinner.setAdapter(adapter);

        rfPowerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {// textView.setText(items[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {//textView.setText("???????????????");
            }
        });
        rfPowerSpinner.setSelection(0);//tag_copy_type_d5_spinner
        //Sensor ??????.
        senser_tag_copy_type_Spinner = findViewById(R.id.tag_copy_type_d5_spinner);
        String[] tag_models = getResources().getStringArray(R.array.tag_type);
        ArrayAdapter<String> tag_spinner_adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.custom_spinner_list, tag_models);
        tag_spinner_adapter.setDropDownViewResource(R.layout.customer_spinner);
        senser_tag_copy_type_Spinner.setAdapter(tag_spinner_adapter);

        senser_tag_copy_type_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {// textView.setText(items[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {//textView.setText("???????????????");
            }
        });
        senser_tag_copy_type_Spinner.setSelection(0);

        btn_sendtotag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //send....
                if (connect_check_flag) {
                    //
                    byte[] tagsend_data = new byte[20];
                    tagsend_data[0] = 0x02;
                    tagsend_data[1] = 0x01;
                    tagsend_data[19] = 0x03;

                    boolean send_ok_check = true;
                    String tag_type_val = senser_tag_type_Text.getText().toString();
                        /* if (tagtype_val > 0) {
                        tagsend_data[2] = (byte) (tagtype_val );
                        tagsend_data[3] = (byte) (tagtype_val >> 8);
                    }*/
                    if (Integer.parseInt(tag_type_val) > 0) {
                        tagsend_data[2] = (byte) (Integer.parseInt(tag_type_val) );
                        tagsend_data[3] = (byte) (Integer.parseInt(tag_type_val) >> 8);
                    }
                else {
                        send_ok_check = false;
                        customToastView("[????????? ??????] ????????? ?????????.");
                    }
                    //????????? ?????? ?????????
                    String tag_no_var = tag_no.getText().toString().trim();
                    if (tag_no_var.length() > 0) {
                        boolean isNumeric = tag_no_var.matches("[+-]?\\d*(\\.\\d+)?");
                        if (isNumeric) {
                            int tag_no_val = Integer.parseInt(tag_no_var);
                            if (tag_no_val > 0) {
                                tagsend_data[4] = (byte) (tag_no_val);
                                tagsend_data[5] = (byte) (tag_no_val >> 8);
                                tagsend_data[6] = (byte) (tag_no_val >> 16);
                                tagsend_data[7] = (byte) (tag_no_val >> 24);

                            } else {
                                send_ok_check = false;
                                // Toast.makeText(getApplication(), "[????????? ??????] ????????? ???????????? ?????? ????????? ????????????.", Toast.LENGTH_LONG).show();
                                customToastView("[????????? ??????] ????????? ???????????? ?????? ????????? ????????????.");
                            }
                        } else {
                            send_ok_check = false;
                            // Toast.makeText(getApplication(), "[????????? ??????] ????????? ????????? ????????? ??? ????????????", Toast.LENGTH_LONG).show();

                            customToastView("[????????? ??????] ????????? ????????? ????????? ??? ????????????");
                        }
                    } else {
                        send_ok_check = false;
                        // Toast.makeText(getApplication(), "[????????? ??????] ????????? ????????? ?????????", Toast.LENGTH_LONG).show();
                        customToastView("[????????? ??????] ????????? ????????? ?????????");

                    }

                    int rfPower_val = Integer.parseInt(rfPowerSpinner.getSelectedItem().toString());
                    tagsend_data[8] = (byte) rfPower_val;

                    String tag_type_copy_val = (senser_tag_copy_type_Spinner.getSelectedItem().toString());
                    int tagtype_copy_val = 0;
                    switch (tag_type_copy_val) {
                        case "??????":
                            customToastView("[????????? ??????] ??? ????????? ?????????.");
                            Toast.makeText(getApplication(), "[????????? ??????] ??? ????????? ?????????.", Toast.LENGTH_LONG).show();
                            tagtype_copy_val = 0;
                            break;
                        case "00C8":
                            tagtype_copy_val = 0xC8;
                            break;
                        case "00C9":
                            tagtype_copy_val = 0xC9;
                            break;
                        case "00CA":
                            tagtype_copy_val = 0xCA;
                            break;
                        case "0007":
                            tagtype_copy_val = 0x07;
                            break;
                        case "00D5":
                            tagtype_copy_val = 0xD5;
                            break;
                    }
                    if (tagtype_copy_val > 0) {
                        tagsend_data[9] = (byte) (tagtype_copy_val );
                        tagsend_data[10] = (byte) (tagtype_copy_val >> 8);
                    } else {
                        send_ok_check = false;
                        customToastView("[????????? ??????] ????????? ?????????.");
                    }

                    String tag_copy_no_var = tag_no.getText().toString().trim();
                    if (tag_copy_no_var.length() > 0) {
                        boolean isNumeric = tag_copy_no_var.matches("[+-]?\\d*(\\.\\d+)?");
                        if (isNumeric) {
                            int tag_no_val = Integer.parseInt(tag_copy_no_var);
                            if (tag_no_val > 0) {
                                tagsend_data[11] = (byte) (tag_no_val);
                                tagsend_data[12] = (byte) (tag_no_val >> 8);
                                tagsend_data[13] = (byte) (tag_no_val >> 16);
                                tagsend_data[14] = (byte) (tag_no_val >> 24);

                            } else {
                                send_ok_check = false;
                                // Toast.makeText(getApplication(), "[????????? ??????] ????????? ???????????? ?????? ????????? ????????????.", Toast.LENGTH_LONG).show();
                                customToastView("[?????? ????????? ??????] ????????? ???????????? ?????? ????????? ????????????.");
                            }
                        } else {
                            send_ok_check = false;
                            // Toast.makeText(getApplication(), "[????????? ??????] ????????? ????????? ????????? ??? ????????????", Toast.LENGTH_LONG).show();

                            customToastView("[?????? ????????? ??????] ????????? ????????? ????????? ??? ????????????");
                        }
                    } else {
                        send_ok_check = false;
                        // Toast.makeText(getApplication(), "[????????? ??????] ????????? ????????? ?????????", Toast.LENGTH_LONG).show();
                        customToastView("[?????? ????????? ??????] ????????? ????????? ?????????");

                    }
                    tagsend_data[15] = 0;
                    tagsend_data[16] = 0;
                    tagsend_data[17] = 0;
                    tagsend_data[18] = 0;
                    if (send_ok_check) {
                        long countnow = System.currentTimeMillis();
                        SimpleDateFormat aftertime = new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.KOREA);
                        String nowtime = aftertime.format(countnow);
                        String[] DeviceNameArray = mbluetootDevice.getName().trim().split("-");
                        String send_hex_data = asHex(tagsend_data);
                        Log.e("?????? ?????????", send_hex_data);
                        mService.writeRXCharacteristic(tagsend_data);
                        customToastView("?????? ??????");
                        connect_fail = false;
                       String send_data = phonenumber + "," + tag_no.getText().toString().trim() + "," + nowtime;
                        Network_Confirm(send_data);

                    }
                } else {
                    customToastView("????????? ????????? ?????????");


                }
            }
        });
        Runnable runnable10 = new Runnable() {
            @Override
            public void run() {
                //  startScan();

                senser_adress_connect();
            }
        };
        connect_handler.postDelayed(runnable10, 2000);
        // Initialize();
    }
    private boolean mConnecting_true = false;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                //  Intent intent = new Intent(Connect_D5_Activity.this, MainActivity.class);
                //  startActivity(intent);
                finish();
            }

            //  senser_adress_connect();

            //

        }
        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };
    private final MyHandler connect_handler = new MyHandler(this);
    private static class MyHandler extends Handler {
        private final WeakReference<Connect_D5_Activity> mActivity;

        public MyHandler(Connect_D5_Activity activity) {
            mActivity = new WeakReference<Connect_D5_Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Connect_D5_Activity activity = mActivity.get();
            if (activity != null) {

            }
        }
    }

    private void senser_adress_connect(){
        Intent intent = getIntent();
        String sneser_adress_save = intent.getStringExtra("address2");
        Log.e("STag", "351");
        connect_fail=true;
        if (mBtAdapter.isEnabled() && sneser_adress_save != null) {
            if (mService != null) {
                mbluetootDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(sneser_adress_save);
                // mConnecting_true = true;
                mService.connect(sneser_adress_save);
                //    connect_check_flag = true;
            }
        }
    }

    long startDate = System.currentTimeMillis();
    boolean first_only_run = true;
    String result = "";

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e("MyReceiver", "Intent: $intent");
            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        btn_sendtotag.setBackgroundTintList(ContextCompat.getColorStateList(getApplication(), R.color.blue));

                        //  btn_sendtotag.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.blue));
                        btn_sendtotag.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.white));
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");

                        vw_txtmacaddrValue.setText(mbluetootDevice.getName());
                        vw_txt_tag_adress.setText(mbluetootDevice+"");
                        customToastView(mbluetootDevice.getName() + " ??????");

                        try {
                            sleep(1100);
                        } catch (Exception ex) {

                        }
                        //
                        startDate = System.currentTimeMillis();
                        first_only_run = true;
                        result = "";
                        //

                        //  mConnecting_true = false;
                        connect_check_flag = true;
                        Saving_File_name = TagName + "_" + mbluetootDevice.toString().replace(":", "") + "_" + StartTime;

                        dataToServer = "";

                  //      listAdapter.add("[" + currentDateTimeString + "] Connected to: " + mbluetootDevice.getName());
                        invalidateOptionsMenu();
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        // btnConnectDisconnect.setText(R.string.str_connect);
                        btn_sendtotag.setTextColor(ContextCompat.getColor(getApplicationContext(),R.color.gray));
                        //  btn_sendtotag.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.gray3));
                        btn_sendtotag.setBackgroundTintList(ContextCompat.getColorStateList(getApplication(), R.color.gray3));
                        connect_check_flag = false;
                        cetting_boolean = false;
                        // mConnecting_true = false;
                        cetting_count = 0;
                        UI_reset();

                       // listAdapter.add("[" + currentDateTimeString + "] Disconnected to: " + mbluetootDevice.getName());

                        mService.close();
                        //setUiState();


                        //  Toast.makeText(getApplication(), mbluetootDevice.getName() + " ????????????", Toast.LENGTH_SHORT).show();
                        if(connect_fail) {
                            customToastView(mbluetootDevice.getName() + " ????????????");
                        }else{
                            customToastView(mbluetootDevice.getName() + " ????????????");
                        }
                        connect_fail = false;
                        try {
                            sleep(100);
                        } catch (Exception ex) {

                        }
                        invalidateOptionsMenu();

                    }
                });
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
                Log.e("730", String.valueOf(mService.getSupportedGattServices()));
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                //  Log.e("STag","Data rece....");\
                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);

                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            Log.e("ble_datae2",byteArrayToHex(txValue));

                            int receDataLength = txValue.length;
                            Log.e("Tag_Rece12", String.valueOf(receDataLength));
                            if (receDataLength == 20) {
                                Log.e("Tag_Rece1", txValue[0]+", "+txValue[19]);

                                if(cetting_count>30&&!cetting_boolean){
                                    cetting_boolean = true;
                                    customToastView("??????????????? ???????????? ????????????.\n?????? ????????? ????????????.");
                                  /*  Toast toastView = Toast.makeText(getApplication(), "??????????????? ???????????? ????????????.\n?????? ????????? ????????????.", Toast.LENGTH_LONG);
                                    toastView.setGravity(Gravity.CENTER,0,0);
                                    toastView.show();
                                    cetting_boolean=true;*/
                                }
                                cetting_count++;
                                if (txValue[0] == 0x02 && txValue[19] == 0x03) {
                                    rcount++;
                                    cetting_count=0;
                                    cetting_boolean=true;
                                    Log.e("Tag_number753","753");
                                    if (rcount == 1) {
                                        String r_data_hex = asHex(txValue);
                                        int rece_tag_type = ConvertToIntLittle(txValue, 2);
                                        Log.e("Tag_Rece3", txValue[1]+", "+txValue[2]);
                                        String tag_type_str = "";
                                        switch (rece_tag_type) {
                                            case 200:
                                                tag_type_str = "00C8";
                                                break;
                                            case 201:
                                                tag_type_str = "00C9";
                                                break;
                                            case 202:
                                                tag_type_str = "00CA";
                                                break;
                                            case 7:
                                                tag_type_str = "0007";
                                                break;
                                        }
                                        // selectValue(senser_tag_type_Spinner, tag_type_str);
                                        String tag1 = String.format("%02X", rece_tag_type & 0xFF);
                                        String tag2 = String.format("%02X", rece_tag_type>>16 & 0xFF);
                                        String tag3 = tag2 + tag1;
                                        senser_tag_type_Text.setText(tag3);

                                        int rece_tag_no = ConvertToIntLittle2(txValue, 4);
                                        tag_no.setText(rece_tag_no + "");

                                        // selectValue(senser_tag_type_Spinner, tag_type_str);
                                        int rece_rf_power = txValue[8];
                                        selectValue(rfPowerSpinner, rece_rf_power);

                                        int rece_copy_type = ConvertToIntLittle2(txValue, 9);

                                        String C1 = String.format("%02X", rece_copy_type & 0xFF);
                                        String C2 = String.format("%02X", rece_copy_type>>16 & 0xFF);
                                        String C3 = C2 + C1;
                                        selectValue(senser_tag_copy_type_Spinner,C3);

                                        int rece_copy_tag_no = ConvertToIntLittle2(txValue, 11);
                                        tag_copy_no.setText(rece_copy_tag_no + "");

                                    } else {
                                        int rece_cal_status = txValue[7] & 0xff;
                                        Log.e("Cal ?????? ??????", rece_cal_status + "");
                                    }

                                }
                                // Log.e("STag", "End...." + accelerometerX);
                            } else {
                                Log.e(TAG, "BLE DATA Length is " + receDataLength);
                            }

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                Toast.makeText(getApplication(), "????????? UART??? ???????????? ????????????.", Toast.LENGTH_SHORT).show();
                mService.disconnect();
            }
        }
    };
    private void UI_reset(){
        vw_txtmacaddrValue.setText("");
        vw_txt_tag_adress.setText("");
        //tag_type.setText("");

        senser_tag_type_Text.setText("");
        tag_no.setText("0");
        selectValue(rfPowerSpinner, "0");
        selectValue(senser_tag_copy_type_Spinner, "??????");
        tag_copy_no.setText("0");
        // selectValue(senser_tag_type_Spinner, "??????");

    }
    private void selectValue(Spinner spinner, Object value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            //Log.e("RF ??????...",spinner.getItemAtPosition(i).toString());
            if (spinner.getItemAtPosition(i).toString().equals(value + "")) {
                spinner.setSelection(i);
                break;
            }
        }
    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.e("connect_TAG", "onStart()");
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.e("connect_TAG", "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        IntentFilter filter3 = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);//????????? ?????? ?????? ??????
        registerReceiver(mBroadcastReceiver2, filter3);
        Log.e("TAG", "onResume2");
        //invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        Log.e("connect_TAG", "onPause");
        super.onPause();

    }
    @Override
    protected void onStop() {
        Log.e("connect_TAG", "onStop");
        if (mbluetootDevice != null) {
            mService.disconnect();
        }

        super.onStop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("connect_TAG", "onDestroy()");
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        try {

            unregisterReceiver(mBroadcastReceiver2);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        try {
            unbindService(mServiceConnection);
        } catch (java.lang.IllegalArgumentException e)
        {
            //Print to log or make toast that it failed
        }

        if(mService!=null) {
            mService.stopSelf();
            mService = null;
        }

    }
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e("connect_TAG", "onRestart");
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    connect_fail=true;
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mbluetootDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mbluetootDevice + "mserviceValue" + mService);
                    // ((TextView) findViewById(R.id.deviceName)).setText(mbluetootDevice.getName() + " - connecting");
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    //   Intent intent = new Intent(Connect_D5_Activity.this, MainActivity.class);
                    //  startActivity(intent);
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }
    @Override
    public void onBackPressed() {
        if (mbluetootDevice != null) {
            mService.disconnect();
        }
        connect_fail = false;

        finish();

    }


    // Thread ?????????

    SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.connect_menu, menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
       /* if (mConnecting_true) {
            menu.findItem(R.id.action_request).setVisible(true);
            menu.findItem(R.id.action_connect).setVisible(false);
            menu.findItem(R.id.action_disconnect).setVisible(false);
        }else*/ if (connect_check_flag) {
            menu.findItem(R.id.action_request).setVisible(false);
            menu.findItem(R.id.action_connect).setVisible(false);
            menu.findItem(R.id.action_disconnect).setVisible(true);
        }else if(!connect_check_flag){
            menu.findItem(R.id.action_request).setVisible(false);
            menu.findItem(R.id.action_connect).setVisible(true);
            menu.findItem(R.id.action_disconnect).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            //Intent intent = new Intent(Connect_D5_Activity.this, MainActivity.class);
            // startActivity(intent);
            connect_fail = false;
            onBackPressed();
            // finish();
            return true;
        }else if (itemId == R.id.action_connect) {
            connect_fail=true;
            rcount = 0;
            senser_adress_connect();

            return true;
        } else if (itemId == R.id.action_disconnect) {
            connect_fail=false;
            if (mbluetootDevice != null) {
                mService.disconnect();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ui_init() {
      //  listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);

        vw_txtmacaddrValue = (TextView) findViewById(R.id.macaddrValue_d5);
        vw_txt_tag_adress = (TextView) findViewById(R.id.macaddrValue2_d5);
        // intervalTime = (EditText) findViewById(R.id.interval);
        //tag_type = (EditText) findViewById(R.id.tag_type);
        tag_no = (EditText) findViewById(R.id.tag_no_d5);
        tag_no.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.e("onTextChanged",i+", "+i1+", "+i2); }
            @Override
            public void afterTextChanged(Editable editable) {
                Log.e("afterTextChanged",editable.toString());
                if(tag_no.length()==0){
                    tag_no.setText("0");
                }
            }
        });
        tag_copy_no = (EditText) findViewById(R.id.tag_copy_no_d5);
        tag_copy_no.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.e("onTextChanged",i+", "+i1+", "+i2); }
            @Override
            public void afterTextChanged(Editable editable) {
                Log.e("afterTextChanged",editable.toString());
                if(tag_copy_no.length()==0){
                    tag_copy_no.setText("0");
                }
            }
        });
        //tag_ver = (TextView) findViewById(R.id.tag_ver);
        btn_sendtotag = (Button) findViewById(R.id.btn_sendTotag_d5);
    }
    private int ui_T_F=0;
    private void ui_ture_false(int mposition) {
        Log.e("Tag_number71248","1248");
        if(mposition==0){
            ui_T_F=0;
            // rcount=0;
            // UI_reset();
           /* O2alarm.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.gray3));//????????? ??????
            O2alarm.setFocusable(false);//????????????
            O2alarm.setClickable(false);*/

        }else if(mposition == 1){
            ui_T_F=1;

           /* O2alarm.setBackgroundResource(R.drawable.textview_design);//????????? ??????
            O2alarm.setFocusable(true);//????????????
            O2alarm.setClickable(true);
            O2alarm.setFocusableInTouchMode(true);*/

        }else if(mposition == 2){
            ui_T_F=2;

        } else{
            ui_T_F=3;
        }
        Log.e("Tag_number71329","1329");
    }
    private boolean checkPermissions() {
        int result;
        List<String> permissionList = new ArrayList<>();
        for (String pm : permissions) {
            result = ContextCompat.checkSelfPermission(this, pm);
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(pm);
            }
        }
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }else{

        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(this.permissions[i])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showToast_PermissionDeny();
                            }
                        }
                    }

                } else {
                    showToast_PermissionDeny();
                }
                return;
            }
        }
    }
    private void showToast_PermissionDeny() {
        Toast.makeText(this, "?????? ????????? ?????? ???????????? ?????? ???????????????. ???????????? ?????? ?????? ????????? ????????????.", Toast.LENGTH_SHORT).show();
        finish();
    }
    // data Parcing Function
    private short ConvertToShortBig(byte[] txValue, int startidx) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(txValue[startidx]);
        bb.put(txValue[startidx + 1]);
        short shortVal = bb.getShort(0);
        return shortVal;
    }

    private short ConvertToShortLittle(byte[] txValue, int startidx) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(txValue[startidx]);
        bb.put(txValue[startidx + 1]);
        short shortVal = bb.getShort(0);
        return shortVal;
    }


    String tmp;

    public static float arr2float (byte[] arr, int start) {

        int i = 0;
        int len = 4;
        int cnt = 0;
        byte[] tmp = new byte[len];

        for (i = start; i < (start + len); i++) {
            tmp[cnt] = arr[i];
            cnt++;
        }

        int accum = 0;
        i = 0;
        for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
            accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
            i++;
        }
        return Float.intBitsToFloat(accum);
    }
    private int ConvertToIntBig(byte[] txValue, int startidx) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4);
        // by choosing big endian, high order bytes must be put
        // to the buffer before low order bytes
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        // since ints are 4 bytes (32 bit), you need to put all 4, so put 0
        // for the high order bytes
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put(txValue[startidx]);
        byteBuffer.put(txValue[startidx + 1]);
        byteBuffer.flip();
        int result = byteBuffer.getInt();
        return result;
    }
    private int ConvertToIntLittle2(byte[] txValue, int startidx) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8);
        // by choosing big endian, high order bytes must be put
        // to the buffer before low order bytes
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        // since ints are 4 bytes (32 bit), you need to put all 4, so put 0

        byteBuffer.put(txValue[startidx]);
        byteBuffer.put(txValue[startidx + 1]);
        byteBuffer.put(txValue[startidx + 2]);
        byteBuffer.put(txValue[startidx + 3]);
        // for the high order bytes
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0x00);
        byteBuffer.flip();
        int result = byteBuffer.getInt();
        return result;
    }
    private int ConvertToIntLittle(byte[] txValue, int startidx) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4);
        // by choosing big endian, high order bytes must be put
        // to the buffer before low order bytes
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        // since ints are 4 bytes (32 bit), you need to put all 4, so put 0
        // for the high order bytes
        byteBuffer.put(txValue[startidx]);
        byteBuffer.put(txValue[startidx + 1]);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0x00);

        byteBuffer.flip();
        int result = byteBuffer.getInt();
        return result;
    }
    public static String asHex(byte bytes[]) {
        if ((bytes == null) || (bytes.length == 0)) {
            return "";
        }

        // ?????????????????????????????????????????????????????????????????????
        StringBuffer sb = new StringBuffer(bytes.length * 2);

        // ?????????????????????????????????????????????????????????
        for (int index = 0; index < bytes.length; index++) {
            // ????????????????????????????????????
            int bt = bytes[index] & 0xff;

            // ???????????????0x10??????????????????
            if (bt < 0x10) {
                // 0x10??????????????????????????????????????????0????????????
                sb.append("0");
            }

            // ???????????????16?????????????????????????????????????????????????????????????????????
            if(bytes.length == index + 1){
                sb.append(Integer.toHexString(bt).toUpperCase());
            }else{
                sb.append(Integer.toHexString(bt).toUpperCase()+"-");
            }

        }

        /// 16??????????????????????????????
        return sb.toString();
    }
    public String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for(final byte b: a)
            sb.append(String.format("%02x ", b&0xff));
        return sb.toString();
    }

    private void writeLog(String data) {//csv?????? ??????
        // String str_Path = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+ "Nineone"+ File.separator;
        File file;// = new File(str_Path);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator+ "Nineone"+ File.separator);
        } else {
            file = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+ "Documents"+ File.separator+ "Nineone"+ File.separator );

        }

        if (!file.exists()) {
            file.mkdirs();
        }
        String str_Path_Full;
        //String str_Path_Full = Environment.getExternalStorageDirectory().getAbsolutePath();
        //str_Path_Full += "/Nineone" + File.separator + "test.csv";
        File file2 ;//= new File(str_Path_Full);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            str_Path_Full = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath()  + File.separator+ "Nineone"+ File.separator + "test.csv";
            file2 = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator+ "Nineone"+ File.separator,"test.csv");

        } else {
            str_Path_Full = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+ "Documents"+ File.separator+ "Nineone"+ File.separator + "test.csv";
            file2 = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+ "Documents"+ File.separator+ "Nineone"+ File.separator , "test.csv");
        }
        if (!file2.exists()) {
            try {
                file2.createNewFile();
            } catch (IOException ignored) {
            }
        }

        try {
            BufferedWriter bfw;

            bfw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(str_Path_Full,true),  "EUC-KR"));
            bfw.write(data + "\r\n");
            //bfw.write(log_data);
            bfw.flush();
            bfw.close();
            Log.e("TAGddd", "ddd");
        } catch (FileNotFoundException e) {
            Log.e("TAGddd", e.toString());
        } catch (IOException e) {
            Log.e("TAGddd", e.toString());
        }
    }

    private void Network_Confirm(String Network_data){
        int status = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(status == NetworkStatus.TYPE_MOBILE){
            Http_post(Network_data);
            Log.e("???????????? ?????????","650");
        }else if (status == NetworkStatus.TYPE_WIFI){
            Http_post(Network_data);
            Log.e("??????????????? ?????????","652");
        }else {
            writeLog(Network_data);
            Log.e("?????? ??????.","654");
        }
    }


    public void Http_post(String post_data) {
        String[] split_data = post_data.trim().split(",");
        new Thread(() -> {
            try {
                String url = "http://stag.nineone.com:8002/si/rece_Setting.asp";
                URL object = null;
                object = new URL(url);

                HttpURLConnection con = null;

                con = (HttpURLConnection) object.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Authorization", "Bearer Key");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestMethod("POST");
                JSONArray array = new JSONArray();

                JSONObject cred = new JSONObject();
                try {
                    cred.put("id", split_data[0]);
                    cred.put("device_idx", split_data[1]);
                    cred.put("time", split_data[2]);
                    cred.put("sensor_margin", split_data[3]+","+ split_data[4]+","+ split_data[5]+","+ split_data[6]+","+ split_data[7]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                array.put(cred);
                OutputStream os = con.getOutputStream();
                os.write(array.toString().getBytes("UTF-8"));
                os.close();
//display what returns the POST request

                StringBuilder sb = new StringBuilder();
                int HttpResult = con.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), "utf-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    Log.e("dd-", "\n" + sb.toString());
                } else {
                    Log.e("dd-", "\n" + con.getResponseMessage());
                    //   System.out.println(con.getResponseMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    ArrayList<Tag_tiem> arrayList = new ArrayList<>();
    private void ReadTextFile() {//csv ?????? ????????? ????????????
        try {
            String str_Path_Full;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                str_Path_Full = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+ File.separator+ "Nineone"+ File.separator + "test.csv";
                // file2 = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator+ "Nineone"+ File.separator,"test.csv");
            } else {
                str_Path_Full = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+ "Documents"+ File.separator+ "Nineone"+ File.separator + "test.csv";

                // file2 = new File(Environment.DIRECTORY_DOCUMENTS + File.separator+ "Nineone"+ File.separator + "test.csv");
            }
            FileInputStream is = new FileInputStream(str_Path_Full);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "EUC-KR"));
            // BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = "";
            int row=0;
            Log.e("aaa", "line");
            while ((line = reader.readLine()) != null) {//?????? ????????? ????????? ??????
                String[] token = line.split("\\,", -1);

                Log.e("aaa2", String.valueOf(row));
                String sensor_margin=token[3]+","+token[4]+","+token[5]+","+token[6]+","+token[7];
                arrayList.add(row,new Tag_tiem(token[0],token[1],token[2],sensor_margin));
                Log.e("aaa3", String.valueOf(row));
                row++;

            }

            reader.close();
            is.close();
            Http_Array_post(arrayList);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("aaa5", String.valueOf(arrayList));

        }
        //return strBuffer.toString();
    }
    public void Http_Array_post(ArrayList<Tag_tiem> array_List){

        new Thread(() -> {
            try {

                String url="http://stag.nineone.com:8002/si/rece_Setting.asp";
                URL object= null;
                object = new URL(url);
                HttpURLConnection con = null;

                con = (HttpURLConnection) object.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Authorization", "Bearer Key");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestMethod("POST");
                JSONArray array=new JSONArray();

                for(int i=0;i<array_List.size();i++){
                    JSONObject cred = new JSONObject();
                    try {
                        cred.put("id", array_List.get(i).getId());
                        cred.put("device_idx",array_List.get(i).getDevice_idx());
                        cred.put("time", array_List.get(i).getTime());
                        cred.put("sensor_margin", array_List.get(i).getSensor_margin());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    array.put(cred);
                }
                OutputStream os = con.getOutputStream();
                os.write(array.toString().getBytes("UTF-8"));
                os.close();
//display what returns the POST request

                StringBuilder sb = new StringBuilder();
                int HttpResult = con.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), "utf-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    Log.e("dd-","\n"+sb.toString());
                    fileDelete();
                } else {
                    Log.e("dd-","\n"+con.getResponseMessage());
                    //   System.out.println(con.getResponseMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }
    private static boolean fileDelete(){
        // String str_Path_Full = Environment.getExternalStorageDirectory().getAbsolutePath();
        // str_Path_Full += "/Nineone" + File.separator + "test.csv";

        try {
            File file;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //     str_Path_Full = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator+ "Nineone"+ File.separator + "test.csv";
                file = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator+ "Nineone"+ File.separator,"test.csv");
            } else {
                //     str_Path_Full = Environment.DIRECTORY_DOCUMENTS + File.separator+ "Nineone"+ File.separator + "test.csv";
                file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+ "Documents" +File.separator+"Nineone"+ File.separator + "test.csv");
            }
            if(file.exists()){
                file.delete();
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Bundle extras = intent.getExtras();
            NetworkInfo info = (NetworkInfo) extras.getParcelable("networkInfo");
            NetworkInfo.State networkstate = info.getState();
            Log.d("TEST Internet", info.toString() + " " + networkstate.toString());
            if (networkstate == NetworkInfo.State.CONNECTED) {
                Log.e("??????????????? ?????????","652");
                //  Toast.makeText(activity.getApplication(), "Internet connection is on", Toast.LENGTH_LONG).show();
                ReadTextFile();
            } else {
                Log.e("??????????????? ?????????","652");
                //   Toast.makeText(activity.getApplication(), "Internet connection is Off", Toast.LENGTH_LONG).show();
            }
        }
    };
    public void customToastView(String text){

        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.connect_success_toast, (ViewGroup) findViewById(R.id.Send_toast));
        TextView textView = layout.findViewById(R.id.toast);
        textView.setText(text);

        Toast toastView = Toast.makeText(getApplicationContext(),text, Toast.LENGTH_SHORT);
        toastView.setGravity(Gravity.CENTER,0,50);
        toastView.setView(layout);
        toastView.show();
    }
}
