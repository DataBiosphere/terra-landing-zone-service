package bio.terra.landingzone.stairway.flight;

public class LandingZoneStepsDefinitionProviderFactory {
  // TODO: make enum
  public static final String CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE =
      "CromwellBaseResourcesFactory";
  public static final String PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME =
      "ProtectedDataResourcesFactory";

  public static StepsDefinitionProvider create(String type) {
    return switch (type) {
      case CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE -> new CromwellStepsDefinitionProvider();
      case PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME -> new ProtectedDataStepsDefinitionProvider();
      default -> throw new RuntimeException("Unknown step definition provider type");
    };
  }
}
