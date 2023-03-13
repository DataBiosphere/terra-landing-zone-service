package bio.terra.landingzone.library.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "landingzone.testing")
public class LandingZoneTestingConfiguration {

  private boolean allowAttach;

  /**
   * Determines whether we allow the "attachment" of a Terra landing zone to pre-deployed Azure
   * resources.
   */
  public boolean isAllowAttach() {
    return allowAttach;
  }

  public void setAllowAttach(boolean allowAttach) {
    this.allowAttach = allowAttach;
  }
}
