package bio.terra.landingzone.library.configuration;

import java.util.List;
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
  private List<String> longTermStorageTableNames;
  private String longTermStorageResourceGroupName;
  private String adminSubscriptionId;

  /**
   * List of tables that will be exported from a protected data Landing Zone's log analytics
   * workspace to a storage account in an administrative subscription
   *
   * @return List of log analytics workspace table names
   */
  public List<String> getLongTermStorageTableNames() {
    return longTermStorageTableNames;
  }

  public void setLongTermStorageTableNames(List<String> longTermStorageTableNames) {
    this.longTermStorageTableNames = longTermStorageTableNames;
  }

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

  /**
   * Resource group name that provides storage accounts for long term storage of protected data
   * logs.
   *
   * @return Resource group name
   */
  public String getLongTermStorageResourceGroupName() {
    return longTermStorageResourceGroupName;
  }

  public void setLongTermStorageResourceGroupName(String longTermStorageResourceGroupName) {
    this.longTermStorageResourceGroupName = longTermStorageResourceGroupName;
  }

  /**
   * ID of the subscription where the long term storage account resource group resides
   *
   * @return Subscription ID
   */
  public String getAdminSubscriptionId() {
    return adminSubscriptionId;
  }

  public void setAdminSubscriptionId(String adminSubscriptionId) {
    this.adminSubscriptionId = adminSubscriptionId;
  }
}
