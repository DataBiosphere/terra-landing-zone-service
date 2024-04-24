package bio.terra.lz.futureservice.common.fixture;

import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZone;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDeployedResource;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDetails;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResourcesList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResourcesPurposeGroup;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiCreateAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiCreateLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneJobResult;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiErrorReport;
import bio.terra.lz.futureservice.generated.model.ApiJobControl;
import bio.terra.lz.futureservice.generated.model.ApiJobReport;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AzureLandingZoneFixtures {
  private AzureLandingZoneFixtures() {}

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

  public static ApiDeleteAzureLandingZoneResult buildApiDeleteAzureLandingZoneResult(
      String jobId, ApiJobReport.StatusEnum jobStatus, UUID landingZoneId) {
    var jobReport = buildApiJobReport(jobId, jobStatus);
    return new ApiDeleteAzureLandingZoneResult().landingZoneId(landingZoneId).jobReport(jobReport);
  }

  public static ApiDeleteAzureLandingZoneRequestBody buildDeleteAzureLandingZoneRequest(
      String jobId) {
    return new ApiDeleteAzureLandingZoneRequestBody().jobControl(new ApiJobControl().id(jobId));
  }

  public static ApiDeleteAzureLandingZoneJobResult buildApiDeleteAzureLandingZoneJobResult(
      String jobId, UUID landingZoneId, ApiJobReport.StatusEnum jobStatus) {
    var jobReport = buildApiJobReport(jobId, jobStatus);
    return switch (jobStatus) {
      case SUCCEEDED ->
          new ApiDeleteAzureLandingZoneJobResult()
              .jobReport(jobReport)
              .landingZoneId(landingZoneId)
              .resources(List.of("resource/id1", "resource/id2"));
      case RUNNING -> new ApiDeleteAzureLandingZoneJobResult().jobReport(jobReport);
      case FAILED ->
          new ApiDeleteAzureLandingZoneJobResult()
              .jobReport(jobReport)
              .errorReport(buildApiErrorReport(500));
    };
  }

  public static ApiAzureLandingZoneResourcesList buildListLandingZoneResourcesByPurposeResult(
      UUID landingZoneId) {
    var resourcePurposeGroups =
        List.of(
            new ApiAzureLandingZoneResourcesPurposeGroup()
                .purpose("sharedResources")
                .deployedResources(
                    List.of(
                        new ApiAzureLandingZoneDeployedResource()
                            .resourceName("subnet")
                            .resourceType("azure/subnet")
                            .resourceId("subnet1"))),
            new ApiAzureLandingZoneResourcesPurposeGroup()
                .purpose("lzResources")
                .deployedResources(
                    List.of(
                        new ApiAzureLandingZoneDeployedResource()
                            .resourceName("sentinel")
                            .resourceType("azure/solution")
                            .resourceId("sentinel1"))));
    return new ApiAzureLandingZoneResourcesList()
        .id(landingZoneId)
        .resources(resourcePurposeGroups);
  }

  public static ApiAzureLandingZoneResourcesList buildEmptyListLandingZoneResourcesByPurposeResult(
      UUID landingZoneId) {
    return new ApiAzureLandingZoneResourcesList().id(landingZoneId);
  }

  public static ApiAzureLandingZoneResult buildApiAzureLandingZoneResult(
      String jobId, ApiJobReport.StatusEnum jobStatus) {
    return new ApiAzureLandingZoneResult().jobReport(buildApiJobReport(jobId, jobStatus));
  }

  public static ApiAzureLandingZoneResult buildApiAzureLandingZoneResult(
      String jobId, UUID landingZoneId, ApiJobReport.StatusEnum jobStatus) {
    return new ApiAzureLandingZoneResult()
        .jobReport(buildApiJobReport(jobId, jobStatus))
        .landingZone(buildApiAzureLandingZoneDetails(landingZoneId));
  }

  public static ApiAzureLandingZone buildDefaultApiAzureLandingZone(
      UUID landingZoneId,
      UUID billingProfileId,
      String definition,
      String version,
      OffsetDateTime createDate,
      String region) {
    return new ApiAzureLandingZone()
        .landingZoneId(landingZoneId)
        .billingProfileId(billingProfileId)
        .definition(definition)
        .version(version)
        .region(region)
        .createdDate(createDate);
  }

  private static ApiJobReport buildApiJobReport(String jobId, ApiJobReport.StatusEnum status) {
    return new ApiJobReport().description("LZ creation").status(status).id(jobId);
  }

  private static ApiErrorReport buildApiErrorReport(int statusCode) {
    return new ApiErrorReport().statusCode(statusCode);
  }

  private static ApiAzureLandingZoneDetails buildApiAzureLandingZoneDetails(UUID landingZoneId) {
    return new ApiAzureLandingZoneDetails().id(landingZoneId);
  }
}
