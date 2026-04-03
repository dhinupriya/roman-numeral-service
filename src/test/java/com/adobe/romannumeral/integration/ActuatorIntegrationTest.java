package com.adobe.romannumeral.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for actuator endpoints — health, prometheus, metrics.
 *
 * <p>Uses {@code RANDOM_PORT} with {@code TestRestTemplate} (no MockMvc) to ensure
 * actuator endpoints are fully initialized including the Prometheus registry.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Actuator Integration Tests")
class ActuatorIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Nested
    @DisplayName("Health endpoint")
    class Health {

        @Test
        @DisplayName("/actuator/health should return UP with conversion indicator")
        void shouldReturnHealthUp() {
            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody())
                    .contains("\"status\":\"UP\"")
                    .contains("\"conversion\"")
                    .contains("convert(1) = I");
        }
    }

    @Nested
    @DisplayName("Prometheus endpoint")
    class Prometheus {

        @Test
        @DisplayName("/actuator/prometheus should return 200 with Prometheus format metrics")
        void shouldReturnPrometheusEndpoint() {
            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("jvm_memory");
        }

        @Test
        @DisplayName("Should contain single conversion metrics after a request")
        void shouldContainSingleMetrics() {
            restTemplate.getForEntity("/romannumeral?query=1", String.class);

            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody())
                    .contains("roman_conversion_single_total")
                    .contains("roman_conversion_single_duration_seconds");
        }

        @Test
        @DisplayName("Should contain range metrics after a range request")
        void shouldContainRangeMetrics() {
            restTemplate.getForEntity("/romannumeral?min=1&max=10", String.class);

            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody())
                    .contains("roman_conversion_range_total")
                    .contains("roman_conversion_range_size");
        }

        @Test
        @DisplayName("Should contain error metrics after an error request")
        void shouldContainErrorMetrics() {
            restTemplate.getForEntity("/romannumeral?query=0", String.class);

            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody())
                    .contains("roman_conversion_error_total");
        }
    }

    @Nested
    @DisplayName("Metrics endpoint")
    class Metrics {

        @Test
        @DisplayName("/actuator/metrics should list available metrics")
        void shouldReturnMetricsList() {
            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("names");
        }
    }
}
