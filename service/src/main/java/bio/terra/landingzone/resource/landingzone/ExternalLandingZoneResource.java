package bio.terra.landingzone.resource.landingzone;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.model.AzureCloudContext;
import bio.terra.landingzone.resource.ExternalResource;
import bio.terra.landingzone.resource.ExternalResourceType;
import bio.terra.landingzone.resource.flight.create.CreateExternalResourceFlight;
import bio.terra.landingzone.resource.model.StewardshipType;
import java.util.Map;
import java.util.UUID;

public class ExternalLandingZoneResource extends ExternalResource {
  private final String definition;
  private final String version;
  private final Map<String, String> properties;

  public ExternalLandingZoneResource(
      UUID resourceId,
      String definition,
      String version,
      Map<String, String> properties,
      String name,
      String description,
      AzureCloudContext azureCloudContext) {
    super(resourceId, name, description, azureCloudContext);
    this.definition = definition;
    this.version = version;
    this.properties = properties;
  }

  public String getDefinition() {
    return definition;
  }

  public String getVersion() {
    return version;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public ExternalResourceType getResourceType() {
    return ExternalResourceType.AZURE_LANDING_ZONE;
  }

  @Override
  public StewardshipType getStewardshipType() {
    return StewardshipType.EXTERNAL;
  }

  @Override
  public void addCreateSteps(
      CreateExternalResourceFlight flight, LandingZoneFlightBeanBag flightBeanBag) {

    flight.addStep(
        new CreateAzureExternalLandingZoneStep(
            flightBeanBag.getAzureLandingZoneService(),
            flightBeanBag.getAzureLandingZoneManagerProvider()),
        RetryRules.cloud());

    flight.addStep(
        new CreateAzureLandingZoneDbRecordStep(flightBeanBag.getLandingZoneDao()),
        RetryRules.shortDatabase());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    ExternalLandingZoneResource that = (ExternalLandingZoneResource) o;

    return definition.equals(that.getDefinition()) && version.equals(that.getVersion());
  }

  // TODO SG: review it later
  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + definition.hashCode() + version.hashCode();
    return result;
  }

  public static ExternalLandingZoneResource.Builder builder() {
    return new ExternalLandingZoneResource.Builder();
  }

  public static class Builder {
    private UUID resourceId;
    private String definition;
    private String version;
    private String name;
    private String description;
    private Map<String, String> properties;
    private AzureCloudContext azureCloudContext;

    public Builder resourceId(UUID resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    public Builder definition(String definition) {
      this.definition = definition;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder parameters(Map<String, String> properties) {
      this.properties = properties;
      return this;
    }

    public Builder azureCloudContext(AzureCloudContext azureCloudContext) {
      this.azureCloudContext = azureCloudContext;
      return this;
    }

    public ExternalLandingZoneResource build() {
      return new ExternalLandingZoneResource(
          resourceId, definition, version, properties, name, description, azureCloudContext);
    }
  }
}
