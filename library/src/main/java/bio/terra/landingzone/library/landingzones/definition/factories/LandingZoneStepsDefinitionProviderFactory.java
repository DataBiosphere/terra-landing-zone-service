package bio.terra.landingzone.library.landingzones.definition.factories;

public class LandingZoneStepsDefinitionProviderFactory {
  private LandingZoneStepsDefinitionProviderFactory() {}

  public static StepsDefinitionProvider create(StepsDefinitionFactoryType type) {
    return switch (type) {
      case CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE -> new CromwellStepsDefinitionProvider();
      case PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME ->
          new ProtectedDataStepsDefinitionProvider();
      case REFERENCED_DEFINITION_STEPS_PROVIDER_TYPE ->
          new ReferencedLandingZoneStepsDefinitionProvider();
    };
  }
}
