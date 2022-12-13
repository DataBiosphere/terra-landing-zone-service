package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.ParametersExtractor;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.StorageAccountBlobCorsParametersNames;
import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.storage.models.CorsRuleAllowedMethodsItem;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BlobCorsParametersValidator implements InputParameterValidator {
  static final String WRONG_PARAMETER_VALUE_MESSAGE = "Value of the '%s' parameter is not valid.";
  static final String HEADERS_INVALID_CHARACTERS = "[@(){}\\[\\]<>;:/\\\\]";

  private final Pattern headersInvalidCharactersPattern =
      Pattern.compile(HEADERS_INVALID_CHARACTERS);

  private final StringBuilder sbErrors = new StringBuilder();

  @Override
  public void validate(ParametersResolver parametersResolver)
      throws InvalidInputParameterException {
    Optional<String> maxAgeValidationMessage = validateMaxAge(parametersResolver);
    maxAgeValidationMessage.ifPresent(s -> sbErrors.append(s).append(" "));

    Optional<String> allowedMethodsValidationMessage = validateAllowedMethods(parametersResolver);
    allowedMethodsValidationMessage.ifPresent(s -> sbErrors.append(s).append(" "));

    Optional<String> allowedHeadersValidationMessage = validateAllowedHeaders(parametersResolver);
    allowedHeadersValidationMessage.ifPresent(s -> sbErrors.append(s).append(" "));

    Optional<String> exposedHeadersValidationMessage = validateExposedHeaders(parametersResolver);
    exposedHeadersValidationMessage.ifPresent(s -> sbErrors.append(s).append(" "));

    if (!sbErrors.isEmpty()) {
      throw new InvalidInputParameterException(sbErrors.toString());
    }
  }

  private Optional<String> validateMaxAge(ParametersResolver parametersResolver) {
    String maxAgeValue =
        parametersResolver.getValue(
            StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE.name());
    String errorMessage =
        buildErrorMessage(
            StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE,
            String.format(" The value must be a number between 0 and %s.", Integer.MAX_VALUE));

    int value;
    try {
      value = Integer.parseInt(maxAgeValue);
    } catch (NumberFormatException e) {
      return Optional.of(errorMessage);
    }
    if (value < 0) {
      return Optional.of(errorMessage);
    }
    return Optional.empty();
  }

  private Optional<String> validateAllowedMethods(ParametersResolver parametersResolver) {
    var allowedMethodsFromParameters =
        ParametersExtractor.extractAllowedMethods(parametersResolver);
    var validAllowedMethods =
        CorsRuleAllowedMethodsItem.values().stream()
            .map(ExpandableStringEnum::toString)
            .collect(Collectors.toSet());
    for (var method : allowedMethodsFromParameters) {
      if (!validAllowedMethods.contains(method)) {
        return Optional.of(
            buildErrorMessage(
                StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS,
                String.format(
                    " The list of allowed values:'%s'.", getCsvListOfAllowedMethodValues())));
      }
    }
    return Optional.empty();
  }

  private Optional<String> validateExposedHeaders(ParametersResolver parametersResolver) {
    return validateHeaders(
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS,
        parametersResolver.getValue(
            StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS
                .name()));
  }

  private Optional<String> validateAllowedHeaders(ParametersResolver parametersResolver) {
    return validateHeaders(
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS,
        parametersResolver.getValue(
            StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS
                .name()));
  }

  private Optional<String> validateHeaders(
      StorageAccountBlobCorsParametersNames headerParamName, String headersValue) {
    Matcher m = headersInvalidCharactersPattern.matcher(headersValue);
    if (m.find()) {
      return Optional.of(
          buildErrorMessage(
              headerParamName,
              String.format(
                  " Value contains one of the forbidden characters '%s'.",
                  HEADERS_INVALID_CHARACTERS)));
    }
    return Optional.empty();
  }

  private String buildErrorMessage(StorageAccountBlobCorsParametersNames p, String details) {
    return wrongParameterMessage(p) + details;
  }

  private String wrongParameterMessage(StorageAccountBlobCorsParametersNames p) {
    return String.format(WRONG_PARAMETER_VALUE_MESSAGE, p.name());
  }

  private String getCsvListOfAllowedMethodValues() {
    return CorsRuleAllowedMethodsItem.values().stream()
        .map(ExpandableStringEnum::toString)
        .collect(Collectors.joining(","));
  }
}
