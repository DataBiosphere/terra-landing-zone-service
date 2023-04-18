package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.ParametersExtractor;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.StorageAccountBlobCorsParametersNames;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.storage.models.CorsRule;
import com.azure.resourcemanager.storage.models.CorsRuleAllowedMethodsItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateStorageAccountCorsRules extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateStorageAccountCorsRules.class);

  public CreateStorageAccountCorsRules(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);

    var storageAccountName =
        getParameterOrThrow(
            context.getWorkingMap(), LandingZoneFlightMapKeys.STORAGE_ACCOUNT_NAME, String.class);
    try {
      var corsRules = buildStorageAccountBlobServiceCorsRules(parametersResolver);
      armManagers
          .azureResourceManager()
          .storageBlobServices()
          .define("blobCorsConfiguration")
          .withExistingStorageAccount(resourceGroup.name(), storageAccountName)
          .withCORSRules(corsRules)
          .create();
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(
            RESOURCE_ALREADY_EXISTS, "CORS rules", storageAccountName, resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "cors rules", landingZoneId.toString());
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // rollback here or in case of sub-flight do it there
    return StepResult.getStepResultSuccess();
  }

  /**
   * Build list of Cors rules for storage account
   *
   * @param parametersResolver
   * @return List of Cors rules
   */
  private ArrayList<CorsRule> buildStorageAccountBlobServiceCorsRules(
      ParametersResolver parametersResolver) {
    ArrayList<CorsRule> corsRules = new ArrayList<>();

    var rule = new CorsRule();
    var origins =
        Arrays.stream(
                parametersResolver
                    .getValue(
                        StorageAccountBlobCorsParametersNames
                            .STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS
                            .name())
                    .split(","))
            .toList();
    rule.withAllowedOrigins(origins);

    var methods =
        ParametersExtractor.extractAllowedMethods(parametersResolver).stream()
            .map(CorsRuleAllowedMethodsItem::fromString)
            .toList();
    rule.withAllowedMethods(methods);

    var headers =
        Arrays.stream(
                parametersResolver
                    .getValue(
                        StorageAccountBlobCorsParametersNames
                            .STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS
                            .name())
                    .split(","))
            .toList();
    rule.withAllowedHeaders(headers);

    List<String> expHeaders =
        Arrays.stream(
                parametersResolver
                    .getValue(
                        StorageAccountBlobCorsParametersNames
                            .STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS
                            .name())
                    .split(","))
            .toList();
    rule.withExposedHeaders(expHeaders);

    rule =
        rule.withMaxAgeInSeconds(
            Integer.parseInt(
                parametersResolver.getValue(
                    StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE
                        .name())));

    corsRules.add(rule);
    return corsRules;
  }
}
