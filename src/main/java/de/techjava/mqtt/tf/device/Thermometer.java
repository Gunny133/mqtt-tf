package de.techjava.mqtt.tf.device;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tinkerforge.BrickletTemperature;
import com.tinkerforge.IPConnection;
import com.tinkerforge.NotConnectedException;
import com.tinkerforge.TimeoutException;

import de.techjava.mqtt.tf.comm.MqttSender;
import de.techjava.mqtt.tf.core.DeviceFactory;
import de.techjava.mqtt.tf.core.DeviceFactoryRegistry;
import de.techjava.mqtt.tf.core.EnvironmentHelper;

@Component
public class Thermometer implements DeviceFactory {

    private Logger logger = LoggerFactory.getLogger(Thermometer.class);
    @Value("${tinkerforge.thermometer.callbackperiod?: 10000}")
    private long callbackperiod;
    @Value("${tinkerforge.thermometer.topic?:temperature}")
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
        registry.registerDeviceFactory(BrickletTemperature.DEVICE_IDENTIFIER, this);
    }

    @Override
    public void createDevice(String uid) {
        final BrickletTemperature sensor = new BrickletTemperature(uid, ipcon);
        sensor.addTemperatureListener((temperature) -> {
            sender.sendMessage(envHelper.getTopic(uid) + topic, (((Short) temperature).doubleValue()) / 100.0);
        });
        try {
            sensor.setTemperatureCallbackPeriod(envHelper.getCallback(uid, callbackperiod));
        } catch (TimeoutException | NotConnectedException e) {
            logger.error("Error setting callback period", e);
        }
        logger.info("Thermometer uid {} initialized!", uid);
    }

}
