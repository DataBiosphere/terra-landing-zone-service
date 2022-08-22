package bio.terra.landingzone.resource.landingzone;

import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.library.AzureLandingZoneManagerProvider;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.resource.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.resource.flight.utils.FlightUtils;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneResource;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAzureExternalLandingZoneStep implements Step {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateAzureExternalLandingZoneStep.class);

  private final LandingZoneService landingZoneService;
  private final AzureLandingZoneManagerProvider azureLandingZoneManagerProvider;

  public CreateAzureExternalLandingZoneStep(
      LandingZoneService landingZoneService,
      AzureLandingZoneManagerProvider azureLandingZoneManagerProvider) {
    this.landingZoneService = landingZoneService;
    this.azureLandingZoneManagerProvider = azureLandingZoneManagerProvider;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(
        inputMap,
        LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
        LandingZoneFlightMapKeys.LANDING_ZONE_AZURE_CONFIGURATION);

    var requestedExternalLandingZoneResource =
        inputMap.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, JobLandingZoneDefinition.class);
    var azureConfiguration =
        inputMap.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_AZURE_CONFIGURATION,
            LandingZoneAzureConfiguration.class);

    var azureLandingZoneRequest =
        AzureLandingZoneRequest.builder()
            .definition(requestedExternalLandingZoneResource.getDefinition())
            .version(requestedExternalLandingZoneResource.getVersion())
            .parameters(requestedExternalLandingZoneResource.getProperties())
            .build();
    try {
      var tokenCredential = buildTokenCredential(azureConfiguration);
      AzureLandingZone createdAzureLandingZone =
          landingZoneService.createLandingZone(
              azureLandingZoneRequest,
              azureLandingZoneManagerProvider.createLandingZoneManager(
                  tokenCredential, requestedExternalLandingZoneResource.getAzureCloudContext()));

      // save for the next step
      context
          .getWorkingMap()
          .put(
              LandingZoneFlightMapKeys.DEPLOYED_AZURE_LANDING_ZONE_ID,
              createdAzureLandingZone.getId());

      persistResponse(context, createdAzureLandingZone);

      logger.info(
          "Successfully created Azure landing zone. id='{}', deployed resources='{}'",
          createdAzureLandingZone.getId(),
          listDeployedResourcesAsCsv(createdAzureLandingZone.getDeployedResources()));
    } catch (Exception e) {
      // TODO SG: check if we can retry?
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return null;
  }

  private String listDeployedResourcesAsCsv(List<AzureLandingZoneResource> deployedResources) {
    if (deployedResources == null || deployedResources.isEmpty()) {
      return "No resource found!";
    }
    return deployedResources.stream()
        .map(AzureLandingZoneResource::getResourceId)
        .collect(Collectors.joining());
  }

  private void persistResponse(FlightContext context, AzureLandingZone createdAzureLandingZone) {
    FlightMap workingMap = context.getWorkingMap();
    workingMap.put(JobMapKeys.RESPONSE.getKeyName(), createdAzureLandingZone);
  }

  private TokenCredential buildTokenCredential(LandingZoneAzureConfiguration azureConfiguration) {
    return new ClientSecretCredentialBuilder()
        .clientId(azureConfiguration.getManagedAppClientId())
        .clientSecret(azureConfiguration.getManagedAppClientSecret())
        .tenantId(azureConfiguration.getManagedAppTenantId())
        .build();
  }
}
