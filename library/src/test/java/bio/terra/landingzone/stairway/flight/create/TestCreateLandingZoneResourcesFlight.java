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
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.*;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import java.util.UUID;

/**
 * This is a test flight only. It is intended to test the creation of LZ resources in an isolation
 * without Sam and Db interactions.
 */
public class TestCreateLandingZoneResourcesFlight extends Flight {

  public TestCreateLandingZoneResourcesFlight(
      FlightMap inputParameters, Object applicationContext) {
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

    var stepsDefinitionProvider =
        LandingZoneStepsDefinitionProviderFactory.create(
            StepsDefinitionFactoryType.fromString(requestedLandingZone.definition()));
    var landingZoneId = inputParameters.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

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
            AzureEnvironment.AZURE);
    return LandingZoneManager.createArmManagers(
        flightBeanBag.getAzureCredentialsProvider().getTokenCredential(),
        azureProfile,
        flightBeanBag.getAzureCustomerUsageConfiguration().getUsageAttribute());
  }
}
