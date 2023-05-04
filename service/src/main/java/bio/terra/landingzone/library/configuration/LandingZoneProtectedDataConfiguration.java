package bio.terra.landingzone.library.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "landingzone.protected-data")
public class LandingZoneProtectedDataConfiguration {
  private String logicAppResourceId;
  private String tenantId;

  public String getLogicAppResourceId() {
    return logicAppResourceId;
  }

  public void setLogicAppResourceId(String logicAppResourceId) {
    this.logicAppResourceId = logicAppResourceId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }
}
