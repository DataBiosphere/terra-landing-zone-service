package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.library.landingzones.definition.factories.CromwellStepsDefinitionProvider;
import bio.terra.landingzone.library.landingzones.definition.factories.ProtectedDataStepsDefinitionProvider;
import bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionProvider;

public class LandingZoneStepsDefinitionProviderFactory {
  private LandingZoneStepsDefinitionProviderFactory() {}

  public static StepsDefinitionProvider create(StepsDefinitionFactoryType type) {
    return switch (type) {
      case CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE -> new CromwellStepsDefinitionProvider();
      case PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME -> new ProtectedDataStepsDefinitionProvider();
    };
  }
}
