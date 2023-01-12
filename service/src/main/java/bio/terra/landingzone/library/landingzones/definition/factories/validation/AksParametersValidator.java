package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;

import java.util.Optional;

public class AksParametersValidator implements InputParameterValidator {
  static final String WRONG_PARAMETER_VALUE_MESSAGE = "Value of the '%s' parameter is not valid.";

  private final StringBuilder sbErrors = new StringBuilder();

  @Override
  public void validate(ParametersResolver parametersResolver)
      throws InvalidInputParameterException {
    Optional<String> aksAutoscalingRangeValidationMessage = validateAksAutoscalingRange(parametersResolver);
    aksAutoscalingRangeValidationMessage.ifPresent(s -> sbErrors.append(s).append(" "));

    if (!sbErrors.isEmpty()) {
      throw new InvalidInputParameterException(sbErrors.toString());
    }
  }

  private Optional<String> validateAksAutoscalingRange(ParametersResolver parametersResolver) {
    String min = parametersResolver.getValue(CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_MIN.name());
    String max = parametersResolver.getValue(CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_MAX.name());

    String minErrorMessage =
        buildErrorMessage(
            CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_MIN,
            " The value must be a number between 0 and the autoscaling max.");
    String maxErrorMessage =
        buildErrorMessage(
            CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_MAX,
            " The value must be a number between the autoscaling min and 1000.");

    int minValue;
    int maxValue;
    try {
      minValue = Integer.parseInt(min);
    } catch (NumberFormatException e) {
      return Optional.of(minErrorMessage);
    }
    try {
      maxValue = Integer.parseInt(max);
    } catch (NumberFormatException e) {
      return Optional.of(maxErrorMessage);
    }

    if (minValue < 0 || minValue > maxValue) {
      return Optional.of(minErrorMessage);
    } else if (maxValue > 1000) {
      return Optional.of(maxErrorMessage);
    }

    return Optional.empty();
  }

  private <E extends Enum<E>> String buildErrorMessage(E p, String details) {
    return wrongParameterMessage(p) + details;
  }

  private <E extends Enum<E>> String wrongParameterMessage(E p) {
    return String.format(WRONG_PARAMETER_VALUE_MESSAGE, p.name());
  }
}
