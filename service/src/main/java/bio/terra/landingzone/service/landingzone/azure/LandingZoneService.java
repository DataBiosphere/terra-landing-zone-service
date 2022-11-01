package bio.terra.landingzone.service.landingzone.azure;

import static bio.terra.landingzone.service.iam.LandingZoneSamService.IS_AUTHORIZED;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.model.LandingZone;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.job.LandingZoneJobBuilder;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.job.LandingZoneJobService.AsyncJobResult;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.DeployedSubnet;
import bio.terra.landingzone.library.landingzones.deployment.LandingZonePurpose;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.service.bpm.LandingZoneBillingProfileManagerService;
import bio.terra.landingzone.service.iam.LandingZoneSamService;
import bio.terra.landingzone.service.iam.SamConstants;
import bio.terra.landingzone.service.iam.SamRethrow;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDefinitionNotFound;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDeleteNotImplemented;
import bio.terra.landingzone.service.landingzone.azure.model.DeletedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResourcesByPurpose;
import bio.terra.landingzone.service.landingzone.azure.model.StartLandingZoneCreation;
import bio.terra.landingzone.service.landingzone.azure.model.StartLandingZoneDeletion;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.create.CreateLandingZoneFlight;
import bio.terra.landingzone.stairway.flight.delete.DeleteLandingZoneFlight;
import com.azure.core.util.ExpandableStringEnum;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneService {
  private static final Logger logger = LoggerFactory.getLogger(LandingZoneService.class);
  private final LandingZoneJobService azureLandingZoneJobService;
  private final LandingZoneManagerProvider landingZoneManagerProvider;
  private final LandingZoneDao landingZoneDao;
  private final LandingZoneSamService samService;
  private final LandingZoneBillingProfileManagerService bpmService;

  public LandingZoneService(
      LandingZoneJobService azureLandingZoneJobService,
      LandingZoneManagerProvider landingZoneManagerProvider,
      LandingZoneDao landingZoneDao,
      LandingZoneSamService samService,
      LandingZoneBillingProfileManagerService bpmService) {
    this.azureLandingZoneJobService = azureLandingZoneJobService;
    this.landingZoneManagerProvider = landingZoneManagerProvider;
    this.landingZoneDao = landingZoneDao;
    this.samService = samService;
    this.bpmService = bpmService;
  }

  /**
   * Retrieves the result of an asynchronous landing zone creation job.
   *
   * @param bearerToken bearer token for the user request.
   * @param jobId job identifier.
   * @return result of asynchronous job.
   */
  public AsyncJobResult<DeployedLandingZone> getAsyncJobResult(
      BearerToken bearerToken, String jobId) {
    // Check calling user has access to the landing zone referenced by this job
    azureLandingZoneJobService.verifyUserAccess(bearerToken, jobId);
    return azureLandingZoneJobService.retrieveAsyncJobResult(jobId, DeployedLandingZone.class);
  }

  /**
   * Retrieves the result of an asynchronous landing zone deleting job.
   *
   * @param bearerToken bearer token for the user request.
   * @param landingZoneId landing zone id associated with the job.
   * @param jobId job identifier.
   * @return result of asynchronous job.
   */
  public AsyncJobResult<DeletedLandingZone> getAsyncDeletionJobResult(
      BearerToken bearerToken, UUID landingZoneId, String jobId) {
    // Check calling user has access to the landing zone referenced by this job
    azureLandingZoneJobService.verifyUserAccessForDeleteJobResult(
        bearerToken, landingZoneId, jobId);
    return azureLandingZoneJobService.retrieveAsyncJobResult(jobId, DeletedLandingZone.class);
  }

  /**
   * Starts the process to create a landing zone.
   *
   * @param bearerToken bearer token of the user request.
   * @param jobId job identifier.
   * @param azureLandingZoneRequest landing zone creation request object.
   * @param resultPath API path for checking job result.
   * @return job report
   */
  public AsyncJobResult<StartLandingZoneCreation> startLandingZoneCreationJob(
      BearerToken bearerToken,
      String jobId,
      LandingZoneRequest azureLandingZoneRequest,
      String resultPath) {
    // Check that the calling user has "link" permission on the billing profile resource in Sam
    SamRethrow.onInterrupted(
        () ->
            samService.checkAuthz(
                bearerToken,
                SamConstants.SamResourceType.SPEND_PROFILE,
                azureLandingZoneRequest.billingProfileId().toString(),
                SamConstants.SamSpendProfileAction.LINK),
        IS_AUTHORIZED);

    checkIfRequestedFactoryExists(azureLandingZoneRequest);
    String jobDescription = "Creating Azure Landing Zone. Definition=%s, Version=%s";
    UUID landingZoneId = UUID.randomUUID();
    final LandingZoneJobBuilder jobBuilder =
        azureLandingZoneJobService
            .newJob()
            .jobId(jobId)
            .description(
                String.format(
                    jobDescription,
                    azureLandingZoneRequest.definition(),
                    azureLandingZoneRequest.version()))
            .flightClass(CreateLandingZoneFlight.class)
            .landingZoneRequest(azureLandingZoneRequest)
            .operationType(OperationType.CREATE)
            .bearerToken(bearerToken)
            .addParameter(
                LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, azureLandingZoneRequest)
            .addParameter(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneId)
            .addParameter(JobMapKeys.RESULT_PATH.getKeyName(), resultPath);
    return azureLandingZoneJobService.retrieveStartingAsyncJobResult(
        jobBuilder.submit(),
        new StartLandingZoneCreation(
            landingZoneId,
            azureLandingZoneRequest.definition(),
            azureLandingZoneRequest.version()));
  }

  /**
   * Starts a landing zone deletion job.
   *
   * @param bearerToken bearer token of the user request.
   * @param jobId job identifier.
   * @param landingZoneId landing zone creation request object.
   * @param resultPath API path for checking job result.
   * @return job report
   */
  public AsyncJobResult<StartLandingZoneDeletion> startLandingZoneDeletionJob(
      BearerToken bearerToken, String jobId, UUID landingZoneId, String resultPath) {

    SamRethrow.onInterrupted(
        () ->
            samService.checkAuthz(
                bearerToken,
                SamConstants.SamResourceType.LANDING_ZONE,
                landingZoneId.toString(),
                SamConstants.SamLandingZoneAction.DELETE),
        IS_AUTHORIZED);

    String jobDescription = "Deleting Azure Landing Zone. Landing Zone ID:%s";
    final LandingZoneJobBuilder jobBuilder =
        azureLandingZoneJobService
            .newJob()
            .jobId(jobId)
            .description(String.format(jobDescription, landingZoneId.toString()))
            .flightClass(DeleteLandingZoneFlight.class)
            .operationType(OperationType.DELETE)
            .bearerToken(bearerToken)
            .addParameter(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneId)
            .addParameter(JobMapKeys.RESULT_PATH.getKeyName(), resultPath);
    return azureLandingZoneJobService.retrieveStartingAsyncJobResult(
        jobBuilder.submit(), new StartLandingZoneDeletion(landingZoneId));
  }

  /**
   * Lists available landing zone definitions.
   *
   * @param bearerToken bearer token of the user request.
   * @return list of landing zone definitions.
   */
  @Cacheable("landingZoneDefinitions")
  public List<LandingZoneDefinition> listLandingZoneDefinitions(BearerToken bearerToken) {
    // Check that the calling user is enabled in Sam, but no further authz checks.
    SamRethrow.onInterrupted(() -> samService.checkUserEnabled(bearerToken), "checkUserEnabled");

    return LandingZoneManager.listDefinitionFactories().stream()
        .flatMap(
            d ->
                d.versions().stream()
                    .map(
                        v ->
                            LandingZoneDefinition.builder()
                                .definition(d.className())
                                .name(d.name())
                                .description(d.description())
                                .version(v.toString())
                                .build()))
        .collect(Collectors.toList());
  }

  /**
   * Lists all landing zone resources with a provided ResourcePurpose.
   *
   * @param bearerToken bearer token of the calling user.
   * @param landingZoneId landing zone ID to query.
   * @param purpose landing zone purpose to query.
   * @return list of resources with the purpose specified.
   */
  public List<LandingZoneResource> listResourcesByPurpose(
      BearerToken bearerToken, UUID landingZoneId, LandingZonePurpose purpose) {
    List<LandingZoneResource> deployedResources = null;

    checkIfUserHasPermissionForLandingZoneResource(
        bearerToken, landingZoneId, SamConstants.SamLandingZoneAction.LIST_RESOURCES);

    LandingZoneTarget landingZoneTarget = buildLandingZoneTarget(landingZoneId);

    LandingZoneManager landingZoneManager =
        landingZoneManagerProvider.createLandingZoneManager(landingZoneTarget);

    if (purpose.getClass().equals(ResourcePurpose.class)) {
      deployedResources =
          listResourcesByPurpose(landingZoneManager, landingZoneId, (ResourcePurpose) purpose);
    }

    if (purpose.getClass().equals(SubnetResourcePurpose.class)) {
      deployedResources =
          listResourcesByPurpose(
              landingZoneManager, landingZoneId, (SubnetResourcePurpose) purpose);
    }

    return deployedResources;
  }

  /**
   * Lists landing zones for a given billing profile ID that the calling user has access to.
   *
   * @param billingProfileId the billing profile ID to search.
   * @return list of landing zone IDs.
   */
  public List<UUID> listLandingZoneIds(BearerToken bearerToken, UUID billingProfileId) {
    // Call BPM to get the landing zone target.
    var profile = bpmService.getBillingProfile(bearerToken, billingProfileId);

    // Query the database for landing zone ids with the given target.
    var landingZoneIds = listLandingZoneIdsByTarget(LandingZoneTarget.fromBillingProfile(profile));

    // Filter the list based on what the user has access to.
    // Note: this makes a Sam query per landing zone, but we expect there to be at most
    // 1 landing zone per billing profile in most/all cases.
    return landingZoneIds.stream()
        .filter(
            lz ->
                SamRethrow.onInterrupted(
                    () ->
                        samService.isAuthorized(
                            bearerToken,
                            SamConstants.SamResourceType.LANDING_ZONE,
                            lz.toString(),
                            SamConstants.SamLandingZoneAction.LIST_RESOURCES),
                    IS_AUTHORIZED))
        .toList();
  }

  /**
   * Lists landing zones for a given LandingZoneTarget.
   *
   * <p>Note: this method does not perform authorization checks. It is public, but only called as
   * part of other authorized requests, never from a direct API request.
   *
   * @param landingZoneTarget the landing zone target to search.
   * @return list of landing zone IDs.
   */
  public List<UUID> listLandingZoneIdsByTarget(LandingZoneTarget landingZoneTarget) {
    return landingZoneDao
        .getLandingZoneList(
            landingZoneTarget.azureSubscriptionId(),
            landingZoneTarget.azureTenantId(),
            landingZoneTarget.azureResourceGroupId())
        .stream()
        .map(dlz -> dlz.landingZoneId())
        .toList();
  }

  /**
   * Deletes a landing zone.
   *
   * @param bearerToken bearer token of the calling user.
   * @param landingZoneId id of the landing zone
   */
  public void deleteLandingZone(BearerToken bearerToken, UUID landingZoneId) {
    throw new LandingZoneDeleteNotImplemented("Delete operation is not implemented");
  }

  /**
   * List all resources in a landing zone.
   *
   * @param bearerToken bearer token of the calling user.
   * @param landingZoneId the landing zone ID.
   * @return list of resources grouped by purpose.
   */
  public LandingZoneResourcesByPurpose listResourcesWithPurposes(
      BearerToken bearerToken, UUID landingZoneId) {
    checkIfUserHasPermissionForLandingZoneResource(
        bearerToken, landingZoneId, SamConstants.SamLandingZoneAction.LIST_RESOURCES);

    LandingZoneTarget landingZoneTarget = buildLandingZoneTarget(landingZoneId);

    LandingZoneManager landingZoneManager =
        landingZoneManagerProvider.createLandingZoneManager(landingZoneTarget);

    var listGeneralResources =
        listGeneralResourcesWithPurposes(landingZoneId.toString(), landingZoneManager);
    var listSubnetResources =
        listSubnetResourcesWithPurposes(landingZoneId.toString(), landingZoneManager);
    // Merge lists, no key collision is expected since the purpose sets are different.
    listGeneralResources.putAll(listSubnetResources);

    return new LandingZoneResourcesByPurpose(listGeneralResources);
  }

  private LandingZoneTarget buildLandingZoneTarget(UUID landingZoneId) {
    // Look up the landing zone record from the database
    LandingZone landingZoneRecord = landingZoneDao.getLandingZone(landingZoneId);

    return new LandingZoneTarget(
        landingZoneRecord.tenantId(),
        landingZoneRecord.subscriptionId(),
        landingZoneRecord.resourceGroupId());
  }

  private void checkIfUserHasPermissionForLandingZoneResource(
      BearerToken bearerToken, UUID landingZoneId, String permissionName) {
    SamRethrow.onInterrupted(
        () ->
            samService.checkAuthz(
                bearerToken,
                SamConstants.SamResourceType.LANDING_ZONE,
                landingZoneId.toString(),
                permissionName),
        IS_AUTHORIZED);
  }

  private List<LandingZoneResource> listResourcesByPurpose(
      LandingZoneManager landingZoneManager, UUID landingZoneId, ResourcePurpose purpose) {

    List<DeployedResource> deployedResources =
        landingZoneManager.reader().listResourcesByPurpose(landingZoneId.toString(), purpose);

    return deployedResources.stream().map(this::toLandingZoneResource).toList();
  }

  private List<LandingZoneResource> listResourcesByPurpose(
      LandingZoneManager landingZoneManager, UUID landingZoneId, SubnetResourcePurpose purpose) {
    List<DeployedSubnet> deployedSubnets =
        landingZoneManager.reader().listSubnetsBySubnetPurpose(landingZoneId.toString(), purpose);

    return deployedSubnets.stream().map(s -> toLandingZoneResource(s)).toList();
  }

  private LandingZoneResource toLandingZoneResource(DeployedSubnet subnet) {
    return LandingZoneResource.builder()
        .resourceId(subnet.id())
        .resourceType(subnet.getClass().getSimpleName())
        .resourceName(subnet.name())
        .resourceParentId(subnet.vNetId())
        .region(subnet.vNetRegion())
        .build();
  }

  private LandingZoneResource toLandingZoneResource(DeployedResource resource) {
    return LandingZoneResource.builder()
        .resourceId(resource.resourceId())
        .resourceType(resource.resourceType())
        .tags(resource.tags())
        .region(resource.region())
        .build();
  }

  private Map<LandingZonePurpose, List<LandingZoneResource>> listGeneralResourcesWithPurposes(
      String landingZoneId, LandingZoneManager landingZoneManager) {
    var deployedResources = landingZoneManager.reader().listResourcesWithPurpose(landingZoneId);

    return deployedResources.stream()
        .map(dp -> toLandingZoneResource(dp))
        .collect(
            Collectors.groupingBy(
                r ->
                    ResourcePurpose.fromString(
                        r.tags().get(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString()))));
  }

  private Map<LandingZonePurpose, List<LandingZoneResource>> listSubnetResourcesWithPurposes(
      String landingZoneId, LandingZoneManager landingZoneManager) {
    Map<LandingZonePurpose, List<LandingZoneResource>> subnetPurposeMap = new HashMap<>();
    SubnetResourcePurpose.values()
        .forEach(
            p ->
                subnetPurposeMap.put(
                    p,
                    landingZoneManager
                        .reader()
                        .listSubnetsBySubnetPurpose(landingZoneId, p)
                        .stream()
                        .map(s -> toLandingZoneResource(s))
                        .toList()));
    // Remove empty mappings
    subnetPurposeMap.entrySet().removeIf(entry -> entry.getValue().size() == 0);
    return subnetPurposeMap;
  }

  private void checkIfRequestedFactoryExists(LandingZoneRequest azureLandingZone) {
    Predicate<FactoryDefinitionInfo> requiredDefinition =
        (FactoryDefinitionInfo f) ->
            f.className().equals(azureLandingZone.definition())
                && f.versions().stream()
                    .map(ExpandableStringEnum::toString)
                    .toList()
                    .contains(azureLandingZone.version());
    var requestedFactory =
        LandingZoneManager.listDefinitionFactories().stream()
            .filter(requiredDefinition)
            .findFirst();

    if (requestedFactory.isEmpty()) {
      logger.warn(
          "Azure landing zone definition with name={} and version={} doesn't exist",
          azureLandingZone.definition(),
          azureLandingZone.version());
      throw new LandingZoneDefinitionNotFound("Requested landing zone definition doesn't exist");
    }
  }
}
