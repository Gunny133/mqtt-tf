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
import de.techjava.mqtt.tf.comm.naming.MqttTinkerForgeRealm;
import de.techjava.mqtt.tf.core.DeviceFactory;
import de.techjava.mqtt.tf.core.DeviceFactoryRegistry;

@Component
public class DistanceUSMeter implements DeviceFactory {

	private Logger logger = LoggerFactory.getLogger(DistanceIRMeter.class);
	@Value("${tinkerforge.distance.us.callbackperiod?: 500}")
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
	private MqttTinkerForgeRealm realm;

	@PostConstruct
	public void init() {
		registry.registerDeviceFactory(BrickletDistanceUS.DEVICE_IDENTIFIER, this);
	}

	@Override
	public void createDevice(String uid) {
		BrickletDistanceUS sensor = new BrickletDistanceUS(uid, ipcon);
		sensor.addDistanceListener((distance) -> {
			sender.sendMessage(realm.getTopic(uid) + topic, String.valueOf(distance));
		});
		try {
			sensor.setDistanceCallbackPeriod(realm.getCallback(uid, callbackperiod));
		} catch (TimeoutException | NotConnectedException e) {
			logger.error("Error setting callback period", e);
		}
		logger.info("Ultra-sound distance with uid {} initialized", uid);
	}
}