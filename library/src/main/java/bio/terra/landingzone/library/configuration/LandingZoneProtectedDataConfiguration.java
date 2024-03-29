package bio.terra.landingzone.library.configuration;

import java.util.List;
import java.util.Map;
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
  private Map<String, String> longTermStorageAccountIds;
  private List<String> sentinelScheduledAlertRuleTemplateIds;
  private List<String> sentinelMlRuleTemplateIds;
  private List<String> sentinelNrtRuleTemplateIds;

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
   * Map of region names to storage account IDs that may be used for long term storage of logs
   *
   * @return Map of regions to storage account IDs
   */
  public Map<String, String> getLongTermStorageAccountIds() {
    return longTermStorageAccountIds;
  }

  public void setLongTermStorageAccountIds(Map<String, String> longTermStorageAccountIds) {
    this.longTermStorageAccountIds = longTermStorageAccountIds;
  }

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

  public List<String> getSentinelScheduledAlertRuleTemplateIds() {
    return sentinelScheduledAlertRuleTemplateIds;
  }

  public void setSentinelScheduledAlertRuleTemplateIds(
      List<String> sentinelScheduledAlertRuleTemplateIds) {
    this.sentinelScheduledAlertRuleTemplateIds = sentinelScheduledAlertRuleTemplateIds;
  }

  public List<String> getSentinelMlRuleTemplateIds() {
    return sentinelMlRuleTemplateIds;
  }

  public void setSentinelMlRuleTemplateIds(List<String> sentinelMlRuleTemplateIds) {
    this.sentinelMlRuleTemplateIds = sentinelMlRuleTemplateIds;
  }

  public List<String> getSentinelNrtRuleTemplateIds() {
    return sentinelNrtRuleTemplateIds;
  }

  public void setSentinelNrtRuleTemplateIds(List<String> sentinelNrtRuleTemplateIds) {
    this.sentinelNrtRuleTemplateIds = sentinelNrtRuleTemplateIds;
  }
}
