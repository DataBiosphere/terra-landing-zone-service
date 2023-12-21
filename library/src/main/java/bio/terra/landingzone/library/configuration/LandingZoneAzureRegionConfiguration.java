package bio.terra.landingzone.library.configuration;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "landingzone.regions")
public class LandingZoneAzureRegionConfiguration {

  private Map<String, Map<String, String>> defaultParameters;

  public Map<String, Map<String, String>> getDefaultParameters() {
    return defaultParameters;
  }

  public void setDefaultParameters(Map<String, Map<String, String>> defaultParameters) {
    this.defaultParameters = defaultParameters;
  }
}
