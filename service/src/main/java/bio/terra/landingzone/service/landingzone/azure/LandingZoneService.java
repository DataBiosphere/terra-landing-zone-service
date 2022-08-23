package bio.terra.landingzone.service.landingzone.azure;

import bio.terra.landingzone.job.AzureLandingZoneJobBuilder;
import bio.terra.landingzone.job.AzureLandingZoneJobService;
import bio.terra.landingzone.job.AzureLandingZoneJobService.AsyncJobResult;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDefinitionNotFound;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDeleteNotImplemented;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.create.CreateLandingZoneFlight;
import com.azure.core.util.ExpandableStringEnum;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneService {
  private static final Logger logger = LoggerFactory.getLogger(LandingZoneService.class);
public class LandingZoneService {
  private static final Logger logger = LoggerFactory.getLogger(LandingZoneService.class);
  private final AzureLandingZoneJobService azureLandingZoneJobService;

  public LandingZoneService(AzureLandingZoneJobService azureLandingZoneJobService) {
    this.azureLandingZoneJobService = azureLandingZoneJobService;
  }

  public AsyncJobResult<DeployedLandingZone> getJobResult(String jobId) {
    return azureLandingZoneJobService.retrieveAsyncJobResult(jobId, DeployedLandingZone.class);
  }

  public String startLandingZoneCreationJob(
      String jobId, AzureLandingZoneRequest azureLandingZoneRequest, String resultPath) {

    checkIfRequestedFactoryExists(azureLandingZoneRequest);

    String jobDescription = "Creating Azure Landing Zone. Definition=%s, Version=%s";
    final AzureLandingZoneJobBuilder jobBuilder =
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
            // .userRequest(userRequest)
            // .resourceType(jobLandingZoneDefinition.getResourceType())
            //.stewardshipType(jobLandingZoneDefinition.getStewardshipType())
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
      LandingZoneManager landingZoneManager, ResourcePurpose purpose) {

    var deployedResources = landingZoneManager.reader().listResourcesByPurpose(purpose);

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
