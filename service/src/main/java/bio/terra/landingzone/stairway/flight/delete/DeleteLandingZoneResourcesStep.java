package bio.terra.landingzone.stairway.flight.delete;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.model.LandingZoneRecord;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.service.landingzone.azure.model.DeletedLandingZone;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteLandingZoneResourcesStep implements Step {

  private static final Logger logger =
      LoggerFactory.getLogger(DeleteLandingZoneResourcesStep.class);

  private final LandingZoneManagerProvider landingZoneManagerProvider;
  private final LandingZoneDao landingZoneDao;

  public DeleteLandingZoneResourcesStep(
      LandingZoneManagerProvider landingZoneManagerProvider, LandingZoneDao landingZoneDao) {
    this.landingZoneManagerProvider = landingZoneManagerProvider;
    this.landingZoneDao = landingZoneDao;
  }

  @Override
  public StepResult doStep(FlightContext context) throws RetryException {
    // Read input parameters
    FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(inputMap, LandingZoneFlightMapKeys.LANDING_ZONE_ID);
    var landingZoneId = inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    try {
      // Look up the landing zone record from the database
      LandingZoneRecord landingZoneRecord = landingZoneDao.getLandingZoneRecord(landingZoneId);

      LandingZoneTarget landingZoneTarget =
          new LandingZoneTarget(
              landingZoneRecord.tenantId(),
              landingZoneRecord.subscriptionId(),
              landingZoneRecord.resourceGroupId());

      // Delete the landing zone resources
      DeletedLandingZone deletedLandingZone =
          deleteLandingZoneResources(
              landingZoneId,
              landingZoneRecord.billingProfileId(),
              landingZoneManagerProvider.createLandingZoneManager(landingZoneTarget),
              isAttached(landingZoneRecord));

      persistResponse(context, deletedLandingZone);

      String deletedResources = String.join(", ", deletedLandingZone.deleteResources());
      logger.info(
          "Successfully deleted landing zone resources. id='{}', deleted resources='{}'",
          deletedLandingZone.landingZoneId(),
          deletedResources);

    } catch (LandingZoneRuleDeleteException e) {
      logger.error("Failed to delete the landing zone due to delete rules.", e);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    } catch (Exception e) {
      logger.error("Unexpected exception while deleting the landing zone.", e);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    return StepResult.getStepResultSuccess();
  }

  private void persistResponse(FlightContext context, DeletedLandingZone deletedLandingZone) {
    FlightMap workingMap = context.getWorkingMap();
    workingMap.put(JobMapKeys.RESPONSE.getKeyName(), deletedLandingZone);
  }

  private DeletedLandingZone deleteLandingZoneResources(
      UUID landingZoneId,
      UUID billingProfileId,
      LandingZoneManager landingZoneManager,
      boolean isAttached)
      throws LandingZoneRuleDeleteException {
    if (isAttached) {
      logger.info("Landing zone {} was attached, skipping Azure resource deletion", landingZoneId);
      return new DeletedLandingZone(landingZoneId, Collections.emptyList(), billingProfileId);
    }

    List<String> deletedResources = landingZoneManager.deleteResources(landingZoneId.toString());
    return new DeletedLandingZone(landingZoneId, deletedResources, billingProfileId);
  }

  private boolean isAttached(LandingZoneRecord record) {
    return Boolean.parseBoolean(
        record.properties().getOrDefault(LandingZoneFlightMapKeys.ATTACH, "false"));
  }
}
