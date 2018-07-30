package li.power.idsl.most.app.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
/**
 * Created by PowerLi on 2017/6/14.
 */
@DatabaseTable(tableName = "device_value")
public class SensorData {

    @DatabaseField(id = true,columnName = "ID")
    int id;

    @DatabaseField(columnName = "DeviceID")
    String deviceId;

    @DatabaseField(columnName = "DataType")
    int dataType;

    @DatabaseField(columnName = "Value")
    float value;

    @DatabaseField(columnName = "name" )
    String name;

    @DatabaseField(columnName = "reportid")
    String reportId="01122333-4556-6778-899a-abbccddeeff0";

    public SensorData(String deviceId, int dataType, float value, String name) {
        this.deviceId = deviceId;
        this.dataType = dataType;
        this.value = value;
        this.name=name;
    }

    public SensorData() {
    }
}