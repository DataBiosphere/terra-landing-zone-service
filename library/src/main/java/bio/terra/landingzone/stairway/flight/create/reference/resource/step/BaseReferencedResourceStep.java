package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import static bio.terra.landingzone.stairway.flight.utils.FlightUtils.maybeThrowAzureInterruptedException;

import bio.terra.landingzone.common.utils.MetricUtils;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetManagedResourceGroupInfo;
import bio.terra.landingzone.stairway.flight.exception.translation.FlightExceptionTranslator;
import bio.terra.landingzone.stairway.flight.exception.utils.ManagementExceptionUtils;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.stairway.*;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.GenericResource;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseReferencedResourceStep implements Step {
  private static final Logger logger = LoggerFactory.getLogger(BaseReferencedResourceStep.class);

  public static final String REFERENCED_RESOURCE_ID = "referencedResourceId";

  protected static final String FAILED_TO_CREATE_REF_RESOURCE =
      "Failed to create landing zone {} referenced resource. landingZoneId={}. Error: {}";

  protected static final String RESOURCE_CREATED =
      "{} resource id='{}' in resource group '{}' successfully created.";
  private static final String REFERENCED_RESOURCE_NOT_FOUND = "{} doesn't exist.";
  private static final String FAILED_ATTEMPT_TO_REMOVE_LZ_TAGS =
      "Failed attempt to remove the landing zone tags for resource: {}. Id={}";

  protected final ArmManagers armManagers;

  protected BaseReferencedResourceStep(ArmManagers armManagers) {
    this.armManagers = armManagers;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {

    var landingZoneId = getLandingZoneId(context);

    var azureLandingZoneRequest =
        getParameterOrThrow(
            context.getInputParameters(),
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            LandingZoneRequest.class);

    try {
      var stepDuration =
          MetricUtils.configureTimerForLzStepDuration(
              azureLandingZoneRequest.definition(), getArmResourceType().toString());
      var start = Instant.now().toEpochMilli();

      tagReferencedResourceAndSetContext(context);

      var finish = Instant.now().toEpochMilli();
      stepDuration.record(Duration.ofMillis(finish - start));
    } catch (ManagementException e) {
      return handleManagementException(
          e, landingZoneId.toString(), azureLandingZoneRequest.definition());
    } catch (RuntimeException e) {
      MetricUtils.incrementLandingZoneCreationFailure(azureLandingZoneRequest.definition());
      throw maybeThrowAzureInterruptedException(e);
    }
    return StepResult.getStepResultSuccess();
  }

  private UUID getLandingZoneId(FlightContext context) {
    return getParameterOrThrow(
        context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
  }

  private void tagReferencedResourceAndSetContext(FlightContext context) {
    GenericResource resource =
        findReferencedResourceByArmResourceType(context)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        String.format(
                            "A resource of type:%s could not be found in resource group: %s",
                            getArmResourceType(), getMRGName(context))));

    setLandingZoneResourceTags(context, resource);

    context.getWorkingMap().put(REFERENCED_RESOURCE_ID, resource.id());
  }

  private void setLandingZoneResourceTags(FlightContext context, GenericResource genericResource) {

    UUID lzId = getLandingZoneId(context);
    String lzIdTagValue = genericResource.tags().get(LandingZoneTagKeys.LANDING_ZONE_ID.toString());
    if (Strings.isNotBlank(lzIdTagValue) && !lzIdTagValue.equalsIgnoreCase(lzId.toString())) {
      throw new RuntimeException("Resource already tagged with a Landing Zone ID");
    }

    armManagers
        .azureResourceManager()
        .tagOperations()
        .updateTags(genericResource, getTagsToApply(context, genericResource));
  }

  private Map<String, String> getTagsToApply(
      FlightContext context, GenericResource genericResource) {

    // merge the tags from the resource with the tags that need to be applied.
    Map<String, String> tagsToApply = new HashedMap<>(getLzResourceTags(context, genericResource));

    tagsToApply.putAll(genericResource.tags());

    return tagsToApply;
  }

  private Map<String, String> getLzResourceTags(
      FlightContext context, GenericResource genericResource) {

    Map<String, String> tagsToApply =
        new HashedMap<>(getLandingZoneResourceTags(context, genericResource.id()));

    tagsToApply.put(
        LandingZoneTagKeys.LANDING_ZONE_ID.toString(), getLandingZoneId(context).toString());

    if (isSharedResource()) {
      tagsToApply.put(
          LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
          ResourcePurpose.SHARED_RESOURCE.toString());
    }
    return tagsToApply;
  }

  protected abstract boolean isSharedResource();

  protected abstract Map<String, String> getLandingZoneResourceTags(
      FlightContext context, String resourceId);

  private Optional<GenericResource> findReferencedResourceByArmResourceType(
      FlightContext flightContext) {

    for (GenericResource resource :
        armManagers
            .azureResourceManager()
            .genericResources()
            .listByResourceGroup(getMRGName(flightContext))) {
      if (resource.type().equalsIgnoreCase(getArmResourceType().toString())) {
        return Optional.of(resource);
      }
    }
    return Optional.empty();
  }

  protected Optional<StepResult> maybeHandleManagementException(ManagementException e) {
    return Optional.empty();
  }

  @Override
  public StepResult undoStep(FlightContext flightContext) throws InterruptedException {
    var resourceId = getResourceId(flightContext);
    try {
      if (resourceId.isPresent()) {

        removeLandingZoneTags(flightContext, resourceId.get());

        return StepResult.getStepResultSuccess();
      }

      // if the resource id is not set, that means that the referenced resource does not exist.
      // There is no need to perform any rollback operation. Hence, we warn and return success.
      logger.warn(REFERENCED_RESOURCE_NOT_FOUND, getArmResourceType());
      return StepResult.getStepResultSuccess();
    } catch (RuntimeException e) {
      logger.error(
          FAILED_ATTEMPT_TO_REMOVE_LZ_TAGS, getArmResourceType(), resourceId.orElse("n/a"));
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
  }

  private void removeLandingZoneTags(FlightContext flightContext, String resourceId) {
    // get tags for the resource.
    var genericResource = armManagers.azureResourceManager().genericResources().getById(resourceId);

    Map<String, String> tags = new HashMap<>(genericResource.tags());

    // only attempt to remove the landing zone tags if they are assigned to the current LZ
    if (containsCurrentLzId(tags, flightContext)) {
      // remove LZ id tag.
      tags.remove(LandingZoneTagKeys.LANDING_ZONE_ID.toString());

      // remove purpose tag.
      if (isSharedResource()) {
        tags.remove(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString());
      }

      // remove the tags that were added by reference resource, if present.
      for (String key : getLandingZoneResourceTags(flightContext, resourceId).keySet()) {
        tags.remove(key);
      }

      armManagers.azureResourceManager().tagOperations().updateTags(genericResource, tags);
    }
  }

  private boolean containsCurrentLzId(Map<String, String> tags, FlightContext context) {
    var landingZoneId = getLandingZoneId(context);

    return tags.containsKey(LandingZoneTagKeys.LANDING_ZONE_ID.toString())
        && tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()).equals(landingZoneId.toString());
  }

  protected abstract ArmResourceType getArmResourceType();

  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(REFERENCED_RESOURCE_ID, String.class));
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

  private StepResult handleManagementException(
      ManagementException e, String landingZoneId, String landingZoneType) {
    var handled = maybeHandleManagementException(e);
    if (handled.isPresent()) {
      var stepResult = handled.get();
      if (!stepResult.isSuccess()
          && stepResult.getStepStatus().equals(StepStatus.STEP_RESULT_FAILURE_FATAL)) {
        MetricUtils.incrementLandingZoneCreationFailure(landingZoneType);
      }
      return handled.get();
    }

    logger.error(
        FAILED_TO_CREATE_REF_RESOURCE,
        getArmResourceType(),
        landingZoneId,
        ManagementExceptionUtils.buildErrorInfo(e));
    MetricUtils.incrementLandingZoneCreationFailure(landingZoneType);
    return new StepResult(
        StepStatus.STEP_RESULT_FAILURE_FATAL, new FlightExceptionTranslator(e).translate());
  }
}
