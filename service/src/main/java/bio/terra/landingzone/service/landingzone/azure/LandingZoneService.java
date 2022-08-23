package bio.terra.landingzone.service.landingzone.azure;

import bio.terra.landingzone.job.AzureLandingZoneJobBuilder;
import bio.terra.landingzone.job.AzureLandingZoneJobService;
import bio.terra.landingzone.job.AzureLandingZoneJobService.AsyncJobResult;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
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
import java.util.ArrayList;
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
  private final LandingZoneAzureConfiguration landingZoneAzureConfiguration;

  public LandingZoneService(
      AzureLandingZoneJobService azureLandingZoneJobService,
      LandingZoneAzureConfiguration landingZoneAzureConfiguration) {
    this.azureLandingZoneJobService = azureLandingZoneJobService;
    this.landingZoneAzureConfiguration = landingZoneAzureConfiguration;
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
                LandingZoneFlightMapKeys.LANDING_ZONE_AZURE_CONFIGURATION,
                landingZoneAzureConfiguration)
            .addParameter(
                LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, azureLandingZoneRequest)
            .addParameter(JobMapKeys.RESULT_PATH.getKeyName(), resultPath);
    return jobBuilder.submit();
  }

  @Cacheable("landingZoneDefinitions")
  public List<LandingZoneDefinition> listLandingZoneDefinitions() {
    List<LandingZoneDefinition> landingZoneTemplates = new ArrayList<>();
    for (var factoryInfo : LandingZoneManager.listDefinitionFactories()) {
      factoryInfo
          .versions()
          .forEach(
              version ->
                  landingZoneTemplates.add(
                      LandingZoneDefinition.builder()
                          .definition(factoryInfo.className())
                          .name(factoryInfo.name())
                          .description(factoryInfo.description())
                          .version(version.toString())
                          .build()));
    }
    return landingZoneTemplates;
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
