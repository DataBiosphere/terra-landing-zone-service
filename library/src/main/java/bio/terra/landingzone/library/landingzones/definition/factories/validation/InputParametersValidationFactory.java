package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionFactoryType;
import java.util.List;

public class InputParametersValidationFactory {
  private InputParametersValidationFactory() {}

  public static List<InputParameterValidator> buildValidators(StepsDefinitionFactoryType type) {
    return switch (type) {
      case CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE -> buildCromwellLandingZoneValidators();
      case PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME ->
          buildProtectedDataLandingZoneValidators();
      case REFERENCED_DEFINITION_STEPS_PROVIDER_TYPE -> buildReferencedLandingZoneValidators();
    };
  }

  private static List<InputParameterValidator> buildReferencedLandingZoneValidators() {
    return List.of(new ReferencedVNetValidator());
  }

  private static List<InputParameterValidator> buildCromwellLandingZoneValidators() {
    return List.of(
        new AksParametersValidator(),
        new BlobCorsParametersValidator(),
        new StorageAccountSkuTypeValidator());
  }

  private static List<InputParameterValidator> buildProtectedDataLandingZoneValidators() {
    return List.of(
        new AksParametersValidator(),
        new BlobCorsParametersValidator(),
        new StorageAccountSkuTypeValidator());
  }
}
