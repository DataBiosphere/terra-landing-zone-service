package bio.terra.lz.futureservice.common.fixture;

import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.job.model.JobReport;
import bio.terra.landingzone.service.landingzone.azure.model.StartLandingZoneCreation;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDetails;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiCreateAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiCreateLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiJobControl;
import bio.terra.lz.futureservice.generated.model.ApiJobReport;
import java.time.Instant;
import java.util.UUID;
import org.apache.http.HttpStatus;

public class AzureLandingZoneFixtures {
  private AzureLandingZoneFixtures() {}

  public static LandingZoneJobService.AsyncJobResult<StartLandingZoneCreation>
      createStartCreateJobResult(String jobId, JobReport.StatusEnum jobStatus, UUID landingZoneId) {
    return createStartCreateJobResultWithStartLandingZoneCreation(
        jobId, jobStatus, new StartLandingZoneCreation(landingZoneId, "testdefinition", "v1"));
  }

  public static LandingZoneJobService.AsyncJobResult<StartLandingZoneCreation>
      createStartCreateJobResultWithStartLandingZoneCreation(
          String jobId,
          JobReport.StatusEnum jobStatus,
          StartLandingZoneCreation startLandingZoneCreation) {
    LandingZoneJobService.AsyncJobResult<StartLandingZoneCreation> asyncJobResult =
        new LandingZoneJobService.AsyncJobResult<>();
    asyncJobResult.jobReport(
        new JobReport()
            .id(jobId)
            .description("description")
            .status(jobStatus)
            .statusCode(HttpStatus.SC_ACCEPTED)
            .submitted(Instant.now().toString())
            .resultURL("create-result/"));
    asyncJobResult.result(startLandingZoneCreation);
    return asyncJobResult;
  }

  public static ApiCreateAzureLandingZoneRequestBody buildCreateAzureLandingZoneRequest(
      String jobId, UUID billingProfileId) {
    return new ApiCreateAzureLandingZoneRequestBody()
        .billingProfileId(billingProfileId)
        .jobControl(new ApiJobControl().id(jobId))
        .definition("azureLandingZoneDefinition")
        .version("v1");
  }

  public static ApiCreateAzureLandingZoneRequestBody
      buildCreateAzureLandingZoneRequestWithoutDefinition(String jobId) {
    return new ApiCreateAzureLandingZoneRequestBody()
        .billingProfileId(UUID.randomUUID())
        .jobControl(new ApiJobControl().id(jobId))
        .version("v1");
  }

  public static ApiCreateAzureLandingZoneRequestBody
      buildCreateAzureLandingZoneRequestWithoutBillingProfile(String jobId) {
    return new ApiCreateAzureLandingZoneRequestBody()
        .jobControl(new ApiJobControl().id(jobId))
        .version("v1");
  }

  public static ApiCreateLandingZoneResult buildApiCreateLandingZoneSuccessResult(String jobId) {
    var jobReport = buildApiJobReport(jobId, ApiJobReport.StatusEnum.RUNNING);
    return new ApiCreateLandingZoneResult()
        .jobReport(jobReport)
        .landingZoneId(UUID.randomUUID())
        .definition("lzDefinition")
        .version("lzVersion");
  }

  private static ApiJobReport buildApiJobReport(String jobId, ApiJobReport.StatusEnum status) {
    return new ApiJobReport().description("LZ creation").status(status).id(jobId);
  }

  public static ApiAzureLandingZoneResult createApiAzureLandingZoneResult(
      String jobId, ApiJobReport.StatusEnum jobStatus) {
    return new ApiAzureLandingZoneResult().jobReport(buildApiJobReport(jobId, jobStatus));
  }

  public static ApiAzureLandingZoneResult createApiAzureLandingZoneResult(
      String jobId, UUID landingZoneId, ApiJobReport.StatusEnum jobStatus) {
    return new ApiAzureLandingZoneResult()
        .jobReport(buildApiJobReport(jobId, jobStatus))
        .landingZone(buildApiAzureLandingZoneDetails(landingZoneId));
  }

  private static ApiAzureLandingZoneDetails buildApiAzureLandingZoneDetails(UUID landingZoneId) {
    return new ApiAzureLandingZoneDetails().id(landingZoneId);
  }
}
