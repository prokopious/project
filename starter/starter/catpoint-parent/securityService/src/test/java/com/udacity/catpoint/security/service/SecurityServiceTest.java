package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.awt.image.BufferedImage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.Set;

import static com.udacity.catpoint.security.data.AlarmStatus.*;
import static com.udacity.catpoint.security.data.ArmingStatus.ARMED_HOME;
import static com.udacity.catpoint.security.data.ArmingStatus.DISARMED;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SecurityServiceTest {

    private SecurityService securityService;

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
    }

// 1 If alarm is armed and a sensor becomes activated, put the system into pending alarm status.

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void alarmAlreadyArmed_sensorGetsActivated_alarmChangesToPending(ArmingStatus status) {

        Set<Sensor> x = new HashSet<>(getDummySensors(true));
        Sensor sensor = new Sensor("z", SensorType.WINDOW);
        when(securityRepository.getArmingStatus()).thenReturn(status);
        when(securityRepository.getSensors()).thenReturn(x);
        when(securityRepository.getAlarmStatus()).thenReturn(NO_ALARM);
        securityService = new SecurityService(securityRepository, imageService);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(PENDING_ALARM);
    }

    //2  If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm on.

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void alarmAlreadyArmed_alreadyPending_sensorGetsActivated_alarmTurnsOn(ArmingStatus status) {

        Sensor sensor = new Sensor("z", SensorType.WINDOW);
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(status);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(ALARM);
    }

    //If pending alarm and all sensors are inactive, return to no alarm state
    //3 part 1: Sensors going from active to inactive while already pending
    @Test
    void alreadyPendingAlarm_sensorsAlreadyInactive_returnNoAlarm() {

        when(securityRepository.getSensors()).thenReturn(getDummySensors(true));
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        securityService.resetSensors();
        verify(securityRepository).setAlarmStatus(NO_ALARM);
    }

    //3 part 2: going from not pending to pending while sensors already inactive
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "ALARM"})
    void sensorsAlreadyInactive_setToPendingAlarm_returnNoAlarm(AlarmStatus status) {

        when(securityRepository.getSensors()).thenReturn(getDummySensors(false));
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        securityService.setAlarmStatus(PENDING_ALARM);
        verify(securityRepository).setAlarmStatus(NO_ALARM);
    }

    //   4. If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM"})
    void whenAlarmActive_sensorState_doesNotAffectAlarmState(AlarmStatus status) {

        when(securityRepository.getSensors()).thenReturn(getDummySensors(true));
        when(securityRepository.getAlarmStatus()).thenReturn(ALARM);
        securityService.resetSensors();
        verify(securityRepository, never()).setAlarmStatus(status);
    }

    //5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    void sensorActivatedWhileActive_systemAlreadyPending_changeToAlarm() {

        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        HashSet<Sensor> s = new HashSet(getDummySensors(true));
        when(securityRepository.getSensors()).thenReturn(s);
        for (Sensor sn : s) {
            securityService.changeSensorActivationStatus(sn, true);
        }
        verify(securityRepository, times(3)).setAlarmStatus(ALARM);
    }

    // 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    //part one: multiple sensors
    @ParameterizedTest
    @CsvSource({"ALARM, PENDING_ALARM", "PENDING_ALARM, PENDING_ALARM",
            "PENDING_ALARM, ALARM", "PENDING_ALARM, NO_ALARM", "PENDING_ALARM, ALARM", "ALARM, ALARM",
            "NO_ALARM, PENDING_ALARM", "NO_ALARM, ALARM", "NO_ALARM, NO_ALARM"})
    void sensorDeactivated_alreadyActive_noAlarmStateChange_multipleSensors(AlarmStatus statusOne, AlarmStatus statusTwo) {

        when(securityRepository.getSensors()).thenReturn(getDummySensors(false));
        when(securityRepository.getAlarmStatus()).thenReturn(statusOne);
        securityService.resetSensors();
        verify(securityRepository, never()).setAlarmStatus(statusTwo);
    }

    // 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    // part 2 (one sensor):
    @ParameterizedTest
    @CsvSource({"ALARM, PENDING_ALARM", "PENDING_ALARM, PENDING_ALARM",
            "PENDING_ALARM, ALARM", "PENDING_ALARM, NO_ALARM", "PENDING_ALARM, ALARM", "ALARM, ALARM",
            "NO_ALARM, PENDING_ALARM", "NO_ALARM, ALARM", "NO_ALARM, NO_ALARM"})
    void sensorDeactivated_alreadyActive_noAlarmStateChange_oneSensor(AlarmStatus statusOne, AlarmStatus statusTwo) {

        Sensor sensor = new Sensor("z", SensorType.WINDOW);
        sensor.setActive(false);
        when(securityRepository.getAlarmStatus()).thenReturn(statusOne);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(statusTwo);
    }

    //7 If the camera image contains a cat while the system is armed-home, put the system into alarm status.
    @Test
    void cameraImageProcessed_alreadyArmedHome_goToAlarmState() {
        //part one: System already PENDING_ALARM. Then a cat image is processed.
        BufferedImage bi = getImage();
        when(imageService.imageContainsCat(bi, 50.0f)).thenReturn(true);
        doReturn(ARMED_HOME).when(securityRepository).getArmingStatus();
        securityService.processImage(bi);
        verify(securityRepository).setAlarmStatus(ALARM);
    }

    //7 If the camera image contains a cat while the system is armed-home, put the system into alarm status.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cameraAlreadyShowsCat_systemdGetsArmedHome_activateAlarm(Boolean sensorsActive) {
        //part 2: A cat image is the current image stored in state. Then we change to PENDING_ALARM.
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        HashSet<Sensor> s = new HashSet(getDummySensors(sensorsActive));
        when(securityRepository.getSensors()).thenReturn(s);
        doReturn(true).when(securityRepository).getCatDisplayed();
        securityService.setAlarmStatus(PENDING_ALARM);
        verify(securityRepository).setAlarmStatus(ALARM);
    }

    //8 If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @ParameterizedTest  //alarm status
    @CsvSource({"FALSE, FALSE, PENDING_ALARM", "FALSE, FALSE, ALARM", "FALSE, FALSE, NO_ALARM",
    })
    void sensorsAlreadyInactive_catFreeImageGetsProcessed_changeToNoAlarm(Boolean sensorsActive, Boolean cat, AlarmStatus alarm) {
        BufferedImage bi = getImage();
        when(imageService.imageContainsCat(bi, 50.0f)).thenReturn(cat);
        Set<Sensor> s = new HashSet<>(getDummySensors(sensorsActive));
        when(securityRepository.getSensors()).thenReturn(s);
        when(securityRepository.getAlarmStatus()).thenReturn(alarm);
        securityService.processImage(bi);
        verify(securityRepository).setAlarmStatus(NO_ALARM);
    }

    // 9. If the system is disarmed, set the status to no alarm.
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"ALARM", "PENDING_ALARM", "NO_ALARM"})
    void ifSystemAlreadyDisarmed_changeToNoAlarm(AlarmStatus status) {

        doReturn(status).when(securityRepository).getAlarmStatus();
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(NO_ALARM);
    }

    // 10.If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void systemAlreadyArmed_resetSensors(ArmingStatus status) {

        Set<Sensor> s = new HashSet<>(getDummySensors(true));
        when(securityService.getArmingStatus()).thenReturn(ARMED_HOME);
        when(securityService.getSensors()).thenReturn(s);
        securityService.setArmingStatus(status);
        verify(securityRepository).changeSensorStatus(true);
    }

    // 11 If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    //part one: cat image most recent image stored in state. status changes to armed home
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"DISARMED", "ARMED_AWAY"})
    void catImageCurrentImage_systemArmedHome_setToAlarm(ArmingStatus status) {
        //part one (the most recent image already shows a cat)
        when(securityRepository.getCatDisplayed()).thenReturn(true);
        securityService.setArmingStatus(ARMED_HOME);
        verify(securityRepository).setAlarmStatus(ALARM);
    }

    // 11 If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
//part two (cat image processed while status already armed home)
    @Test
    void statusAlreadyArmedHome_catImageProcessed_setToAlarm() {

        BufferedImage bi = getImage();
        when(imageService.imageContainsCat(bi, 50.0f)).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ARMED_HOME);
        securityService.processImage(bi);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }


    //testing other methods
    @ParameterizedTest
    @CsvSource({"TRUE, TRUE", "FALSE, FALSE", "FALSE, TRUE", "TRUE, FALSE"})
    public void changeSensorActivationStatusBooleanCombinations(Boolean oldStatus, Boolean newStatus) {
        //changeSensorActivationStatus()
        when(securityRepository.getAlarmStatus()).thenReturn(NO_ALARM);
        Sensor s = getSensor(oldStatus);
        securityService.changeSensorActivationStatus(s, newStatus);
        verify(securityRepository).updateSensor(s);
    }

    @ParameterizedTest
    @CsvSource({"ARMED_HOME, PENDING_ALARM, ALARM", "ARMED_AWAY, PENDING_ALARM, ALARM",
            "ARMED_HOME, NO_ALARM, PENDING_ALARM", "ARMED_AWAY, NO_ALARM, PENDING_ALARM"})
    public void handleSensorActivated_differentArmingAndAlarmStatus(ArmingStatus arm, AlarmStatus alarm, AlarmStatus newAlarm) {
        //handleSensorActivated()
        if (arm != DISARMED) {
            when(securityRepository.getSensors()).thenReturn(getDummySensors(true));
            when(securityRepository.getArmingStatus()).thenReturn(arm);
            when(securityRepository.getAlarmStatus()).thenReturn(alarm);
            Sensor s = getSensor(true);
            securityService.changeSensorActivationStatus(s, true);
            verify(securityRepository).setAlarmStatus(newAlarm);
        }
    }

    @ParameterizedTest
    @CsvSource({"PENDING_ALARM, FALSE, NO_ALARM"})
    public void handleSensorDeactivated_differentArmingAndAlarmStatus(AlarmStatus alarm, Boolean activated, AlarmStatus newAlarm) {
        //handleSensorDeactivated()

        HashSet<Sensor> s = new HashSet<>(getDummySensors(activated));
        when(securityRepository.getSensors()).thenReturn(s);
        when(securityRepository.getAlarmStatus()).thenReturn(alarm);
        Sensor se = getSensor(true);
        securityService.changeSensorActivationStatus(se, false);
        verify(securityRepository).setAlarmStatus(newAlarm);
    }

    @ParameterizedTest
    @CsvSource({"TRUE, ARMED_HOME, TRUE", "TRUE, ARMED_AWAY, TRUE",
            "FALSE, ARMED_HOME, TRUE", "FALSE, ARMED_AWAY, TRUE",
            "FALSE, ARMED_HOME, FALSE", "FALSE, ARMED_AWAY, FALSE",
            "TRUE, ARMED_HOME, FALSE", "TRUE, ARMED_AWAY, FALSE"})
    //getCatDetected()
    public void getCatDetected_booleans_and_arming_status(Boolean cat, ArmingStatus arm, Boolean sensors) {

        BufferedImage bi = getImage();
        when(imageService.imageContainsCat(bi, 50.0f)).thenReturn(cat);
        HashSet<Sensor> s = new HashSet<>(getDummySensors(sensors));
        when(securityRepository.getSensors()).thenReturn(s);
        when(securityRepository.getArmingStatus()).thenReturn(arm);
        securityService.processImage(bi);
        verify(securityRepository).setCatDisplayed(cat);
    }

    @ParameterizedTest
    @CsvSource({"ARMED_HOME, TRUE", "ARMED_AWAY, TRUE",
            "ARMED_HOME, FALSE", "ARMED_AWAY, FALSE"})
        //setArmingStatus
    void testSetArmingStatus_differentSensorStatus(ArmingStatus status, Boolean sensors) {

        HashSet<Sensor> s = new HashSet<>(getDummySensors(sensors));
        when(securityRepository.getSensors()).thenReturn(s);
        securityService.setArmingStatus(status);
        verify(securityRepository).setArmingStatus(status);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void resetSensors_dummySensorParameters(Boolean sensors) {
        HashSet<Sensor> s = new HashSet<>(getDummySensors(sensors));
        when(securityRepository.getSensors()).thenReturn(s);
        securityService.resetSensors();
        for (Sensor sn : s) {
            verify(securityRepository).updateSensor(sn);
        }
    }



    @ParameterizedTest
    @CsvSource({"TRUE, ALARM, TRUE", "TRUE, PENDING_ALARM, TRUE", "TRUE, NO_ALARM, TRUE",
            "FALSE, ALARM, TRUE", "FALSE, PENDING_ALARM, TRUE", "FALSE, NO_ALARM, TRUE",
            "FALSE, ALARM, FALSE", "FALSE, PENDING_ALARM, FALSE", "FALSE, NO_ALARM, FALSE",
            "TRUE, ALARM, FALSE", "TRUE, PENDING_ALARM, FALSE", "FALSE, NO_ALARM, FALSE"})
    public void setAlarmStatus_testAgainstDifferentSensorStatusAndDifferentCatStatus(Boolean cat, AlarmStatus alarm, Boolean sensors) {
        //setAlarmStatus
        BufferedImage bi = getImage();
        when(imageService.imageContainsCat(bi, 50.0f)).thenReturn(cat);
        HashSet<Sensor> s = new HashSet<>(getDummySensors(sensors));
        when(securityRepository.getSensors()).thenReturn(s);
        when(securityRepository.getAlarmStatus()).thenReturn(alarm);
        securityService.setAlarmStatus(alarm);
        if (!cat && alarm != PENDING_ALARM) {
            verify(securityRepository).setAlarmStatus(alarm);
        }
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
        //verifySensorsInactive()
    void verifySensorInactive_differentStartingSensorStatus(Boolean active) {

        HashSet<Sensor> s = new HashSet<>(getDummySensors(active));
        when(securityRepository.getSensors()).thenReturn(s);
        securityService.verifySensorsInactive();
        verify(securityRepository).getSensors();
        assertTrue(securityService.verifySensorsInactive() == !active);
    }


    private Set<Sensor> sensorProvider() {
        return Set.of(
                // active status automatically set to false in constructor
                new Sensor("x", SensorType.WINDOW),
                new Sensor("y", SensorType.DOOR),
                new Sensor("z", SensorType.MOTION)
        );
    }

    public Set<Sensor> getDummySensors(Boolean active) {
        Set<Sensor> s = new HashSet<>(sensorProvider());
        for (Sensor sn : s) {
            sn.setActive(active);
        }
        return s;
    }

    public Sensor getSensor(Boolean active) {
        UUID u = UUID.randomUUID();
        Sensor s = new Sensor("d", SensorType.WINDOW);
        s.setActive(active);
        return s;
    }

    public BufferedImage getImage() {
        BufferedImage b = new BufferedImage(10, 10, BufferedImage.OPAQUE);
        return b;
    }
}