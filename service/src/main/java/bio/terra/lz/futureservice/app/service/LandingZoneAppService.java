package bio.terra.lz.futureservice.app.service;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.StartLandingZoneCreation;
import bio.terra.lz.futureservice.app.service.exception.LandingZoneInvalidInputException;
import bio.terra.lz.futureservice.common.utils.MapperUtils;
import bio.terra.lz.futureservice.generated.model.ApiCreateAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiCreateLandingZoneResult;
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
}
