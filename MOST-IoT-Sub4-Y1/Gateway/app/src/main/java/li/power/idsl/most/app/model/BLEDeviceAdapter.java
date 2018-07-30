package li.power.idsl.most.app.model;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import li.power.idsl.most.app.R;

import java.util.*;

/**
 * Created by PowerLi on 2017/5/23.
 */
public class BLEDeviceAdapter extends RecyclerView.Adapter {

    public List<BLEDevice> bleDevices = new ArrayList<>();

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ble_device_item, parent, false);
        BLEDeviceViewHolder vh = new BLEDeviceViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        BLEDeviceViewHolder bdHolder = (BLEDeviceViewHolder) holder;
        bdHolder.nameText.setText(bleDevices.get(position).getName());
        bdHolder.addressText.setText(bleDevices.get(position).getAddress());
        bdHolder.rssiText.setText(String.valueOf(bleDevices.get(position).getRssi()));
        bdHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return bleDevices.size();
    }

    public class BLEDeviceViewHolder extends RecyclerView.ViewHolder {
        public TextView nameText;
        public TextView addressText;
        public TextView rssiText;

        public BLEDeviceViewHolder(View v) {
            super(v);
            nameText = (TextView) v.findViewById(R.id.device_name);
            addressText = (TextView) v.findViewById(R.id.device_address);
            rssiText = (TextView) v.findViewById(R.id.device_rssi);
        }
    }

    public void add(BLEDevice item) {
        for (BLEDevice d : bleDevices) {
            if (d.getAddress().equals(item.getAddress())) {
                d.setRssi(item.getRssi());
                return;
            }
        }

        this.bleDevices.add(item);
    }

    public BLEDevice get(int i) {
        return bleDevices.get(i);
    }

    public BLEDevice getDeviceByName(String name)
    {
        for (BLEDevice d:bleDevices) {
            if(d.getName().equals(name))
                return d;

        }
        return null;
    }

    public void clear() {
        this.bleDevices.clear();
    }
}
