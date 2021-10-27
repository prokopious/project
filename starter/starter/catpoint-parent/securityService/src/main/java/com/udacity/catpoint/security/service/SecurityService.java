package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static com.udacity.catpoint.security.data.AlarmStatus.*;

public class SecurityService {

    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();


    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    public void resetSensors() {

        ConcurrentSkipListSet<Sensor> sensors = new ConcurrentSkipListSet<>(getSensors());
        for (Sensor s : sensors) {
            changeSensorActivationStatus(s, false);
        }
    }

    public boolean verifySensorsInactive() {

        ConcurrentSkipListSet<Sensor> sensors = new ConcurrentSkipListSet<>(getSensors());
        ConcurrentSkipListSet<Sensor> active = new ConcurrentSkipListSet<>();
        for (Sensor s : sensors) {
            if (s.getActive()) {
                active.add(s);
            }
        }
        return active.isEmpty();
    }

    public void setArmingStatus(ArmingStatus armingStatus) {

        if (armingStatus == ArmingStatus.ARMED_AWAY) {
            resetSensors();
        } else {
            if (armingStatus == ArmingStatus.DISARMED) {
                setAlarmStatus(NO_ALARM);
            } else if (armingStatus == ArmingStatus.ARMED_HOME) {
                if (getCatDisplayed()) {
                    setAlarmStatus(ALARM);
                }
                resetSensors();
            }
        }
        securityRepository.setArmingStatus(armingStatus);
        System.out.println("Arming status: " + armingStatus);
    }

    private void catDetected(Boolean cat) {

        setCatDisplayed(cat);
        if (cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(ALARM);
        } else if (!cat && verifySensorsInactive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        for (StatusListener sl : statusListeners) {
            sl.catDetected(cat);
        }
        System.out.println("Cat detected: " + cat);
    }

    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    public void setAlarmStatus(AlarmStatus status) {
        if (verifySensorsInactive() && status == PENDING_ALARM && !getCatDisplayed()) {
            securityRepository.setAlarmStatus(NO_ALARM);
            System.out.println("alarm status: " + NO_ALARM);
        } else if (status == PENDING_ALARM && getCatDisplayed()) {
            securityRepository.setAlarmStatus(ALARM);
            System.out.println("alarm status: " + ALARM);
        } else {
            securityRepository.setAlarmStatus(status);
            System.out.println("alarm status: " + status);
            statusListeners.forEach(sl -> sl.notify(status));
        }
    }

    private void handleSensorActivated() {
        if (securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch (securityRepository.getAlarmStatus()) {
            case NO_ALARM -> {
                setAlarmStatus(AlarmStatus.PENDING_ALARM);
            }
            case PENDING_ALARM -> {
                setAlarmStatus(ALARM);
            }
        }
        if (verifySensorsInactive()) {
            securityRepository.changeSensorStatus(false);
        }
    }

    private void handleSensorDeactivated() {

        if (securityRepository.getAlarmStatus() == PENDING_ALARM) {
            if (verifySensorsInactive()) {
                setAlarmStatus(NO_ALARM);
            }
        }
        if (verifySensorsInactive()) {
            securityRepository.changeSensorStatus(true);
        }
    }

    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {

        if (!sensor.getActive() && active) {
            sensor.setActive(true);
            securityRepository.updateSensor(sensor);
            handleSensorActivated();
        } else if (sensor.getActive() && !active) {
            sensor.setActive(false);
            securityRepository.updateSensor(sensor);
            handleSensorDeactivated();
        } else if (sensor.getActive() && active) {
            sensor.setActive(true);
            securityRepository.updateSensor(sensor);
            handleSensorActivated();
        } else {
            sensor.setActive(active);
            securityRepository.updateSensor(sensor);
        }
    }

    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }


    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }


    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

    public Boolean getCatDisplayed() {
        return securityRepository.getCatDisplayed();
    }

    public void setCatDisplayed(Boolean cat) {
        securityRepository.setCatDisplayed(cat);
    }
}
