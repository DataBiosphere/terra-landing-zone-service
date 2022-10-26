package bio.terra.landingzone.stairway.flight.create;

import static bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys.RESOURCE_GROUP_TAGS;

import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadResourceGroupTagsStep implements Step {
  private static final Logger logger = LoggerFactory.getLogger(ReadResourceGroupTagsStep.class);
  private final LandingZoneManagerProvider landingZoneManagerProvider;
  private final ObjectMapper objectMapper;

  public ReadResourceGroupTagsStep(
      LandingZoneManagerProvider landingZoneManagerProvider, ObjectMapper objectMapper) {
    this.landingZoneManagerProvider = landingZoneManagerProvider;
    this.objectMapper = objectMapper;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {

    // Read billing profile from  working map parameters
    FlightMap workingMap = context.getWorkingMap();
    FlightUtils.validateRequiredEntries(workingMap, LandingZoneFlightMapKeys.BILLING_PROFILE);
    var billingProfile =
        workingMap.get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);

    try {
      var azureResourceManager =
          landingZoneManagerProvider.createAzureResourceManagerClient(
              LandingZoneTarget.fromBillingProfile(billingProfile));
      Map<String, String> tags =
          azureResourceManager
              .resourceGroups()
              .getByName(billingProfile.getManagedResourceGroupId())
              .tags();

      if (tags != null && !tags.isEmpty()) {
        logger.info(
            "Tags where found in the resource group: {}. Saving the flight context. Tags: {}",
            billingProfile.getManagedResourceGroupId(),
            tags);
        context.getWorkingMap().put(RESOURCE_GROUP_TAGS, objectMapper.writeValueAsString(tags));
      }

      return StepResult.getStepResultSuccess();
    } catch (Exception e) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
