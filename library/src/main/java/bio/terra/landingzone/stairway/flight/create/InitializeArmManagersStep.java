package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.library.AzureCredentialsProvider;
import bio.terra.landingzone.library.configuration.AzureCustomerUsageConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;

public class InitializeArmManagersStep implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    FlightMap inputMap = context.getInputParameters();
    LandingZoneFlightBeanBag flightBeanBag =
        LandingZoneFlightBeanBag.getFromObject(context.getApplicationContext());
    var armManagers =
        initializeArmManagers(
            inputMap,
            flightBeanBag.getAzureCustomerUsageConfiguration(),
            flightBeanBag.getAzureCredentialsProvider());
    context.getWorkingMap().put(LandingZoneFlightMapKeys.ARM_MANAGERS_KEY, armManagers);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {

    return StepResult.getStepResultSuccess();
  }

  private ArmManagers initializeArmManagers(
      FlightMap inputParameters,
      AzureCustomerUsageConfiguration azureCustomerUsageConfiguration,
      AzureCredentialsProvider azureCredentialsProvider) {
    var billingProfile =
        inputParameters.get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);
    var landingZoneTarget = LandingZoneTarget.fromBillingProfile(billingProfile);
    var azureProfile =
        new AzureProfile(
            landingZoneTarget.azureTenantId(),
            landingZoneTarget.azureSubscriptionId(),
            AzureEnvironment.AZURE);
    var tokenCredentials = azureCredentialsProvider.getTokenCredential();
    return LandingZoneManager.createArmManagers(
        tokenCredentials, azureProfile, azureCustomerUsageConfiguration.getUsageAttribute());
  }
}
