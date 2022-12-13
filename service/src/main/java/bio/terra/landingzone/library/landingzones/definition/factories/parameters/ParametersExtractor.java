package bio.terra.landingzone.library.landingzones.definition.factories.parameters;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import java.util.Arrays;
import java.util.List;

public class ParametersExtractor {
  public static List<String> extractAllowedMethods(ParametersResolver parametersResolver) {
    return Arrays.stream(
            parametersResolver
                .getValue(
                    StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS
                        .name())
                .split(","))
        .map(String::trim)
        .toList();
  }
}
