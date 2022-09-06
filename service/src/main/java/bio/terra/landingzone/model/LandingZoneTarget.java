package bio.terra.landingzone.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class LandingZoneTarget {
  private String azureTenantId;
  private String azureSubscriptionId;
  private String azureResourceGroupId;

  // Constructor for Jackson
  public LandingZoneTarget() {}

  // Constructor for deserializer
  public LandingZoneTarget(
      String azureTenantId, String azureSubscriptionId, String azureResourceGroupId) {
    this.azureTenantId = azureTenantId;
    this.azureSubscriptionId = azureSubscriptionId;
    this.azureResourceGroupId = azureResourceGroupId;
  }

  public String getAzureTenantId() {
    return azureTenantId;
  }

  public String getAzureSubscriptionId() {
    return azureSubscriptionId;
  }

  public String getAzureResourceGroupId() {
    return azureResourceGroupId;
  }

  public void setAzureTenantId(String azureTenantId) {
    this.azureTenantId = azureTenantId;
  }

  public void setAzureSubscriptionId(String azureSubscriptionId) {
    this.azureSubscriptionId = azureSubscriptionId;
  }

  public void setAzureResourceGroupId(String azureResourceGroupId) {
    this.azureResourceGroupId = azureResourceGroupId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LandingZoneTarget that = (LandingZoneTarget) o;

    return new EqualsBuilder()
        .append(azureTenantId, that.azureTenantId)
        .append(azureSubscriptionId, that.azureSubscriptionId)
        .append(azureResourceGroupId, that.azureResourceGroupId)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(azureTenantId)
        .append(azureSubscriptionId)
        .append(azureResourceGroupId)
        .toHashCode();
  }
}
