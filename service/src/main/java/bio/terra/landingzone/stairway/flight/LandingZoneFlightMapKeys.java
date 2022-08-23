package bio.terra.landingzone.stairway.flight;

public class LandingZoneFlightMapKeys {
  public static final String OPERATION_TYPE = "operationType";
  public static final String DEPLOYED_AZURE_LANDING_ZONE_ID = "deployedAzureLandingZoneId";
  public static final String LANDING_ZONE_CREATE_PARAMS = "landingZoneCreateParams";
  public static final String LANDING_ZONE_AZURE_CONFIGURATION = "landingZoneAzureConfiguration";

  private LandingZoneFlightMapKeys() {}

  /** Common resource keys */
  public static class ResourceKeys {
    public static final String RESOURCE_ID = "resourceId";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String RESOURCE_NAME = "resourceName";
    // additional keys for job filtering
    public static final String STEWARDSHIP_TYPE = "stewardshipType";
    public static final String RESOURCE = "resource";

    private ResourceKeys() {}
  }
}
