package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.ProtectedDataAzureStorageHelper;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks in the provided administrative subscription + resource group for a suitable storage
 * account for long-term-storage of protected data logs.
 *
 * <p>Exactly one storage account must be present in the administrative resource group that is in
 * the same region as the source landing zone's resource group to avoid excessive egress costs. If
 * more than one or none match the criteria, this step will fail.
 */
public class FetchLongTermStorageAccountStep implements Step {
  private static final Logger logger =
      LoggerFactory.getLogger(FetchLongTermStorageAccountStep.class);
  private final ProtectedDataAzureStorageHelper azureStorageHelper;

  public FetchLongTermStorageAccountStep(ProtectedDataAzureStorageHelper azureStorageHelper) {

    this.azureStorageHelper = azureStorageHelper;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var lzMrgName = BaseResourceCreateStep.getMRGName(context);
    var filtered = azureStorageHelper.getMatchingAdminStorageAccounts(lzMrgName);

    if (filtered.size() > 1) {
      logger.error(
          "More than one protected data long term storage account found in target resource group");
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL);
    }

    if (filtered.isEmpty()) {
      logger.error("No protected data long term storage account found in target resource group");
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL);
    }

    context
        .getWorkingMap()
        .put(LandingZoneFlightMapKeys.PROTECTED_DATA_LTS_STORAGE_ACCT_ID, filtered.get(0).id());

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
