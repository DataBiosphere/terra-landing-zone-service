package bio.terra.landingzone.service.landingzone.azure.model;

import bio.terra.landingzone.common.exception.MissingRequiredFieldsException;
import java.util.Map;

import bio.terra.landingzone.model.AzureCloudContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public record AzureLandingZoneRequest(String definition, String version, Map<String, String> parameters,
                                      AzureCloudContext azureCloudContext) {



  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String definition;
    private String version;
    private Map<String, String> parameters;

    private AzureCloudContext azureCloudContext;

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

    public Builder azureCloudContext(AzureCloudContext azureCloudContext) {
      this.azureCloudContext = azureCloudContext;
      return this;
    }

    public AzureLandingZoneRequest build() {
      if (StringUtils.isBlank(definition)) {
        throw new MissingRequiredFieldsException(
                "Azure landing zone definition requires definition");
      }

      validateCloudContext();

      return new AzureLandingZoneRequest(definition, version, parameters, azureCloudContext);
    }

    private void validateCloudContext() {
      if (azureCloudContext == null) {
        throw new MissingRequiredFieldsException(
                "Azure cloud context can't be null or is missing");
      }

      if (StringUtils.isBlank(azureCloudContext.getAzureResourceGroupId())){
        throw new MissingRequiredFieldsException(
                "Resource Group ID is missing from the cloud context");
      }

      if (StringUtils.isBlank(azureCloudContext.getAzureSubscriptionId())){
        throw new MissingRequiredFieldsException(
                "Subscription ID is missing from the cloud context");
      }


      if (StringUtils.isBlank(azureCloudContext.getAzureTenantId())){
        throw new MissingRequiredFieldsException(
                "Tenant ID is missing from the cloud context");
      }
    }
  }
}
