package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import org.apache.logging.log4j.util.Strings;

public class ReferencedVNetValidator implements InputParameterValidator {

  @Override
  public void validate(ParametersResolver parametersResolver)
      throws InvalidInputParameterException {
    StringBuilder sbErrors = new StringBuilder();

    validateRequiredParameter(
        parametersResolver, LandingZoneDefaultParameters.ParametersNames.BATCH_SUBNET, sbErrors);
    validateRequiredParameter(
        parametersResolver, LandingZoneDefaultParameters.ParametersNames.COMPUTE_SUBNET, sbErrors);

    var errorMessage = sbErrors.toString();
    if (!Strings.isBlank(errorMessage)) {
      throw new InvalidInputParameterException(errorMessage);
    }
  }

  private void validateRequiredParameter(
      ParametersResolver parametersResolver,
      LandingZoneDefaultParameters.ParametersNames parameterName,
      StringBuilder sbErrorMessage) {
    String value = parametersResolver.getValue(parameterName.name());
    if (Strings.isBlank(value)) {
      sbErrorMessage.append(" ");
      sbErrorMessage.append(buildMissingParameterErrorMessage(parameterName));
    }
  }

  private String buildMissingParameterErrorMessage(
      LandingZoneDefaultParameters.ParametersNames parameterName) {
    return String.format("%s is required.", parameterName.name());
  }
}
