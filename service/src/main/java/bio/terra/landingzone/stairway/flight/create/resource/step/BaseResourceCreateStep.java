package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
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

  // TODO: separate step for resourceGroup
  protected ResourceGroup resourceGroup;
  protected final ArmManagers armManagers;
  protected final ResourceNameGenerator resourceNameGenerator;
  protected final ParametersResolver parametersResolver;

  public BaseResourceCreateStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    this.armManagers = armManagers;
    this.parametersResolver = parametersResolver;
    this.resourceNameGenerator = resourceNameGenerator;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var billingProfile =
        getParameterOrThrow(
            context.getInputParameters(),
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            ProfileModel.class);
    var landingZoneTarget = LandingZoneTarget.fromBillingProfile(billingProfile);

    // TODO: introduce separate step for this
    resourceGroup =
        armManagers
            .azureResourceManager()
            .resourceGroups()
            .getByName(landingZoneTarget.azureResourceGroupId());

    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
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
    //    try {
    //      deleteResource(context, armManagers);
    //    } catch (ManagementException e) {
    //      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
    //        logger.error("{} doesn't exist or has been already deleted.", getResourceType());
    //        return StepResult.getStepResultSuccess();
    //      }
    //      logger.error("Failed attempt to delete {} resource in managed resource group.", vNetId);
    //      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    //    }
    //    return StepResult.getStepResultSuccess();
    return StepResult.getStepResultSuccess();
  }

  protected abstract void createResource(FlightContext context, ArmManagers armManagers);

  // protected abstract void deleteResource(FlightContext context, ArmManagers armManagers);

  protected abstract String getResourceType();

  protected <T> T getParameterOrThrow(FlightMap parameters, String name, Class<T> clazz) {
    // TODO: throw different exception
    FlightUtils.validateRequiredEntries(parameters, name);
    return parameters.get(name, clazz);
  }
}
