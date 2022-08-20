package bio.terra.landingzone.resource.landingzone;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.model.LandingZone;
import bio.terra.landingzone.resource.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.resource.flight.exception.AzureLandingZoneIdNotFound;
import bio.terra.landingzone.resource.flight.utils.FlightUtils;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAzureLandingZoneDbRecordStep implements Step {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateAzureLandingZoneDbRecordStep.class);
  private final LandingZoneDao landingZoneDao;

  public CreateAzureLandingZoneDbRecordStep(LandingZoneDao landingZoneDao) {
    this.landingZoneDao = landingZoneDao;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    final FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(
        inputMap, LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS);

    var requestedExternalLandingZoneResource =
        inputMap.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, ExternalLandingZoneResource.class);
    var azureCloudContext = requestedExternalLandingZoneResource.getAzureCloudContext();

    if (!context
        .getWorkingMap()
        .containsKey(LandingZoneFlightMapKeys.DEPLOYED_AZURE_LANDING_ZONE_ID)) {
      logger.error(
          "Azure Landing Zone flight couldn't be completed. "
              + "Azure landing zone Id not found. FlightId: {}",
          context.getFlightId());
      return new StepResult(
          StepStatus.STEP_RESULT_FAILURE_FATAL,
          new AzureLandingZoneIdNotFound(
              String.format(
                  "Azure landing zone id not found. " + "FlightId: %s", context.getFlightId())));
    }

    String landingZoneId =
        context
            .getWorkingMap()
            .get(LandingZoneFlightMapKeys.DEPLOYED_AZURE_LANDING_ZONE_ID, String.class);

    landingZoneDao.createLandingZone(
        LandingZone.builder()
            .landingZoneId(UUID.fromString(landingZoneId))
            .definition(requestedExternalLandingZoneResource.getDefinition())
            .version(requestedExternalLandingZoneResource.getVersion())
            .description(requestedExternalLandingZoneResource.getDescription())
            .displayName(requestedExternalLandingZoneResource.getName())
            .properties(requestedExternalLandingZoneResource.getProperties())
            .resourceGroupId(azureCloudContext.getAzureResourceGroupId())
            .build());
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    final FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(
        inputMap, LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS);

    if (!context
        .getWorkingMap()
        .containsKey(LandingZoneFlightMapKeys.DEPLOYED_AZURE_LANDING_ZONE_ID)) {
      logger.error(
          "Azure Landing Zone flight couldn't be completed. "
              + "Deployed Azure landing zone Id not found. FlightId: {}",
          context.getFlightId());
      return new StepResult(
          StepStatus.STEP_RESULT_FAILURE_FATAL,
          new AzureLandingZoneIdNotFound(
              String.format(
                  "Azure landing zone id not found. " + "FlightId: %s", context.getFlightId())));
    }

    String landingZoneId =
        context
            .getWorkingMap()
            .get(LandingZoneFlightMapKeys.DEPLOYED_AZURE_LANDING_ZONE_ID, String.class);

    landingZoneDao.deleteLandingZone(UUID.fromString(landingZoneId));
    return StepResult.getStepResultSuccess();
  }
}
