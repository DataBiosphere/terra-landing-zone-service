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
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResetResourceGroupTagsStep implements Step {
  private static final Logger logger = LoggerFactory.getLogger(ResetResourceGroupTagsStep.class);
  private final LandingZoneManagerProvider landingZoneManagerProvider;
  private final ObjectMapper objectMapper;

  public ResetResourceGroupTagsStep(
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
      resetExistingTagsIfRequired(workingMap, billingProfile);

      return StepResult.getStepResultSuccess();
    } catch (Exception e) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
  }

  private void resetExistingTagsIfRequired(FlightMap workingMap, ProfileModel billingProfile)
      throws JsonProcessingException {
    AzureResourceManager azureResourceManager =
        landingZoneManagerProvider.createAzureResourceManagerClient(
            LandingZoneTarget.fromBillingProfile(billingProfile));

    Map<String, String> preexistingTags = getPreexistingTagsFromWorkingMap(workingMap);
    if (preexistingTags == null || preexistingTags.isEmpty()) return;

    logger.info("Preexisting tags were found. Tags:{}", preexistingTags);
    mergeAndSetTags(billingProfile, azureResourceManager, preexistingTags);
  }

  @Nullable
  private Map<String, String> getPreexistingTagsFromWorkingMap(FlightMap workingMap)
      throws JsonProcessingException {
    String mapValue = workingMap.get(RESOURCE_GROUP_TAGS, String.class);

    if (StringUtils.isEmpty(mapValue)) {
      return null;
    }

    return objectMapper.readValue(mapValue, Map.class);
  }

  private void mergeAndSetTags(
      ProfileModel billingProfile,
      AzureResourceManager azureResourceManager,
      Map<String, String> preexistingTags) {
    ResourceGroup resourceGroup =
        azureResourceManager.resourceGroups().getByName(billingProfile.getManagedResourceGroupId());
    Map<String, String> currentTags = resourceGroup.tags();

    Map<String, String> resourceTags = new HashMap<>();

    // order is important here, this means any value of a pre-existing tag
    // will be replaced with the current tag's value
    resourceTags.putAll(preexistingTags);
    resourceTags.putAll(currentTags);

    azureResourceManager.tagOperations().updateTags(resourceGroup, resourceTags);
    logger.info("Tags successfully set in the resource group. Tags:{}", resourceTags);
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
