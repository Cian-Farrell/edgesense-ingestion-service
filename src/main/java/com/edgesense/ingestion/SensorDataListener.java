package com.edgesense.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class SensorDataListener {
    private static final String ENDPOINT = "a3ukfj6l4dra4j-ats.iot.eu-west-1.amazonaws.com";
    private static final String CLIENT_ID = "edge-sense-ingestion-service";
    private static final String TOPIC = "edgesense/sensor-data";

    private final StorageServiceClient storageServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MqttClientConnection connection;

    public SensorDataListener(StorageServiceClient storageServiceClient,
                              NotificationServiceClient notificationServiceClient) {
        this.storageServiceClient = storageServiceClient;
        this.notificationServiceClient = notificationServiceClient;
    }

    @PostConstruct
    public void connect() throws Exception {
        Path certPath = writeTempFile("cert", System.getenv("CERT_CONTENT"));
        Path keyPath  = writeTempFile("key",  System.getenv("KEY_CONTENT"));
        Path caPath   = writeTempFile("ca",   System.getenv("CA_CONTENT"));

        connection = AwsIotMqttConnectionBuilder
                .newMtlsBuilderFromPath(certPath.toString(), keyPath.toString())
                .withCertificateAuthorityFromPath(null, caPath.toString())
                .withEndpoint(ENDPOINT)
                .withClientId(CLIENT_ID)
                .withCleanSession(true)
                .withConnectionEventCallbacks(new MqttClientConnectionEvents() {
                    @Override
                    public void onConnectionInterrupted(int errorCode) {
                        System.err.println("MQTT connection interrupted, error code: " + errorCode
                                + " — SDK will attempt to reconnect automatically");
                    }

                    @Override
                    public void onConnectionResumed(boolean sessionPresent) {
                        System.out.println("MQTT connection resumed. Session present: " + sessionPresent);
                        if (!sessionPresent) {
                            // Clean session means the broker forgot our subscription — re-subscribe now
                            System.out.println("No session present after reconnect — re-subscribing to " + TOPIC);
                            resubscribe();
                        }
                    }
                })
                .build();

        connection.connect().get();
        System.out.println("Connected to AWS IoT Core");

        subscribe();
    }

    private void subscribe() throws Exception {
        connection.subscribe(TOPIC, QualityOfService.AT_LEAST_ONCE, (message) -> {
            try {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                System.out.println("Received sensor data: " + payload);

                Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                boolean anomaly = (Boolean) data.get("anomaly");

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

    private void resubscribe() {
        try {
            subscribe();
        } catch (Exception e) {
            System.err.println("Failed to re-subscribe after reconnect: " + e.getMessage());
        }
    }

    private Path writeTempFile(String prefix, String content) throws IOException {
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Environment variable for cert '" + prefix + "' is missing or empty");
        }
        String normalised = content.replace("\r\n", "\n").replace("\r", "\n");
        Path tempFile = Files.createTempFile(prefix, ".pem");
        Files.writeString(tempFile, normalised, StandardCharsets.UTF_8);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }
}