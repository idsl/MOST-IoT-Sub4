package li.power.idsl.most.app.model;

import li.power.idsl.most.app.sensor.Sensor;

/**
 * Created by PowerLi on 2017/7/14.
 */
public class SensorValue {
    String name="null";
    long authTime=0;
    float value1=0;
    float value2=0;
    String valueType1="Sensor Data: ";
    String valueType2="Sensor Data: ";
    String authStatus="no connect";

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthTime(long authTime) {
        this.authTime = authTime;
    }

    public void setValue1(float value1) {
        this.value1 = value1;
    }

    public void setValue2(float value2) {
        this.value2 = value2;
    }

    public void setValueType1(String valueType1) {
        this.valueType1 = "Sensor Data: "+valueType1;
    }

    public void setValueType2(String valueType2) {
        this.valueType2 = "Sensor Data: "+valueType2;
    }

    public void setAuthStatus(String authStatus) {
        this.authStatus = authStatus;
    }

    public String getName() {
        return name;
    }

    public long getAuthTime() {
        return authTime;
    }

    public float getValue1() {
        return value1;
    }

    public float getValue2() {
        return value2;
    }

    public String getValueType1() {
        return valueType1;
    }

    public String getValueType2() {
        return valueType2;
    }

    public String getAuthStatus() {
        return authStatus;
    }

}
