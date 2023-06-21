package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;

public interface InputParameterValidator {
  String WRONG_PARAMETER_VALUE_MESSAGE = "Value of the '%s' parameter is not valid.";

  void validate(ParametersResolver parametersResolver) throws InvalidInputParameterException;

  default <E extends Enum<E>> String buildErrorMessage(E p, String details) {
    return wrongParameterMessage(p) + details;
  }

  default <E extends Enum<E>> String wrongParameterMessage(E p) {
    return String.format(WRONG_PARAMETER_VALUE_MESSAGE, p.name());
  }
}
