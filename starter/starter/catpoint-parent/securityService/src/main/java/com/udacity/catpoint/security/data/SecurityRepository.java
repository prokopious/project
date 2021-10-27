package com.udacity.catpoint.security.data;

import java.util.Set;


public interface SecurityRepository {
    String add(String input1, String input2);
    void addSensor(Sensor sensor);
    void removeSensor(Sensor sensor);
    void updateSensor(Sensor sensor);
    void setAlarmStatus(AlarmStatus alarmStatus);
    void setArmingStatus(ArmingStatus armingStatus);
    void setCatDisplayed(Boolean cat);
    void changeSensorStatus(Boolean status);
    Boolean getCatDisplayed();
    Set<Sensor> getSensors();
    AlarmStatus getAlarmStatus();
    ArmingStatus getArmingStatus();


}
