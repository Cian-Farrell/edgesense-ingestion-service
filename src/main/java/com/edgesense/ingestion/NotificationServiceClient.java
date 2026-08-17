package com.edgesense.ingestion;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class NotificationServiceClient {

    private static final String NOTIFICATION_SERVICE_URL = "http://notification.edgesense.local:8083/api/notifications/anomaly";
    private final RestTemplate restTemplate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "notification-sender");
        t.setDaemon(true);
        return t;
    });

    public NotificationServiceClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); // 2s to establish connection
        factory.setReadTimeout(3000);    // 3s to wait for a response
        this.restTemplate = new RestTemplate(factory);
    }

    public void sendAnomalyAlert(String payload) {
        executor.submit(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> request = new HttpEntity<>(payload, headers);
                restTemplate.postForEntity(NOTIFICATION_SERVICE_URL, request, String.class);
                System.out.println("Anomaly alert sent successfully");
            } catch (Exception e) {
                System.err.println("Failed to send anomaly alert: " + e.getMessage());
            }
        });
    }
}