package bio.terra.landingzone.stairway.flight.create.resource.step;

import static bio.terra.landingzone.stairway.flight.utils.FlightUtils.maybeThrowAzureInterruptedException;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.MetricUtils;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.landingzone.stairway.flight.exception.translation.FlightExceptionTranslator;
import bio.terra.landingzone.stairway.flight.exception.utils.ManagementExceptionUtils;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseResourceCreateStep implements Step {
  private static final Logger logger = LoggerFactory.getLogger(BaseResourceCreateStep.class);

  protected static final String FAILED_TO_CREATE_RESOURCE =
      "Failed to create landing zone {} resource. landingZoneId={}. Error: {}";
  protected static final String RESOURCE_ALREADY_EXISTS =
      "{} resource in managed resource group {} already exists.";
  protected static final String RESOURCE_CREATED =
      "{} resource id='{}' in resource group '{}' successfully created.";
  private static final String RESOURCE_DELETED_OR_DOESNT_EXIST =
      "{} doesn't exist or has been already deleted. Id={}";
  private static final String FAILED_ATTEMPT_TO_DELETE_RESOURCE =
      "Failed attempt to delete {}. Id={}";
  private static final String RESOURCE_DELETED = "{} resource with id={} deleted.";

  protected final ResourceNameProvider resourceNameProvider;

  protected BaseResourceCreateStep(ResourceNameProvider resourceNameProvider) {
    this.resourceNameProvider = resourceNameProvider;
    registerForNameGeneration(resourceNameProvider, this);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var billingProfile =
        getParameterOrThrow(
            context.getWorkingMap(), LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);

    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var azureLandingZoneRequest =
        getParameterOrThrow(
            context.getInputParameters(),
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            LandingZoneRequest.class);
    try {
      var stepDuration =
          MetricUtils.configureTimerForLzStepDuration(
              azureLandingZoneRequest.definition(), getResourceType());
      var start = Instant.now().toEpochMilli();
      createResource(context, getArmManagers(context));
      var finish = Instant.now().toEpochMilli();
      stepDuration.record(Duration.ofMillis(finish - start));
    } catch (ManagementException e) {
      return handleManagementException(
          e,
          landingZoneId.toString(),
          azureLandingZoneRequest.definition(),
          billingProfile.getManagedResourceGroupId());
    } catch (RuntimeException e) {
      MetricUtils.incrementLandingZoneCreationFailure(azureLandingZoneRequest.definition());
      throw maybeThrowAzureInterruptedException(e);
    }
    return StepResult.getStepResultSuccess();
  }

  protected Optional<StepResult> maybeHandleManagementException(ManagementException e) {
    return Optional.empty();
  }

  @Override
  public StepResult undoStep(FlightContext flightContext) throws InterruptedException {
    var resourceId = getResourceId(flightContext);
    try {

      if (resourceId.isPresent()) {
        var armManagers = getArmManagers(flightContext);
        deleteResource(resourceId.get(), armManagers);
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

  protected abstract void deleteResource(String resourceId, ArmManagers armManagers);

  protected abstract String getResourceType();

  protected abstract Optional<String> getResourceId(FlightContext context);

  protected ArmManagers getArmManagers(FlightContext context) {
    return LandingZoneFlightBeanBag.getFromObject(context.getApplicationContext()).getArmManagers();
  }

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

  protected ParametersResolver getParametersResolver(FlightContext context) {
    return getParameterOrThrow(
        context.getWorkingMap(),
        LandingZoneFlightMapKeys.CREATE_LANDING_ZONE_PARAMETERS_RESOLVER,
        ParametersResolver.class);
  }

  private <T extends BaseResourceCreateStep> void registerForNameGeneration(
      ResourceNameProvider resourceNameProvider, T step) {
    resourceNameProvider.registerStep(step);
  }

  private StepResult handleManagementException(
      ManagementException e,
      String landingZoneId,
      String landingZoneType,
      String managedResourceGroup) {
    var handled = maybeHandleManagementException(e);
    if (handled.isPresent()) {
      var stepResult = handled.get();
      if (!stepResult.isSuccess()
          && stepResult.getStepStatus().equals(StepStatus.STEP_RESULT_FAILURE_FATAL)) {
        MetricUtils.incrementLandingZoneCreationFailure(landingZoneType);
      }
      return handled.get();
    }

    if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
      logger.info(RESOURCE_ALREADY_EXISTS, getResourceType(), managedResourceGroup);
      return StepResult.getStepResultSuccess();
    }
    logger.error(
        FAILED_TO_CREATE_RESOURCE,
        getResourceType(),
        landingZoneId,
        ManagementExceptionUtils.buildErrorInfo(e));
    MetricUtils.incrementLandingZoneCreationFailure(landingZoneType);
    return new StepResult(
        StepStatus.STEP_RESULT_FAILURE_FATAL, new FlightExceptionTranslator(e).translate());
  }
}
