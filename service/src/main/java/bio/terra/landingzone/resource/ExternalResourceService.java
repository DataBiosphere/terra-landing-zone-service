package bio.terra.landingzone.resource;

import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.job.LandingZoneJobBuilder;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.model.AuthenticatedUserRequest;
import bio.terra.landingzone.resource.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.resource.flight.create.CreateLandingZoneFlight;
import bio.terra.landingzone.resource.landingzone.JobLandingZoneDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExternalResourceService {
  private final LandingZoneJobService landingZoneJobService;

  @Autowired
  public ExternalResourceService(LandingZoneJobService landingZoneJobService) {
    this.landingZoneJobService = landingZoneJobService;
  }

  public String createAzureLandingZone(
      String jobId,
      JobLandingZoneDefinition jobLandingZoneDefinition,
      AuthenticatedUserRequest userRequest,
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      String resultPath) {
    String jobDescription = "Creating Azure Landing Zone. Definition=%s, Version=%s";
    final LandingZoneJobBuilder jobBuilder =
        landingZoneJobService
            .newJob()
            .jobId(jobId)
            .description(
                String.format(
                    jobDescription,
                    jobLandingZoneDefinition.getDefinition(),
                    jobLandingZoneDefinition.getVersion()))
            .flightClass(CreateLandingZoneFlight.class)
            //.landingZoneRequest(jobLandingZoneDefinition)
            .operationType(OperationType.CREATE)
            .userRequest(userRequest)
            .resourceType(jobLandingZoneDefinition.getResourceType())
            .stewardshipType(jobLandingZoneDefinition.getStewardshipType())
            .addParameter(
                LandingZoneFlightMapKeys.LANDING_ZONE_AZURE_CONFIGURATION,
                landingZoneAzureConfiguration)
            .addParameter(
                LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, jobLandingZoneDefinition)
            .addParameter(JobMapKeys.RESULT_PATH.getKeyName(), resultPath);
    return jobBuilder.submit();
  }
}
