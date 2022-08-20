package bio.terra.landingzone.resource;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.model.AzureCloudContext;
import bio.terra.landingzone.resource.flight.create.CreateExternalResourceFlight;
import bio.terra.landingzone.resource.model.StewardshipType;
import java.util.UUID;

public abstract class ExternalResource {
  private final UUID resourceId;
  private final String name;
  private final String description;

  private final AzureCloudContext azureCloudContext;

  public ExternalResource(
      UUID resourceId, String name, String description, AzureCloudContext azureCloudContext) {
    this.resourceId = resourceId;
    this.name = name;
    this.description = description;
    this.azureCloudContext = azureCloudContext;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public AzureCloudContext getAzureCloudContext() {
    return azureCloudContext;
  }

  public abstract ExternalResourceType getResourceType();

  public abstract StewardshipType getStewardshipType();

  /**
   * The CreateControlledResourceFlight calls this method to populate the resource-specific steps to
   * create the specific cloud resource.
   *
   * @param flight the create flight
   * @param flightBeanBag bean bag for finding Spring singletons
   */
  public abstract void addCreateSteps(
      CreateExternalResourceFlight flight, LandingZoneFlightBeanBag flightBeanBag);
}
