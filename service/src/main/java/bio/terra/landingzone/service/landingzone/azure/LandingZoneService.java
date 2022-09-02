package bio.terra.landingzone.service.landingzone.azure;

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
import bio.terra.landingzone.library.landingzones.deployment.LandingZonePurpose;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.model.AzureCloudContext;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDefinitionNotFound;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDeleteNotImplemented;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResourcesByPurpose;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.create.CreateLandingZoneFlight;
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

  public LandingZoneService(
      LandingZoneJobService azureLandingZoneJobService,
      LandingZoneManagerProvider landingZoneManagerProvider,
      LandingZoneDao landingZoneDao) {
    this.azureLandingZoneJobService = azureLandingZoneJobService;
    this.landingZoneManagerProvider = landingZoneManagerProvider;
    this.landingZoneDao = landingZoneDao;
  }

  public AsyncJobResult<DeployedLandingZone> getAsyncJobResult(String jobId) {
    return azureLandingZoneJobService.retrieveAsyncJobResult(jobId, DeployedLandingZone.class);
  }

  public String startLandingZoneCreationJob(
      String jobId, LandingZoneRequest azureLandingZoneRequest, String resultPath) {

    checkIfRequestedFactoryExists(azureLandingZoneRequest);

    String jobDescription = "Creating Azure Landing Zone. Definition=%s, Version=%s";
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
            .addParameter(
                LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, azureLandingZoneRequest)
            .addParameter(JobMapKeys.RESULT_PATH.getKeyName(), resultPath);
    return jobBuilder.submit();
  }

  @Cacheable("landingZoneDefinitions")
  public List<LandingZoneDefinition> listLandingZoneDefinitions() {
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

  public List<LandingZoneResource> listResourcesByPurpose(
      ResourcePurpose purpose, AzureCloudContext azureCloudContext) {

    LandingZoneManager landingZoneManager =
        landingZoneManagerProvider.createLandingZoneManager(azureCloudContext);

    List<DeployedResource> deployedResources =
        landingZoneManager.reader().listResourcesByPurpose(purpose);

    return deployedResources.stream()
        .map(
            dp ->
                LandingZoneResource.builder()
                    .resourceId(dp.resourceId())
                    .resourceType(dp.resourceType())
                    .tags(dp.tags())
                    .region(dp.region())
                    .build())
        .collect(Collectors.toList());
  }

  public void deleteLandingZone(String landingZoneId) {
    throw new LandingZoneDeleteNotImplemented("Delete operation is not implemented");
  }

  public LandingZoneResourcesByPurpose listResourcesWithPurposes(String landingZoneId) {
    LandingZone landingZoneRecord = landingZoneDao.getLandingZone(UUID.fromString(landingZoneId));

    AzureCloudContext azureCloudContext = new AzureCloudContext();
    azureCloudContext.setAzureResourceGroupId(landingZoneRecord.getResourceGroupId());
    azureCloudContext.setAzureSubscriptionId(landingZoneRecord.getSubscriptionId());
    azureCloudContext.setAzureTenantId(landingZoneRecord.getTenantId());
    LandingZoneManager landingZoneManager =
        landingZoneManagerProvider.createLandingZoneManager(azureCloudContext);

    var listGeneralResources = listGeneralResourcesWithPurposes(landingZoneManager);
    var listSubnetResources = listSubnetResourcesWithPurposes(landingZoneManager);
    // Merge lists, no key collision is expected since the purpose sets are different.
    listGeneralResources.putAll(listSubnetResources);

    return new LandingZoneResourcesByPurpose(listGeneralResources);
  }

  private Map<LandingZonePurpose, List<LandingZoneResource>> listGeneralResourcesWithPurposes(
      LandingZoneManager landingZoneManager) {
    var deployedResources = landingZoneManager.reader().listResources();

    return deployedResources.stream()
        .map(
            dp ->
                LandingZoneResource.builder()
                    .resourceId(dp.resourceId())
                    .resourceType(dp.resourceType())
                    .tags(dp.tags())
                    .region(dp.region())
                    .build())
        .collect(
            Collectors.groupingBy(
                r ->
                    ResourcePurpose.fromString(
                        r.tags().get(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString()))));
  }

  private Map<LandingZonePurpose, List<LandingZoneResource>> listSubnetResourcesWithPurposes(
      LandingZoneManager landingZoneManager) {
    Map<LandingZonePurpose, List<LandingZoneResource>> subnetPurposeMap = new HashMap<>();
    SubnetResourcePurpose.values()
        .forEach(
            p ->
                subnetPurposeMap.put(
                    p,
                    landingZoneManager.reader().listSubnetsWithSubnetPurpose(p).stream()
                        .map(
                            s ->
                                LandingZoneResource.builder()
                                    .resourceId(s.id())
                                    .resourceType(s.getClass().getSimpleName())
                                    .resourceName(s.name())
                                    .resourceParentId(s.vNetId())
                                    .region(s.vNetRegion())
                                    .build())
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
