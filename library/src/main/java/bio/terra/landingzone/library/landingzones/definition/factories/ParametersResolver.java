package bio.terra.landingzone.library.landingzones.definition.factories;

import java.util.Map;

public record ParametersResolver(Map<String, String> parameters) {

  public String getValue(String parameterName) {
    return parameters.getOrDefault(parameterName, "");
  }
}
