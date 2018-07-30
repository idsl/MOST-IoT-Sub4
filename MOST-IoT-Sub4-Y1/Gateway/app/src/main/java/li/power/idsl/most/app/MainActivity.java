package li.power.idsl.most.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import li.power.idsl.most.app.model.BLEDevice;
import li.power.idsl.most.app.model.BLEDeviceAdapter;
import li.power.idsl.most.app.services.SensorService;

public class MainActivity extends AppCompatActivity {


    private BluetoothAdapter mBluetoothAdapter;

    private final static int REQUEST_ENABLE_BT = 1;

    private static final long SCAN_PERIOD = 10000;

    BLEDeviceAdapter bleDeviceAdapter;
    public static BluetoothDevice[] device = new BluetoothDevice[2];


    static boolean permissionCallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_title);
        requestPermission(this);

//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }
//        bleDeviceAdapter=new BLEDeviceAdapter();
//        final RecyclerView bleDeviceList = (RecyclerView) findViewById(R.id.ble_device_list);
//        bleDeviceList.setAdapter(bleDeviceAdapter);
//        bleDeviceList.setLayoutManager(new LinearLayoutManager(this));
//
//        scanLeDevice(true);
//
//        Button btnConnect=(Button)findViewById(R.id.btn_connect);
//        btnConnect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent i=new Intent(getApplicationContext(),DeviceActivity.class);
//                startActivity(i);
//            }
//        });
//        if(!checkPermission())
//            startService(new Intent().setClass(this, SensorService.class));

    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mBluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
        } else {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        }
    }

    void requestPermission(MainActivity activity) {
        if (checkPermission()) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        } else {
            startService(new Intent().setClass(this, SensorService.class));
            Intent i = new Intent(getApplicationContext(), DeviceActivity.class);
            startActivity(i);
            finish();
        }

    }

    boolean checkPermission() {
        return ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED;
    }


    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getDevice().getAddress().equals("74:DA:EA:B1:FA:DC") || result.getDevice().getAddress().equals("00:15:83:00:3E:B0")) {
                bleDeviceAdapter.add(new BLEDevice(result.getScanRecord().getDeviceName()
                        , result.getDevice().getAddress()
                        , result.getRssi()
                        , result.getDevice()));
                bleDeviceAdapter.notifyDataSetChanged();
            }


        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        Log.d("PER", String.valueOf(requestCode));
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent mStartActivity = new Intent(getApplicationContext(), MainActivity.class);
                    int mPendingIntentId = 123456;
                    PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis(), mPendingIntent);
                    finish();
                } else {

                    finish();
                }
                return;
            }
        }
    }
}
