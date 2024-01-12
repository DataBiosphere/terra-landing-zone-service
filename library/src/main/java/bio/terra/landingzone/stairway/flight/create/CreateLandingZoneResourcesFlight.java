package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.AzureCredentialsProvider;
import bio.terra.landingzone.library.configuration.AzureCustomerUsageConfiguration;
import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneStepsDefinitionProviderFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionFactoryType;
import bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionProvider;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ParametersResolverProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.create.resource.step.AggregateLandingZoneResourcesStep;
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import java.util.UUID;

public class CreateLandingZoneResourcesFlight extends Flight {

  private final StepsDefinitionProvider stepsDefinitionProvider;
  private final LandingZoneRequest landingZoneRequest;
  private final ArmManagers armManagers;
  private final ResourceNameProvider resourceNameProvider;
  private final ParametersResolverProvider parametersResolverProvider;
  private final LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration;
  private final AzureCredentialsProvider azureCredentialsProvider;

  /**
   * All subclasses must provide a constructor with this signature.
   *
   * @param inputParameters FlightMap of the inputs for the flight
   * @param applicationContext Anonymous context meaningful to the application using Stairway
   */
  public CreateLandingZoneResourcesFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);

    final LandingZoneFlightBeanBag flightBeanBag =
        LandingZoneFlightBeanBag.getFromObject(applicationContext);

    azureCredentialsProvider = flightBeanBag.getAzureCredentialsProvider();

    landingZoneRequest =
        inputParameters.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);
    if (landingZoneRequest == null) {
      throw new LandingZoneCreateException("Unable to find requested landing zone in input map");
    }

    landingZoneProtectedDataConfiguration =
        flightBeanBag.getLandingZoneProtectedDataConfiguration();

    var landingZoneId = getLandingZoneId(inputParameters, landingZoneRequest);
    resourceNameProvider = new ResourceNameProvider(landingZoneId);

    stepsDefinitionProvider =
        LandingZoneStepsDefinitionProviderFactory.create(
            StepsDefinitionFactoryType.fromString(landingZoneRequest.definition()));
    armManagers =
        initializeArmManagers(inputParameters, flightBeanBag.getAzureCustomerUsageConfiguration());
    parametersResolverProvider = flightBeanBag.getParametersResolverProvider();

    addCreateSteps();
  }

  private void addCreateSteps() {
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

  private ArmManagers initializeArmManagers(
      FlightMap inputParameters, AzureCustomerUsageConfiguration azureCustomerUsageConfiguration) {
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
}
