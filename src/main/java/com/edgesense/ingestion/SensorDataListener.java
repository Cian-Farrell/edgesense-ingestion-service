package com.edgesense.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class SensorDataListener {
    private static final String ENDPOINT = "a3ukfj6l4dra4j-ats.iot.eu-west-1.amazonaws.com";
    private static final String CLIENT_ID = "edge-sense-ingestion-service";
    private static final String TOPIC = "edgesense/sensor-data";
    private static final String CERT_PATH = "C:/Users/cianf/InternalPlacement/certs/86f52f8b5a613ca70021e08a299db7a2a110aeb70eef4239b4615a4ab88befd6-certificate.pem.crt";
    private static final String KEY_PATH = "C:/Users/cianf/InternalPlacement/certs/86f52f8b5a613ca70021e08a299db7a2a110aeb70eef4239b4615a4ab88befd6-private.pem.key";
    private static final String CA_PATH = "C:/Users/cianf/InternalPlacement/certs/AmazonRootCA1.pem";

    private final StorageServiceClient storageServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final AnomalyDetector anomalyDetector;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SensorDataListener(StorageServiceClient storageServiceClient, NotificationServiceClient notificationServiceClient, AnomalyDetector anomalyDetector) {
        this.storageServiceClient = storageServiceClient;
        this.notificationServiceClient = notificationServiceClient;
        this.anomalyDetector = anomalyDetector;
    }

    @PostConstruct
    public void connect() throws Exception{
        MqttClientConnection connection = AwsIotMqttConnectionBuilder
                .newMtlsBuilderFromPath(CERT_PATH, KEY_PATH)
                .withCertificateAuthorityFromPath(null, CA_PATH)
                .withEndpoint(ENDPOINT)
                .withClientId(CLIENT_ID)
                .withCleanSession(true)
                .build();

        connection.connect().get();
        System.out.println("Connected to AWS IoT Core");

        connection.subscribe(TOPIC, QualityOfService.AT_LEAST_ONCE, (message) -> {
            try {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                System.out.println("Received sensor data: " + payload);

                Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                double temp = ((Number) data.get("temp")).doubleValue();
                double humidity = ((Number) data.get("humidity")).doubleValue();
                double timestamp = ((Number) data.get("timeStamp")).doubleValue();

                boolean anomaly = anomalyDetector.isAnomaly(temp, humidity);

                if (anomaly) {
                    System.out.println("Anomaly detected! Sending alert...");
                    notificationServiceClient.sendAnomalyAlert(payload);
                }

                storageServiceClient.saveSensorReading(payload);

            } catch (Exception e) {
                System.err.println("Error processing message: " + e.getMessage());
            }
        }).get();

        System.out.println("Subscribed to topic: " + TOPIC);
    }
}
