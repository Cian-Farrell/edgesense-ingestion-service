package com.edgesense.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class IngestionServiceApplicationTests {

	// Mock out SensorDataListener so it doesn't try to connect
	// to AWS IoT Core during testing (certs not available in CI)
	@MockitoBean
	private SensorDataListener sensorDataListener;

	@Test
	void contextLoads() {
	}

}
