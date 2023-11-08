package bio.terra.lz.futureservice.app.service;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.job.model.JobReport;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.StartLandingZoneCreation;
import bio.terra.lz.futureservice.app.service.exception.LandingZoneInvalidInputException;
import bio.terra.lz.futureservice.common.utils.MapperUtils;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDeployedResource;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDetails;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiCreateAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiCreateLandingZoneResult;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneAppService {
  private static final Logger logger = LoggerFactory.getLogger(LandingZoneAppService.class);

  // this service is from library module
  private final LandingZoneService landingZoneService;

  public LandingZoneAppService(LandingZoneService landingZoneService) {
    this.landingZoneService = landingZoneService;
  }

  public ApiCreateLandingZoneResult createAzureLandingZone(
      BearerToken bearerToken,
      ApiCreateAzureLandingZoneRequestBody body,
      String asyncResultEndpoint) {
    // TODO: this feature is always on since we inside LZ service?
    // features.azureEnabledCheck();
    logger.info(
        "Requesting new Azure landing zone with definition='{}', version='{}'",
        body.getDefinition(),
        body.getVersion());

    // Prevent deploying more than 1 landing zone per billing profile
    verifyLandingZoneDoesNotExistForBillingProfile(bearerToken, body);

    LandingZoneRequest landingZoneRequest =
        LandingZoneRequest.builder()
            .landingZoneId(body.getLandingZoneId())
            .definition(body.getDefinition())
            .version(body.getVersion())
            .parameters(
                MapperUtils.LandingZoneMapper.landingZoneParametersFrom(body.getParameters()))
            .billingProfileId(body.getBillingProfileId())
            .build();
    return toApiCreateLandingZoneResult(
        landingZoneService.startLandingZoneCreationJob(
            bearerToken, body.getJobControl().getId(), landingZoneRequest, asyncResultEndpoint));
  }

  public ApiAzureLandingZoneResult getCreateAzureLandingZoneResult(
      BearerToken bearerToken, String jobId) {
    // features.azureEnabledCheck();
    return toApiAzureLandingZoneResult(landingZoneService.getAsyncJobResult(bearerToken, jobId));
  }

  private void verifyLandingZoneDoesNotExistForBillingProfile(
      BearerToken bearerToken, ApiCreateAzureLandingZoneRequestBody body) {
    // TODO: Catching the exception is a temp solution.
    // A better approach would be to return an empty list instead of throwing an exception
    try {
      landingZoneService
          .getLandingZonesByBillingProfile(bearerToken, body.getBillingProfileId())
          .stream()
          .findFirst()
          .ifPresent(
              t -> {
                throw new LandingZoneInvalidInputException(
                    "A Landing Zone already exists in the requested billing profile");
              });
    } catch (bio.terra.landingzone.db.exception.LandingZoneNotFoundException ex) {
      logger.info("The billing profile does not have a landing zone. ", ex);
    }
  }

  private ApiCreateLandingZoneResult toApiCreateLandingZoneResult(
      LandingZoneJobService.AsyncJobResult<StartLandingZoneCreation> jobResult) {

    return new ApiCreateLandingZoneResult()
        .jobReport(MapperUtils.JobReportMapper.from(jobResult.getJobReport()))
        .errorReport(MapperUtils.ErrorReportMapper.from(jobResult.getApiErrorReport()))
        .landingZoneId(jobResult.getResult().landingZoneId())
        .definition(jobResult.getResult().definition())
        .version(jobResult.getResult().version());
  }

  private ApiAzureLandingZoneResult toApiAzureLandingZoneResult(
      LandingZoneJobService.AsyncJobResult<DeployedLandingZone> jobResult) {
    ApiAzureLandingZoneDetails azureLandingZone = null;
    if (jobResult.getJobReport().getStatus().equals(JobReport.StatusEnum.SUCCEEDED)) {
      azureLandingZone =
          Optional.ofNullable(jobResult.getResult())
              .map(
                  lz ->
                      new ApiAzureLandingZoneDetails()
                          .id(lz.id())
                          .resources(
                              lz.deployedResources().stream()
                                  .map(
                                      resource ->
                                          new ApiAzureLandingZoneDeployedResource()
                                              .region(resource.region())
                                              .resourceType(resource.resourceType())
                                              .resourceId(resource.resourceId()))
                                  .collect(Collectors.toList())))
              .orElse(null);
    }
    return new ApiAzureLandingZoneResult()
        .jobReport(MapperUtils.JobReportMapper.from(jobResult.getJobReport()))
        .errorReport(MapperUtils.ErrorReportMapper.from(jobResult.getApiErrorReport()))
        .landingZone(azureLandingZone);
  }
}
