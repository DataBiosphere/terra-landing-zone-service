package bio.terra.landingzone.resource;

import bio.terra.landingzone.job.AzureLandingZoneJobBuilder;
import bio.terra.landingzone.job.AzureLandingZoneJobService;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.model.AuthenticatedUserRequest;
import bio.terra.landingzone.resource.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.resource.flight.create.CreateExternalResourceFlight;
import bio.terra.landingzone.resource.landingzone.ExternalLandingZoneResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExternalResourceService {
  private final AzureLandingZoneJobService azureLandingZoneJobService;

  @Autowired
  public ExternalResourceService(AzureLandingZoneJobService azureLandingZoneJobService) {
    this.azureLandingZoneJobService = azureLandingZoneJobService;
  }

  public String createAzureLandingZone(
      String jobId,
      ExternalLandingZoneResource externalLandingZoneResource,
      AuthenticatedUserRequest userRequest,
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      String resultPath) {
    String jobDescription = "Creating Azure Landing Zone. Definition=%s, Version=%s";
    final AzureLandingZoneJobBuilder jobBuilder =
        azureLandingZoneJobService
            .newJob()
            .jobId(jobId)
            .description(
                String.format(
                    jobDescription,
                    externalLandingZoneResource.getDefinition(),
                    externalLandingZoneResource.getVersion()))
            .flightClass(CreateExternalResourceFlight.class)
            .resource(externalLandingZoneResource)
            .operationType(OperationType.CREATE)
            .userRequest(userRequest)
            .resourceType(externalLandingZoneResource.getResourceType())
            .stewardshipType(externalLandingZoneResource.getStewardshipType())
            .addParameter(
                LandingZoneFlightMapKeys.LANDING_ZONE_AZURE_CONFIGURATION,
                landingZoneAzureConfiguration)
            .addParameter(
                LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, externalLandingZoneResource)
            .addParameter(JobMapKeys.RESULT_PATH.getKeyName(), resultPath);
    return jobBuilder.submit();
  }
}
