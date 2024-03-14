package bio.terra.lz.futureservice.common;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      // Disable instrumentation for spring-webmvc because pact still uses javax libs which causes
      // opentelemetry to try to load the same bean name twice, once for javax and once for jakarta
      "otel.instrumentation.spring-webmvc.enabled=false"
    })
public class BaseSpringUnitTest {}
