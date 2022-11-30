package bio.terra.landingzone.library.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "landingzone.sam")
public class LandingZoneSamConfiguration {
  /** URL of the SAM instance */
  private String basePath;

  /**
   * List of emails which should be granted 'user' role on newly created landing-zone resources in
   * Sam.
   */
  private List<String> landingZoneResourceUsers;

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public List<String> getLandingZoneResourceUsers() {
    return landingZoneResourceUsers;
  }

  public void setLandingZoneResourceUsers(List<String> landingZoneResourceUsers) {
    this.landingZoneResourceUsers = landingZoneResourceUsers;
  }
}
