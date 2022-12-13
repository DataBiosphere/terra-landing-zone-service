package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;

public interface InputParameterValidator {
  void validate(ParametersResolver parametersResolver) throws InvalidInputParameterException;
}
