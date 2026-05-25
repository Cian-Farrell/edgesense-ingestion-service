package com.edgesense.ingestion;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

import java.nio.charset.StandardCharsets;

@Component
public class SensorDataListener {
    private static final String ENDPOINT = "a3ukfj6l4dra4j-ats.iot.eu-west-1.amazonaws.com";
    private static final String CLIENT_ID = "edge-sense-ingestion-service";
    private static final String TOPIC = "edgesense/sensor-data";
    private static final String CERT_PATH = "C:/Users/cianf/InternalPlacement/certs/86f52f8b5a613ca70021e08a299db7a2a110aeb70eef4239b4615a4ab88befd6-certificate.pem.crt";
    private static final String KEY_PATH = "C:/Users/cianf/InternalPlacement/certs/86f52f8b5a613ca70021e08a299db7a2a110aeb70eef4239b4615a4ab88befd6-private.pem.key";
    private static final String CA_PATH = "C:/Users/cianf/InternalPlacement/certs/AmazonRootCA1.pem";

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
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("Received sensor data: " + payload);
        }).get();

        System.out.println("Subscribed to topic: " + TOPIC);
    }
}
