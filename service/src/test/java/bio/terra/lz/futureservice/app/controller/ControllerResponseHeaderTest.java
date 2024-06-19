package bio.terra.lz.futureservice.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import bio.terra.lz.futureservice.common.TestEndpoints;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = {
      // Disable instrumentation for spring-webmvc because pact still uses javax libs which causes
      // opentelemetry to try to load the same bean name twice, once for javax and once for jakarta
      "otel.instrumentation.spring-webmvc.enabled=false"
    })
public class ControllerResponseHeaderTest {

  @Autowired TestRestTemplate restTemplate;

  @Test
  void listAzureLandingZoneDefinitions_ResponseContainsCacheControlHeaders() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            TestEndpoints.LIST_AZURE_LANDING_ZONES_DEFINITIONS_PATH, String.class);

    assertEquals("no-store", response.getHeaders().getCacheControl());
    assertEquals("no-cache", response.getHeaders().getPragma());
  }
}
