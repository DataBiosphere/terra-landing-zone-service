package bio.terra.landingzone.library.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "landingzone.protected-data")
/**
 * This represents configuration for "ProtectedData" Landing Zone. We are creating Automation rule
 * in Sentinel. This automation rule should trigger a certain "playbook".The "playbook" is
 * represented by an Azure LogicApp service. To refer to a certain LogicApp it is required to pass
 * LogicApp identifier and tenant where it resides.
 */
public class LandingZoneProtectedDataConfiguration {
  private String logicAppResourceId;
  private String tenantId;

  /**
   * Returns resource identifier of an Azure LogicApp.
   *
   * @return Azure resource identifier.
   */
  public String getLogicAppResourceId() {
    return logicAppResourceId;
  }

  public void setLogicAppResourceId(String logicAppResourceId) {
    this.logicAppResourceId = logicAppResourceId;
  }

  /**
   * Returns tenant identifier where LogicApp resides.
   *
   * @return Azure tenant identifier.
   */
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }
}
