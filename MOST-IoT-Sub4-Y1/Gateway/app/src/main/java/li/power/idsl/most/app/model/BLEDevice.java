package li.power.idsl.most.app.model;

import android.bluetooth.BluetoothDevice;

/**
 * Created by PowerLi on 2017/5/23.
 */
public class BLEDevice {
    private String name;
    private String address;
    private int rssi;

    private BluetoothDevice device;

    public BLEDevice(String name, String address,int rssi,BluetoothDevice device) {
        this.name = name;
        this.address = address;
        this.device=device;
        this.rssi=rssi;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
