package bio.terra.lz.futureservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// temporarily disable autoconfiguration since we don't have it yet.
// this prevents from running application
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class LandingZoneApplication {

  public static void main(String[] args) {
    SpringApplication.run(LandingZoneApplication.class, args);
  }
}
