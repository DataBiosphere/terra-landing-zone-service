package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.model.LandingZoneRecord;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    // Read input parameters
    final FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(
        inputMap,
        LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
        LandingZoneFlightMapKeys.LANDING_ZONE_ID,
        LandingZoneFlightMapKeys.BILLING_PROFILE);
    var requestedExternalLandingZoneResource =
        inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);
    var landingZoneId = inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var billingProfile = inputMap.get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);
    var landingZoneTarget = LandingZoneTarget.fromBillingProfile(billingProfile);

    // Persist the landing zone record
    landingZoneDao.createLandingZone(
        LandingZoneRecord.builder()
            .landingZoneId(landingZoneId)
            .definition(requestedExternalLandingZoneResource.definition())
            .version(requestedExternalLandingZoneResource.version())
            .description(
                String.format(
                    "Definition:%s Version:%s",
                    requestedExternalLandingZoneResource.definition(),
                    requestedExternalLandingZoneResource.version()))
            .displayName(requestedExternalLandingZoneResource.definition())
            .properties(requestedExternalLandingZoneResource.parameters())
            .resourceGroupId(landingZoneTarget.azureResourceGroupId())
            .tenantId(landingZoneTarget.azureTenantId())
            .subscriptionId(landingZoneTarget.azureSubscriptionId())
            .billingProfileId(requestedExternalLandingZoneResource.billingProfileId())
            .createdDate(OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .build());
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    final FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(inputMap, LandingZoneFlightMapKeys.LANDING_ZONE_ID);
    var landingZoneId = inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    landingZoneDao.deleteLandingZone(landingZoneId);
    return StepResult.getStepResultSuccess();
  }
}
