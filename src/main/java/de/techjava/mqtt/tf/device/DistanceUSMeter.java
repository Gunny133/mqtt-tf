package de.techjava.mqtt.tf.device;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tinkerforge.BrickletDistanceUS;
import com.tinkerforge.IPConnection;
import com.tinkerforge.NotConnectedException;
import com.tinkerforge.TimeoutException;

import de.techjava.mqtt.tf.comm.MqttSender;
import de.techjava.mqtt.tf.core.DeviceController;
import de.techjava.mqtt.tf.core.DeviceFactory;
import de.techjava.mqtt.tf.core.DeviceFactoryRegistry;
import de.techjava.mqtt.tf.core.EnvironmentHelper;

@Component
public class DistanceUSMeter implements DeviceFactory<BrickletDistanceUS>, DeviceController<BrickletDistanceUS> {

    private final static Logger LOGGEr = LoggerFactory.getLogger(DistanceIRMeter.class);
    
    @Value("${tinkerforge.distance.us.callbackperiod?:500}")
    private long callbackperiod;
    @Value("${tinkerforge.distance.us.topic?:distance}")
    private String topic;

    @Autowired
    private IPConnection ipcon;
    @Autowired
    private MqttSender sender;
    @Autowired
    private DeviceFactoryRegistry registry;
    @Autowired
    private EnvironmentHelper envHelper;

    @PostConstruct
    public void init() {
        registry.registerDeviceFactory(BrickletDistanceUS.DEVICE_IDENTIFIER, this);
        registry.registerDeviceController(BrickletDistanceUS.DEVICE_IDENTIFIER, this);
    }

    @Override
    public BrickletDistanceUS createDevice(final String uid) {
        return new BrickletDistanceUS(uid, ipcon);
    }

    @Override
    public void setupDevice(final String uid, final BrickletDistanceUS sensor) {
        boolean enable = !envHelper.isDisabled(uid, DistanceUSMeter.class);
        if (enable) {
            sensor.addDistanceListener((distance) -> {
                sender.sendMessage(envHelper.getTopic(uid) + topic, distance);
            });
            try {
                sensor.setDistanceCallbackPeriod(envHelper.getCallback(uid, callbackperiod));
            } catch (TimeoutException | NotConnectedException e) {
                LOGGEr.error("Error setting callback period", e);
            }
        } else {
            LOGGEr.info("{} listener disabled.", getClass().getSimpleName());
        }
        LOGGEr.info("{} with uid {} initilized.", getClass().getSimpleName(), uid);
    }
}
