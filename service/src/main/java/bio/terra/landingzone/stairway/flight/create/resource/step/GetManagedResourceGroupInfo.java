package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;

public class GetManagedResourceGroupInfo implements Step {
  public static final String MRG_NAME_KEY = "MRG_NAME";
  public static final String MRG_REGION_NAME_KEY = "MRG_REGION_NAME";

  private final ArmManagers armManagers;

  public GetManagedResourceGroupInfo(ArmManagers armManagers) {
    this.armManagers = armManagers;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var billingProfile =
        context
            .getInputParameters()
            .get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);
    var resourceGroup =
        armManagers
            .azureResourceManager()
            .resourceGroups()
            .getByName(billingProfile.getManagedResourceGroupId());

    context.getWorkingMap().put(MRG_NAME_KEY, resourceGroup.name());
    context.getWorkingMap().put(MRG_REGION_NAME_KEY, resourceGroup.regionName());

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
