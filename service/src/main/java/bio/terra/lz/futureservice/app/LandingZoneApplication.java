package bio.terra.lz.futureservice.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// temporarily disable autoconfiguration since we don't have any related db yet.
// this prevents from running application
@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class},
    scanBasePackages = {
      // Dependencies for Stairway
      "bio.terra.common.kubernetes",
      // Scan for iam token handling
      "bio.terra.common.iam",
      // Stairway initialization and status
      "bio.terra.common.stairway",
      // Scan all landing zone service packages; from 'library' module
      "bio.terra.landingzone",
      // Scan for Liquibase migration components & configs
      "bio.terra.common.migrate",
      // future lz service
      "bio.terra.lz.futureservice"
    })
@EnableConfigurationProperties
@ConfigurationPropertiesScan("bio.terra.lz.futureservice")
// @ComponentScan(
//    basePackages = {
//      // Dependencies for Stairway
//      "bio.terra.common.kubernetes",
//      // Scan for iam token handling
//      "bio.terra.common.iam",
//      // Stairway initialization and status
//      "bio.terra.common.stairway",
//      // Scan all landing zone service packages; from 'library' module
//      "bio.terra.landingzone",
//      // Scan for Liquibase migration components & configs
//      "bio.terra.common.migrate",
//      // future lz service
//      "bio.terra.lz.futureservice"
//    })
public class LandingZoneApplication {

  public static void main(String[] args) {
    SpringApplication.run(LandingZoneApplication.class, args);
  }
}
