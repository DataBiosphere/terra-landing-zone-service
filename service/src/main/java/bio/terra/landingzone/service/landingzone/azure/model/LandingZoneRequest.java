package bio.terra.landingzone.service.landingzone.azure.model;

import bio.terra.landingzone.common.exception.MissingRequiredFieldsException;
import bio.terra.landingzone.model.LandingZoneTarget;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public record LandingZoneRequest(
    String definition,
    String version,
    Map<String, String> parameters,
    LandingZoneTarget landingZoneTarget) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String definition;
    private String version;
    private Map<String, String> parameters;

    private LandingZoneTarget landingZoneTarget;

    public Builder definition(String definition) {
      this.definition = definition;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder parameters(Map<String, String> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder azureCloudContext(LandingZoneTarget landingZoneTarget) {
      this.landingZoneTarget = landingZoneTarget;
      return this;
    }

    public LandingZoneRequest build() {
      if (StringUtils.isBlank(definition)) {
        throw new MissingRequiredFieldsException(
            "Azure landing zone definition requires definition");
      }

      validateCloudContext();

      return new LandingZoneRequest(definition, version, parameters, landingZoneTarget);
    }

    private void validateCloudContext() {
      if (landingZoneTarget == null) {
        throw new MissingRequiredFieldsException("Azure cloud context can't be null or is missing");
      }

      if (StringUtils.isBlank(landingZoneTarget.getAzureResourceGroupId())) {
        throw new MissingRequiredFieldsException(
            "Resource Group ID is missing from the cloud context");
      }

      if (StringUtils.isBlank(landingZoneTarget.getAzureSubscriptionId())) {
        throw new MissingRequiredFieldsException(
            "Subscription ID is missing from the cloud context");
      }

      if (StringUtils.isBlank(landingZoneTarget.getAzureTenantId())) {
        throw new MissingRequiredFieldsException("Tenant ID is missing from the cloud context");
      }
    }
  }
}
