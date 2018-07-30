package li.power.idsl.most.app.sensor;

import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import li.power.idsl.most.algorithm.Algorithm;
import li.power.idsl.most.algorithm.Hash;
import li.power.idsl.most.app.model.SensorData;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import static li.power.idsl.most.algorithm.Hash.getSha384;

/**
 * Created by PowerLi on 2017/5/29.
 */
public class Sensor implements Serializable {


    public static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_CHARACTERISTIC_UUID = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb");


    Context service;

    BluetoothGattCharacteristic txChar;
    BluetoothGattCharacteristic rxChar;

    public BluetoothDevice device;
    BluetoothGatt mGatt;

    private int sensorId=0;

    private int[] sensor = null;

    private byte[] authData;
    private byte[] sensorData;
    private boolean runningAuth1 = false;
    private boolean authing = false;

    private boolean writing = false;
    public String authStatus="";
    private long authProcessTime=0;

    public Sensor(BluetoothDevice device, Context service,int id)
    {
        this.device=device;
        this.service=service;
        this.sensorId=id;
    }

    public void start()
    {
        device.connectGatt(service,true,gattCallback);
    }

    public void authThread()
    {
        authProcessTime=System.currentTimeMillis();
        authing = true;
        runningAuth1 = true;
        txChar.setValue(new byte[]{0x00, 0x33});
        Log.d(device.getName()+"-AUTH","Init command sent");
        mGatt.writeCharacteristic(txChar);
        while (runningAuth1 && authing) ;

        if(!authing)
            return;

        updateAuthStatus("Auth1 done");
        Log.d(device.getName()+"-AUTH", "Phase 2 Start");
        byte[] toSensor = Algorithm.phase2(device.getName(),sensor[0], sensor[1], sensor[2]);
        int len = toSensor.length;
        for (int i = 0; i < len; i += 20) {
            if (len - i > 20) {
                txChar.setValue(Arrays.copyOfRange(toSensor, i, i + 20));
            } else {
                txChar.setValue(Arrays.copyOfRange(toSensor, i, len));
            }
            writing = true;
            mGatt.writeCharacteristic(txChar);
            while (writing) ;
            Log.d(device.getName()+"-AUTH", "SENT");
        }
        updateAuthStatus("Auth2 SENT");

    }

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("STATUS", String.valueOf(status));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateAuthStatus("Connect success");
                gatt.discoverServices();
                mGatt = gatt;

            }
            else if(status==BluetoothGatt.STATE_CONNECTING)
            {
                updateAuthStatus("Connecting");
            }
            else if(status==8) {
                updateAuthStatus("Disconnected");
                authing = true;
                //device.connectGatt(service,true,this);

            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d("GATT","DW OK");
            new Thread(new Runnable() {
                @Override
                public void run() {

                    authThread();
                }
            }).start();

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            updateAuthStatus("Service discovered");
            txChar = gatt.getService(SERVICE_UUID).getCharacteristic(TX_CHARACTERISTIC_UUID);
            rxChar = gatt.getService(SERVICE_UUID).getCharacteristic(RX_CHARACTERISTIC_UUID);

            if(rxChar==null)
                rxChar=txChar;

            gatt.setCharacteristicNotification(rxChar, true);
            Log.d("RXTX",rxChar.toString()+", "+txChar.toString());
            UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
            BluetoothGattDescriptor descriptor = rxChar.getDescriptor(uuid);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] data = characteristic.getValue();
            characteristic.setValue(new byte[]{0});
            gatt.writeCharacteristic(characteristic);

            if (authing) {

                if (data[0] == 0x33 && data[1] == 0x66) {
                    authData = new byte[93];
                }

                if (runningAuth1) {

                    for (int i = 0; i < data.length; i++) {

                        authData[i + authData[92]] = data[i];
                    }
                    authData[92] += data.length;
                    if (authData[92] == 92) {
                        sensor = Algorithm.phase1(device.getName(),authData);
                        runningAuth1 = false;
                    }
                } else {
                    Log.d("Auth",Hash.bytesToHex(data).substring(0,4));
                    if (Hash.bytesToHex(data).substring(0,4).equals("ff00")) {
                        updateAuthStatus("Auth success");

                        handler.postDelayed(runnable,1000);

                    } else {
                        updateAuthStatus("Auth failed");
                    }
                    authProcessTime=System.currentTimeMillis()-authProcessTime;
                    authing = false;
                }
            }
            else
            {

                if (data[0] == 0x44) {
                    sensorData = new byte[12];
                }
                StringBuilder b=new StringBuilder();
                for (int i = 0; i < data.length; i++) {
                    b.append(String.format("%02x",data[i]));
                    sensorData[i+sensorData[11]] = data[i];
                }

                sensorData[11] += data.length;
                if (sensorData[11] == 11) {
                    updateValue();
                }
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            writing = false;
        }
    };

    private Handler handler = new Handler( );

    private Runnable runnable = new Runnable( ) {
        public void run ( ) {
            if(!authing) {
                txChar.setValue(new byte[]{0x44, 0x33});
                mGatt.writeCharacteristic(txChar);
                handler.postDelayed(this, 1000);
            }
        }
    };


    void updateValue()
    {
        byte[] b1 =Arrays.copyOfRange(sensorData,1,5);
        byte[] b2 =Arrays.copyOfRange(sensorData,5,9);
        float f1 = ByteBuffer.wrap(b1).order(ByteOrder.nativeOrder()).getFloat();
        float f2 = ByteBuffer.wrap(b2).order(ByteOrder.nativeOrder()).getFloat();
        Intent updateIntent=new Intent();
        updateIntent.putExtra("data",new float[]{f1,f2});
        updateIntent.putExtra("name",device.getName());
        updateIntent.putExtra("auth",authStatus);
        updateIntent.putExtra("time",authProcessTime);

        updateIntent.putExtra("sensor",sensorId);
        updateIntent.setAction("li.power.idsl.most.app.SENSOR_UPDATE");
        service.sendBroadcast(updateIntent);
        String databaseUrl = "jdbc:mysql://140.118.19.95:3306/iot_trust";
        try {
            ConnectionSource connectionSource = new JdbcConnectionSource(databaseUrl,"root","root");
            Dao<SensorData,String> sensorDataDao = DaoManager.createDao(connectionSource, SensorData.class);

            if(sensorId==0) {
                sensorDataDao.create(new SensorData(getSha384(device.getAddress()), 1, f1,"Temperature Sensor"));
                sensorDataDao.create(new SensorData(getSha384(device.getAddress()), 2, f2,"Humidity Sensor"));
            }
            else
            {
                sensorDataDao.create(new SensorData(getSha384(device.getAddress()), 3, f1,"Pulse Oximetry"));
                sensorDataDao.create(new SensorData(getSha384(device.getAddress()), 4, f2,"Heart-Rate Sensor"));
            }

            connectionSource.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void updateAuthStatus(final String txt) {

        Intent authUpdateIntent=new Intent();
        authUpdateIntent.putExtra("data",txt);
        authUpdateIntent.putExtra("name",device.getName());

        authUpdateIntent.putExtra("sensor",sensorId);
        authUpdateIntent.setAction("li.power.idsl.most.app.AUTH_UPDATE");
        authStatus=txt;
        service.sendBroadcast(authUpdateIntent);
    }
}
