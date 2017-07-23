package li.power.idsl.most.app;

import android.app.Service;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import li.power.idsl.most.algorithm.Algorithm;
import li.power.idsl.most.algorithm.Hash;
import li.power.idsl.most.app.model.SensorValue;
import li.power.idsl.most.app.model.SensorValueAdapter;
import li.power.idsl.most.app.sensor.Sensor;
import li.power.idsl.most.app.services.SensorService;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DeviceActivity extends AppCompatActivity {
//    TextView[] sensorName=new TextView[2];
//    TextView[] sensorAuth=new TextView[2];
//    TextView[][] sensorValue=new TextView[2][2];
//    TextView[] sensorAuthTime=new TextView[2];

    //Sensor[] sensors=new Sensor[2];

    SensorValueAdapter sensorValueAdapter=new SensorValueAdapter();
    RecyclerView sensorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

//        LinearLayout[] sensorLayout={
//                        (LinearLayout)findViewById(R.id.layout_sensor1),
//                        (LinearLayout)findViewById(R.id.layout_sensor2)};
//
        for(int i=0;i<2;i++)
        {
//            sensorName[i]=(TextView) sensorLayout[i].findViewById(R.id.sensor_name);
//            sensorAuth[i]=(TextView) sensorLayout[i].findViewById(R.id.sensor_auth);
//            sensorValue[i][0]=(TextView) sensorLayout[i].findViewById(R.id.sensor_value1);
//            sensorValue[i][1]=(TextView) sensorLayout[i].findViewById(R.id.sensor_value2);
//            sensorAuthTime[i]=(TextView) sensorLayout[i].findViewById(R.id.sensor_auth_time);
//
            sensorValueAdapter.addSensorValueObject(new SensorValue());

        }
        sensorView=(RecyclerView) findViewById(R.id.sensor_value_list);
        sensorView.setAdapter(sensorValueAdapter);
        sensorView.setLayoutManager(new LinearLayoutManager(this));
//        sensorView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        IntentFilter updateFilter=new IntentFilter();
        updateFilter.addAction("li.power.idsl.most.app.SENSOR_UPDATE");
        updateFilter.addAction("li.power.idsl.most.app.AUTH_UPDATE");
        registerReceiver(new SensorUpdateListener(),updateFilter);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public class SensorUpdateListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {

            final int sensorId=intent.getIntExtra("sensor",0);
            if(intent.getAction().equals("li.power.idsl.most.app.AUTH_UPDATE"))
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        sensorAuth[sensorId].setText(intent.getStringExtra("data"));
                        SensorValue sv=sensorValueAdapter.getSensorValueObject(sensorId);
                        sv.setAuthStatus(intent.getStringExtra("data"));
                        sensorValueAdapter.notifyDataSetChanged();
                    }
                });
            }
            else if(intent.getAction().equals("li.power.idsl.most.app.SENSOR_UPDATE"))
            {
                final float[] data=intent.getFloatArrayExtra("data");
                final String name=intent.getStringExtra("name");
                final String auth=intent.getStringExtra("auth");
                final Long time=intent.getLongExtra("time",0);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        sensorValue[sensorId][0].setText(String.format("%.2f",data[0]));
//                        sensorValue[sensorId][1].setText(String.format("%.2f",data[1]));
//                        sensorName[sensorId].setText(name);
//                        sensorAuth[sensorId].setText(auth);
//                        sensorAuthTime[sensorId].setText(String.valueOf(time)+" ms");
                        SensorValue sv=sensorValueAdapter.getSensorValueObject(sensorId);
                        sv.setValue1(data[0]);
                        sv.setValue2(data[1]);
                        sv.setName(name);
                        sv.setAuthStatus(auth);
                        sv.setAuthTime(time);

                        if(sensorId==0) {
                            sv.setValueType1("Temperature");
                            sv.setValueType2("Humidity");
                        }
                        else
                        {
                            sv.setValueType1("Blood Oxygen Level");
                            sv.setValueType2("Heart Rate");
                        }
                        sensorValueAdapter.notifyDataSetChanged();
                    }
                });
            }


        }
    }
}
