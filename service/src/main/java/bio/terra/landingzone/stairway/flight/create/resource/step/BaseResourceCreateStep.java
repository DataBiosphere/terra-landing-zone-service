package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseResourceCreateStep implements Step {
  private static final Logger logger = LoggerFactory.getLogger(BaseResourceCreateStep.class);

  protected static final String FAILED_TO_CREATE_RESOURCE =
      "Failed to create landing zone {} resource. landingZoneId={}: {}";
  protected static final String RESOURCE_ALREADY_EXISTS =
      "{} resource in managed resource group {} already exists.";
  protected static final String RESOURCE_CREATED =
      "{} resource id='{}' in resource group '{}' successfully created.";
  private static final String RESOURCE_DELETED_OR_DOESNT_EXIST =
      "{} doesn't exist or has been already deleted. Id={}";
  private static final String FAILED_ATTEMPT_TO_DELETE_RESOURCE =
      "Failed attempt to delete {}. Id={}";
  private static final String RESOURCE_DELETED = "{} resource with id={} deleted.";

  protected final ArmManagers armManagers;
  protected final ResourceNameProvider resourceNameProvider;
  protected final ParametersResolver parametersResolver;

  protected BaseResourceCreateStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameProvider resourceNameProvider) {
    this.armManagers = armManagers;
    this.parametersResolver = parametersResolver;
    this.resourceNameProvider = resourceNameProvider;
    registerForNameGeneration(resourceNameProvider, this);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var billingProfile =
        getParameterOrThrow(
            context.getInputParameters(),
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            ProfileModel.class);

    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    try {
      createResource(context, armManagers);
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(
            RESOURCE_ALREADY_EXISTS, getResourceType(), billingProfile.getManagedResourceGroupId());
        return StepResult.getStepResultSuccess();
      }
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "Unauthorized")) {
        logger.info("Unauthorized to create resource, retrying.");
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY);
      }
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "BadRequest")) {
        logger.info("Bad request while creating resource, retrying.");
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY);
      }
      logger.error(
          FAILED_TO_CREATE_RESOURCE, getResourceType(), landingZoneId.toString(), e.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  protected Optional<StepResult> handleException(Exception e) {
    return Optional.empty();
  }

  @Override
  public StepResult undoStep(FlightContext flightContext) throws InterruptedException {
    var resourceId = getResourceId(flightContext);
    try {
      if (resourceId.isPresent()) {
        deleteResource(resourceId.get());
        logger.info(RESOURCE_DELETED, getResourceType(), resourceId.get());
      }
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        logger.error(RESOURCE_DELETED_OR_DOESNT_EXIST, getResourceType(), resourceId.orElse("n/a"));
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_ATTEMPT_TO_DELETE_RESOURCE, getResourceType(), resourceId.orElse("n/a"));
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }

  public abstract List<ResourceNameRequirements> getResourceNameRequirements();

  protected abstract void createResource(FlightContext context, ArmManagers armManagers);

  protected abstract void deleteResource(String resourceId);

  protected abstract String getResourceType();

  protected abstract Optional<String> getResourceId(FlightContext context);

  protected <T> T getParameterOrThrow(FlightMap parameters, String name, Class<T> clazz) {
    FlightUtils.validateRequiredEntries(parameters, name);
    return parameters.get(name, clazz);
  }

  protected String getMRGName(FlightContext context) {
    return getParameterOrThrow(
            context.getWorkingMap(),
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            TargetManagedResourceGroup.class)
        .name();
  }

  protected String getMRGRegionName(FlightContext context) {
    return getParameterOrThrow(
            context.getWorkingMap(),
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            TargetManagedResourceGroup.class)
        .region();
  }

  private <T extends BaseResourceCreateStep> void registerForNameGeneration(
      ResourceNameProvider resourceNameProvider, T step) {
    resourceNameProvider.registerStep(step);
  }
}
