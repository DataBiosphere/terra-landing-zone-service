package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.ParametersExtractor;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.StorageAccountBlobCorsParametersNames;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import com.azure.resourcemanager.storage.models.CorsRule;
import com.azure.resourcemanager.storage.models.CorsRuleAllowedMethodsItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateStorageAccountCorsRules extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateStorageAccountCorsRules.class);

  public CreateStorageAccountCorsRules(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // will be removed as part of storage account deletion
    return StepResult.getStepResultSuccess();
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var storageAccountName =
        getParameterOrThrow(
            context.getWorkingMap(), LandingZoneFlightMapKeys.STORAGE_ACCOUNT_NAME, String.class);

    var corsRules = buildStorageAccountBlobServiceCorsRules(parametersResolver);
    var rules =
        armManagers
            .azureResourceManager()
            .storageBlobServices()
            .define("blobCorsConfiguration")
            .withExistingStorageAccount(getMRGName(context), storageAccountName)
            .withCORSRules(corsRules)
            .create();
    logger.info(RESOURCE_CREATED, getResourceType(), rules.id(), getMRGName(context));
  }

  @Override
  protected String getResourceType() {
    return "StorageAccountCorsRules";
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
