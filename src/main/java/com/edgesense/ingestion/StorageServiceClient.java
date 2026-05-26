package com.edgesense.ingestion;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class StorageServiceClient {

    private static final String STORAGE_SERVICE_URL = "http://localhost:8082/api/readings";
    private final RestTemplate restTemplate;

    public StorageServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    public void saveSensorReading(String payload){
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(STORAGE_SERVICE_URL, request, String.class);
            System.out.println("Forwarded to storage service: " + payload);
        } catch (Exception e) {
            System.out.println("Failed to forward to storage service: " + e.getMessage());
        }
    }

}
