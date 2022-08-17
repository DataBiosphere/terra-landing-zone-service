package bio.terra.landingzone.library.landingzones.definition.factories;

import java.util.HashMap;
import java.util.Map;

public class ParametersResolver {
  private final Map<String, String> defaultParameters;
  private final Map<String, String> parameters;

  public ParametersResolver(Map<String, String> parameters, Map<String, String> defaultParameters) {
    if (defaultParameters == null) {
      this.defaultParameters = new HashMap<>();
    } else {
      this.defaultParameters = defaultParameters;
    }

    this.parameters = parameters;
  }

  public String getValue(String parameterName) {
    if (parameters == null) {
      return defaultParameters.getOrDefault(parameterName, "");
    }

    return parameters.getOrDefault(
        parameterName, defaultParameters.getOrDefault(parameterName, ""));
  }
}
