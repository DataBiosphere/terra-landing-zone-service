package bio.terra.landingzone.resource.landingzone;

import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.resource.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.resource.flight.utils.FlightUtils;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class CreateAzureExternalLandingZoneStep implements Step {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateAzureExternalLandingZoneStep.class);

  private final LandingZoneService landingZoneService;
  private final LandingZoneManagerProvider landingZoneManagerProvider;

  public CreateAzureExternalLandingZoneStep(
      LandingZoneService landingZoneService,
      LandingZoneManagerProvider landingZoneManagerProvider) {
    this.landingZoneService = landingZoneService;
    this.landingZoneManagerProvider = landingZoneManagerProvider;
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
        LandingZoneRequest.builder()
            .definition(requestedExternalLandingZoneResource.getDefinition())
            .version(requestedExternalLandingZoneResource.getVersion())
            .parameters(requestedExternalLandingZoneResource.getProperties())
            .build();
    try {
      var tokenCredential = buildTokenCredential(azureConfiguration);
      LandingZone createdLandingZone =
          landingZoneService.createLandingZone(
              azureLandingZoneRequest,
              landingZoneManagerProvider.createLandingZoneManager(
                  tokenCredential, requestedExternalLandingZoneResource.getAzureCloudContext()));

      // save for the next step
      context
          .getWorkingMap()
          .put(LandingZoneFlightMapKeys.DEPLOYED_AZURE_LANDING_ZONE_ID, createdLandingZone.getId());

      persistResponse(context, createdLandingZone);

      logger.info(
          "Successfully created Azure landing zone. id='{}', deployed resources='{}'",
          createdLandingZone.getId(),
          listDeployedResourcesAsCsv(createdLandingZone.getDeployedResources()));
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

  private String listDeployedResourcesAsCsv(List<LandingZoneResource> deployedResources) {
    if (deployedResources == null || deployedResources.isEmpty()) {
      return "No resource found!";
    }
    return deployedResources.stream()
        .map(LandingZoneResource::getResourceId)
        .collect(Collectors.joining());
  }

  private void persistResponse(FlightContext context, LandingZone createdLandingZone) {
    FlightMap workingMap = context.getWorkingMap();
    workingMap.put(JobMapKeys.RESPONSE.getKeyName(), createdLandingZone);
  }

  private TokenCredential buildTokenCredential(LandingZoneAzureConfiguration azureConfiguration) {
    return new ClientSecretCredentialBuilder()
        .clientId(azureConfiguration.getManagedAppClientId())
        .clientSecret(azureConfiguration.getManagedAppClientSecret())
        .tenantId(azureConfiguration.getManagedAppTenantId())
        .build();
  }
}
