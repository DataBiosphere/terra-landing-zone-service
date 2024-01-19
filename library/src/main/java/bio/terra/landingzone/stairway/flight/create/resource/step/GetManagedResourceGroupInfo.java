package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;

public class GetManagedResourceGroupInfo implements Step {
  public static final String TARGET_MRG_KEY = "TARGET_MRG";

  private final ArmManagers armManagers;

  public GetManagedResourceGroupInfo(ArmManagers armManagers) {
    this.armManagers = armManagers;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var billingProfile =
        FlightUtils.getRequired(
            context.getInputParameters(),
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            ProfileModel.class);

    var resourceGroup =
        armManagers
            .azureResourceManager()
            .resourceGroups()
            .getByName(billingProfile.getManagedResourceGroupId());

    context
        .getWorkingMap()
        .put(
            TARGET_MRG_KEY,
            new TargetManagedResourceGroup(resourceGroup.name(), resourceGroup.regionName()));

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
