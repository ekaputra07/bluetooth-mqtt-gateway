package de.kai_morich.simple_bluetooth_terminal;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class BtMqttGateway {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final List<String> msgBuffer = new ArrayList<>();
    private Boolean startMarkerFound = false;
    private Mqtt5AsyncClient mqttClient;
    private BtMqttGatewayMessageListener listener;

    private final String mqttHost, mqttTopic, mqttIdentifier;

    public BtMqttGateway(String mqttHost, String mqttTopic, String mqttIdentifier) {
        this.mqttHost = mqttHost;
        this.mqttTopic = mqttTopic;
        this.mqttIdentifier = mqttIdentifier;
        initializeMqttClient();
    }

    public BtMqttGateway(String mqttHost, String mqttTopic, String mqttIdentifier, BtMqttGatewayMessageListener listener) {
        this.mqttHost = mqttHost;
        this.mqttTopic = mqttTopic;
        this.mqttIdentifier = mqttIdentifier;
        initializeMqttClient();
        this.listener = listener;
    }

    private void initializeMqttClient() {
        mqttClient = MqttClient.builder()
                .useMqttVersion5()
                .serverHost(this.mqttHost)
                .buildAsync();
    }

    public void mqttDisconnect() {
        if (mqttClient != null && isMqttStateConnected()) {
            mqttClient.disconnect();
        }
    }

    public boolean isMqttStateConnected() {
        return mqttClient.getState().isConnected();
    }

    public void handleRawMessage(ArrayDeque<byte[]> datas) {
        for (byte[] data : datas) {
            for (byte b : data) {
                String s = String.valueOf((char) b);
                String startMarker = "<";
                String endMarker = ">";
                if (startMarkerFound && s.equals(endMarker)) {
                    // startMarker found and endMarker found
                    String msg = String.join("", msgBuffer);
                    msgBuffer.clear();
                    startMarkerFound = false;
                    handleTextMessage(msg);
                } else if (startMarkerFound && s.equals(startMarker)) {
                    // startMarker found and startMarker found again, reset.
                    msgBuffer.clear();
                } else {
                    if (s.equals(startMarker)) {
                        startMarkerFound = true;
                    } else if (startMarkerFound) {
                        // append string to buffer.
                        msgBuffer.add(s);
                    }
                }
            }
        }
    }

    private void handleTextMessage(String message) {
        try {
            JSONObject jsonMessage = buildJsonMessage(message);
            sendMqttMessage(jsonMessage);

            if (listener != null) {
                listener.onTextMessage(message);
                listener.onJsonMessage(jsonMessage);
            }
            logger.info(jsonMessage.toString());
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }

    private JSONObject buildJsonMessage(String message) throws Exception {
        Instant instant = Instant.now();
        instant.getEpochSecond();

        JSONObject obj = new JSONObject();
        obj.put("msg_id", UUID.randomUUID().toString());
        obj.put("timestamp", instant.toEpochMilli());
        obj.put("device", this.mqttIdentifier);
        obj.put("message", message);
        return obj;
    }


    private void sendMqttMessage(JSONObject payload) {
        if (!isMqttStateConnected()) {
            mqttClient.toBlocking().connect();
        }
        mqttClient.publishWith()
                .topic(this.mqttTopic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(payload.toString().getBytes())
                .send();
    }

    public interface BtMqttGatewayMessageListener {
        public void onTextMessage(String message);

        public void onJsonMessage(JSONObject jsonObject);
    }
}
