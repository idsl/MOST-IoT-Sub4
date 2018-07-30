package li.power.idsl.most.app.model;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import li.power.idsl.most.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by PowerLi on 2017/7/14.
 */
public class SensorValueAdapter extends RecyclerView.Adapter{

    List<SensorValue> sensors=new ArrayList<>();

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_sensor, parent, false);
        SensorDataViewHolder vh = new SensorDataViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SensorDataViewHolder viewHolder=(SensorDataViewHolder)holder;
        SensorValue value=sensors.get(position);
        if(value.getName().equals("MOST-1"))
            viewHolder.nameText.setText("MOST-2");
        if(value.getName().equals("MOST-2"))
            viewHolder.nameText.setText("MOST-1");

        viewHolder.valueText1.setText(String.format("%.2f",value.getValue1()));
        viewHolder.valueText2.setText(String.format("%.2f",value.getValue2()));

        viewHolder.valueTypeText1.setText(value.getValueType1());
        viewHolder.valueTypeText2.setText(value.getValueType2());

        viewHolder.authTimeText.setText(String.valueOf(value.getAuthTime()));
        viewHolder.authStatusText.setText(value.getAuthStatus());

    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }

    class SensorDataViewHolder extends RecyclerView.ViewHolder
    {
        TextView nameText;
        TextView authTimeText;
        TextView valueText1;
        TextView valueText2;
        TextView valueTypeText1;
        TextView valueTypeText2;
        TextView authStatusText;

        public SensorDataViewHolder(View itemView) {
            super(itemView);
            nameText=(TextView) itemView.findViewById(R.id.sensor_name);
            authStatusText=(TextView) itemView.findViewById(R.id.sensor_auth);
            authTimeText=(TextView) itemView.findViewById(R.id.sensor_auth_time);
            valueText1=(TextView) itemView.findViewById(R.id.sensor_value1);
            valueText2=(TextView) itemView.findViewById(R.id.sensor_value2);
            valueTypeText1=(TextView) itemView.findViewById(R.id.label_sensor_value1);
            valueTypeText2=(TextView) itemView.findViewById(R.id.label_sensor_value2);
        }
    }

    public void addSensorValueObject(SensorValue value)
    {
        sensors.add(value);
    }

    public SensorValue getSensorValueObject(int position)
    {
        return sensors.get(position);
    }

}
