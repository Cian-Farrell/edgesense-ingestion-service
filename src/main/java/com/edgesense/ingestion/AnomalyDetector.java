package com.edgesense.ingestion;

import org.springframework.stereotype.Component;

@Component
public class AnomalyDetector {

    private static final double MAX_TEMP = 30.0;
    private static final double MIN_TEMP = 10.0;
    private static final double MAX_HUMIDITY = 80.0;
    private static final double MIN_HUMIDITY = 20.0;

    public boolean isAnomaly(double temp, double humidity) {
        return temp > MAX_TEMP || temp < MIN_TEMP || humidity > MAX_HUMIDITY || humidity < MIN_HUMIDITY;
    }
}
