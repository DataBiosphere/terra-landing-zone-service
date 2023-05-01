package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.LandingZoneStepsDefinitionProviderFactory;
import bio.terra.landingzone.stairway.flight.StepsDefinitionProvider;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;

public class CreateLandingZoneResourcesFlight extends Flight {

  private final StepsDefinitionProvider stepsDefinitionProvider;
  private final LandingZoneRequest landingZoneRequest;
  private final ArmManagers armManagers;
  private final ResourceNameGenerator resourceNameGenerator;
  private final ParametersResolver parametersResolver;

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

    landingZoneRequest =
        inputParameters.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);
    stepsDefinitionProvider =
        LandingZoneStepsDefinitionProviderFactory.create(landingZoneRequest.definition());
    armManagers = initializeArmManagers(inputParameters, flightBeanBag.getAzureConfiguration());
    resourceNameGenerator =
        new ResourceNameGenerator(landingZoneRequest.landingZoneId().toString());

    parametersResolver =
        new ParametersResolver(landingZoneRequest.parameters(), LandingZoneDefaultParameters.get());

    addCreateSteps();
  }

  private void addCreateSteps() {
    stepsDefinitionProvider
        .get(armManagers, parametersResolver, resourceNameGenerator)
        .forEach(pair -> addStep(pair.getLeft(), pair.getRight()));
  }

  private ArmManagers initializeArmManagers(
      FlightMap inputParameters, LandingZoneAzureConfiguration azureConfiguration) {
    var billingProfile =
        inputParameters.get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);
    var landingZoneTarget = LandingZoneTarget.fromBillingProfile(billingProfile);
    var azureProfile =
        new AzureProfile(
            landingZoneTarget.azureTenantId(),
            landingZoneTarget.azureSubscriptionId(),
            AzureEnvironment.AZURE);
    var tokenCredentials =
        new ClientSecretCredentialBuilder()
            .clientId(azureConfiguration.getManagedAppClientId())
            .clientSecret(azureConfiguration.getManagedAppClientSecret())
            .tenantId(azureConfiguration.getManagedAppTenantId())
            .build();
    return LandingZoneManager.createArmManagers(tokenCredentials, azureProfile);
  }
}
