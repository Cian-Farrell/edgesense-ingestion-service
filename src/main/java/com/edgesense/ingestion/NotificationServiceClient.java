package com.edgesense.ingestion;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationServiceClient {

    private static final String NOTIFICATION_SERVICE_URL = "http://localhost:8083/api/notifications/anomaly";
    private final RestTemplate restTemplate;

    public NotificationServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    public void sendAnomalyAlert(String payload){
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(NOTIFICATION_SERVICE_URL, request, String.class);
            System.out.println("Anomaly alert sent successfully");
        } catch (Exception e){
            System.err.println("Failed to send anomaly alert: " + e.getMessage());
        }
    }
}
