package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneStepsDefinitionProviderFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionFactoryType;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.create.resource.step.AggregateLandingZoneResourcesStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetManagedResourceGroupInfo;
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.*;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import java.util.UUID;

/** Flight for creation of a Landing Zone */
public class CreateLandingZoneFlight extends Flight {

  @Override
  public void addStep(Step step, RetryRule retryRule) {
    super.addStep(step, retryRule);
  }

  /**
   * All subclasses must provide a constructor with this signature.
   *
   * @param inputParameters FlightMap of the inputs for the flight
   * @param applicationContext Anonymous context meaningful to the application using Stairway
   */
  public CreateLandingZoneFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);
    final LandingZoneFlightBeanBag flightBeanBag =
        LandingZoneFlightBeanBag.getFromObject(applicationContext);

    addCreateSteps(flightBeanBag, inputParameters);
  }

  private void addCreateSteps(LandingZoneFlightBeanBag flightBeanBag, FlightMap inputParameters) {
    var requestedLandingZone =
        inputParameters.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);
    if (requestedLandingZone == null) {
      throw new LandingZoneCreateException("Unable to find requested landing zone in input map");
    }

    addStep(
        new CreateSamResourceStep(flightBeanBag.getSamService()), RetryRules.shortExponential());

    if (!requestedLandingZone.isAttaching()) {
      var stepsDefinitionProvider =
          LandingZoneStepsDefinitionProviderFactory.create(
              StepsDefinitionFactoryType.fromString(requestedLandingZone.definition()));
      var landingZoneId = getLandingZoneId(inputParameters, requestedLandingZone);
      var resourceNameProvider = new ResourceNameProvider(landingZoneId);
      var parametersResolverProvider = flightBeanBag.getParametersResolverProvider();

      var landingZoneProtectedDataConfiguration =
          flightBeanBag.getLandingZoneProtectedDataConfiguration();
      var armManagers = getArmManagers(flightBeanBag, inputParameters);

      stepsDefinitionProvider
          .get(
              armManagers,
              parametersResolverProvider,
              resourceNameProvider,
              landingZoneProtectedDataConfiguration)
          .forEach(pair -> addStep(pair.getLeft(), pair.getRight()));

      // last step to aggregate results
      addStep(new AggregateLandingZoneResourcesStep(), RetryRules.shortExponential());
    } else {
      // GetManagedResourceGroupInfo is needed when we *are* attaching, in order to create the db
      // record
      // armManagers is instantiated here separately to avoid disrupting tests that run on
      // non-attach mode
      // and that are expected to error before the armManagers are instantiated
      // If we moved it outside the conditional block, it would happen before other logic in that
      // branch,
      // and fail for unexpected reasons
      var armManagers = getArmManagers(flightBeanBag, inputParameters);
      addStep(new GetManagedResourceGroupInfo(armManagers), RetryRules.cloud());
    }

    addStep(new CreateAzureLandingZoneDbRecordStep(), RetryRules.shortDatabase());
  }

  private UUID getLandingZoneId(FlightMap inputParameters, LandingZoneRequest landingZoneRequest) {
    // landing zone identifier can come in request's body or we generate it and keep it separately
    if (landingZoneRequest.landingZoneId().isPresent()) {
      return landingZoneRequest.landingZoneId().get();
    } else {
      var landingZoneId = inputParameters.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
      if (landingZoneId == null) {
        throw new LandingZoneCreateException("Unable to find landing zone identifier in input map");
      }
      return landingZoneId;
    }
  }

  protected ArmManagers getArmManagers(
      LandingZoneFlightBeanBag flightBeanBag, FlightMap inputParameters) {
    var billingProfile =
        inputParameters.get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);
    var landingZoneTarget = LandingZoneTarget.fromBillingProfile(billingProfile);
    var azureProfile =
        new AzureProfile(
            landingZoneTarget.azureTenantId(),
            landingZoneTarget.azureSubscriptionId(),
            flightBeanBag.getAzureConfiguration().getAzureEnvironment());
    return LandingZoneManager.createArmManagers(
        flightBeanBag.getAzureCredentialsProvider().getTokenCredential(),
        azureProfile,
        flightBeanBag.getAzureCustomerUsageConfiguration().getUsageAttribute());
  }
}
