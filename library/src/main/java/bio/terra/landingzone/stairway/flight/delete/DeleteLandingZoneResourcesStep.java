package bio.terra.landingzone.stairway.flight.delete;

import static bio.terra.landingzone.stairway.flight.utils.FlightUtils.maybeThrowAzureInterruptedException;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.exception.LandingZoneNotFoundException;
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
import com.azure.core.management.exception.ManagementException;
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
  public StepResult doStep(FlightContext context) throws RetryException, InterruptedException {
    // Read input parameters
    FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(inputMap, LandingZoneFlightMapKeys.LANDING_ZONE_ID);
    var landingZoneId = inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    LandingZoneRecord landingZoneRecord;
    try {
      // Look up the landing zone record from the database
      landingZoneRecord = landingZoneDao.getLandingZoneRecord(landingZoneId);
    } catch (LandingZoneNotFoundException e) {
      logger.error("Landing zone not found. id={}", landingZoneId, e);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }

    try {
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

      return StepResult.getStepResultSuccess();
    } catch (ManagementException e) {
      // Azure returns AuthorizationFailed when an MRG is deleted or otherwise inaccessible. Since
      // the user is unable to change the IAM permissions on an MRG due to deny assignments, we
      // infer that the MRG is gone and move on with the deletion process.
      if (e.getValue().getCode().equals("AuthorizationFailed")) {
        logger.warn(
            "Landing zone MRG is either inaccessible or has been removed. id = '{}'",
            landingZoneId,
            e);
        persistResponse(
            context,
            DeletedLandingZone.emptyLandingZone(
                landingZoneId, landingZoneRecord.billingProfileId()));
        return StepResult.getStepResultSuccess();
      } else {
        logger.error("Failed to delete the landing zone due to Azure error.", e);
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
      }
    } catch (LandingZoneRuleDeleteException e) {
      logger.error("Failed to delete the landing zone due to delete rules.", e);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    } catch (RuntimeException maybeInterrupt) {
      var notInterrupt = maybeThrowAzureInterruptedException(maybeInterrupt);
      logger.error("Unexpected exception while deleting the landing zone.", notInterrupt);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, notInterrupt);
    }
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
      return DeletedLandingZone.emptyLandingZone(landingZoneId, billingProfileId);
    }

    List<String> deletedResources = landingZoneManager.deleteResources(landingZoneId.toString());
    return new DeletedLandingZone(landingZoneId, deletedResources, billingProfileId);
  }

  private boolean isAttached(LandingZoneRecord record) {
    return Boolean.parseBoolean(
        record.properties().getOrDefault(LandingZoneFlightMapKeys.ATTACH, "false"));
  }
}
