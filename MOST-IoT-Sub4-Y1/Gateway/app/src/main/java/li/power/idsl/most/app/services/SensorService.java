package li.power.idsl.most.app.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import li.power.idsl.most.app.DeviceActivity;
import li.power.idsl.most.app.sensor.Sensor;

/**
 * Created by PowerLi on 2017/5/23.
 */
public class SensorService extends Service{

    public static SensorService instance;

    public static SensorService getInstance()
    {
        return instance;
    }

    public Sensor[] sensors=new Sensor[2];

    private BluetoothAdapter mBluetoothAdapter;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        instance=this;

        NotificationManager notificationManger = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        intent= new Intent();
        intent.setClass(this, DeviceActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentTitle("Sensor Service")
                .setContentText("Service is now running...")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        notification.flags = Notification.FLAG_NO_CLEAR;
        notificationManger.notify(1, notification);
        startForeground(1, notification);
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        scanLeDevice(true);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("Service","bye");
        super.onDestroy();
    }
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mBluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
        } else {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(result.getDevice().getAddress().equals("74:DA:EA:B1:FA:DC"))
            {
                if(getInstance().sensors[0]==null) {
                    getInstance().sensors[0] = new Sensor(result.getDevice(), getApplicationContext(), 0);
                    getInstance().sensors[0].start();
                    Log.d("1","NEW");

                }
            }
            if(result.getDevice().getAddress().equals("00:15:83:00:3E:B0"))
            {
                if(getInstance().sensors[1]==null) {
                    getInstance().sensors[1] = new Sensor(result.getDevice(), getApplicationContext(), 1);
                    getInstance().sensors[1].start();
                    Log.d("2","NEW");

                }
            }
        }
    };

}
