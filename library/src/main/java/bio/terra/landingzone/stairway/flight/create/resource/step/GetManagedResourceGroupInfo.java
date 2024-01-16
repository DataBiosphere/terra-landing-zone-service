package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;

public class GetManagedResourceGroupInfo implements Step {
  public static final String TARGET_MRG_KEY = "TARGET_MRG";

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var armManagers =
        LandingZoneFlightBeanBag.getFromObject(context.getApplicationContext()).getArmManagers();
    var billingProfile =
        context
            .getInputParameters()
            .get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);

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
