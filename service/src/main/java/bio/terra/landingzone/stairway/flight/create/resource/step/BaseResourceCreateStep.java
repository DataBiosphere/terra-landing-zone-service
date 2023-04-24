package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseResourceCreateStep implements Step {
  private static final Logger logger = LoggerFactory.getLogger(BaseResourceCreateStep.class);

  protected static final String FAILED_TO_CREATE_RESOURCE =
      "Failed to create landing zone {} resource. landingZoneId={}.";
  protected static final String RESOURCE_ALREADY_EXISTS =
      "{} resource in managed resource group {} already exists.";
  protected static final String RESOURCE_CREATED =
      "{} resource id='{}' in resource group '{}' successfully created.";

  protected final LandingZoneAzureConfiguration landingZoneAzureConfiguration;
  protected final ResourceNameGenerator resourceNameGenerator;

  // initialized in doStep
  protected UUID landingZoneId;
  protected ArmManagers armManagers;
  protected ResourceGroup resourceGroup;
  protected ParametersResolver parametersResolver;

  public BaseResourceCreateStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    this.landingZoneAzureConfiguration = landingZoneAzureConfiguration;
    this.resourceNameGenerator = resourceNameGenerator;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var billingProfile =
        getParameterOrThrow(
            context.getInputParameters(),
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            ProfileModel.class);
    var landingZoneTarget = LandingZoneTarget.fromBillingProfile(billingProfile);

    var azureProfile =
        new AzureProfile(
            landingZoneTarget.azureTenantId(),
            landingZoneTarget.azureSubscriptionId(),
            AzureEnvironment.AZURE);
    var tokenCredentials =
        new ClientSecretCredentialBuilder()
            .clientId(landingZoneAzureConfiguration.getManagedAppClientId())
            .clientSecret(landingZoneAzureConfiguration.getManagedAppClientSecret())
            .tenantId(landingZoneAzureConfiguration.getManagedAppTenantId())
            .build();
    armManagers = LandingZoneManager.createArmManagers(tokenCredentials, azureProfile);

    // TODO: introduce separate step for this
    resourceGroup =
        armManagers
            .azureResourceManager()
            .resourceGroups()
            .getByName(landingZoneTarget.azureResourceGroupId());
    var requestedLandingZone =
        getParameterOrThrow(
            context.getInputParameters(),
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            LandingZoneRequest.class);

    parametersResolver =
        new ParametersResolver(
            requestedLandingZone.parameters(), LandingZoneDefaultParameters.get());

    try {
      createResource(context, armManagers);
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(RESOURCE_ALREADY_EXISTS, getResourceType(), resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, getResourceType(), landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }

  protected abstract void createResource(FlightContext context, ArmManagers armManagers);

  protected abstract String getResourceType();

  protected <T> T getParameterOrThrow(FlightMap parameters, String name, Class<T> clazz) {
    // TODO: throw different exception
    FlightUtils.validateRequiredEntries(parameters, name);
    return parameters.get(name, clazz);
  }
}
