package bio.terra.lz.futureservice.app.service;

import bio.terra.common.exception.ConflictException;
import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.job.model.JobReport;
import bio.terra.landingzone.library.landingzones.deployment.LandingZonePurpose;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.quotas.ResourceQuota;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.service.landingzone.azure.model.DeletedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.service.landingzone.azure.model.StartLandingZoneCreation;
import bio.terra.landingzone.service.landingzone.azure.model.StartLandingZoneDeletion;
import bio.terra.lz.futureservice.app.service.exception.LandingZoneInvalidInputException;
import bio.terra.lz.futureservice.app.service.exception.LandingZoneUnsupportedPurposeException;
import bio.terra.lz.futureservice.common.utils.MapperUtils;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZone;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDefinition;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDefinitionList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDeployedResource;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDetails;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResourcesList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResourcesPurposeGroup;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiCreateAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiCreateLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneJobResult;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiResourceQuota;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
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
    return toApiAzureLandingZoneResult(landingZoneService.getAsyncJobResult(bearerToken, jobId));
  }

  public ApiAzureLandingZoneList listAzureLandingZones(
      BearerToken bearerToken, UUID billingProfileId) {
    if (billingProfileId != null) {
      return getAzureLandingZonesByBillingProfile(bearerToken, billingProfileId);
    }
    List<LandingZone> landingZones = landingZoneService.listLandingZones(bearerToken);
    return new ApiAzureLandingZoneList()
        .landingzones(
            landingZones.stream().map(this::toApiAzureLandingZone).collect(Collectors.toList()));
  }

  public ApiDeleteAzureLandingZoneJobResult getDeleteAzureLandingZoneResult(
      BearerToken token, UUID landingZoneId, String jobId) {
    return toApiDeleteAzureLandingZoneJobResult(
        landingZoneService.getAsyncDeletionJobResult(token, landingZoneId, jobId));
  }

  public ApiAzureLandingZoneDefinitionList listAzureLandingZonesDefinitions(
      BearerToken bearerToken) {
    List<LandingZoneDefinition> templates =
        landingZoneService.listLandingZoneDefinitions(bearerToken);

    return new ApiAzureLandingZoneDefinitionList()
        .landingzones(
            templates.stream()
                .map(
                    t ->
                        new ApiAzureLandingZoneDefinition()
                            .definition(t.definition())
                            .name(t.name())
                            .description(t.description())
                            .version(t.version()))
                .collect(Collectors.toList()));
  }

  public ApiDeleteAzureLandingZoneResult deleteLandingZone(
      BearerToken bearerToken,
      UUID landingZoneId,
      ApiDeleteAzureLandingZoneRequestBody body,
      String resultEndpoint) {
    return toApiDeleteAzureLandingZoneResult(
        landingZoneService.startLandingZoneDeletionJob(
            bearerToken, body.getJobControl().getId(), landingZoneId, resultEndpoint));
  }

  public ApiAzureLandingZone getAzureLandingZone(BearerToken bearerToken, UUID landingZoneId) {
    LandingZone landingZoneRecord = landingZoneService.getLandingZone(bearerToken, landingZoneId);
    return toApiAzureLandingZone(landingZoneRecord);
  }

  public ApiAzureLandingZoneResourcesList listAzureLandingZoneResources(
      BearerToken bearerToken, UUID landingZoneId) {
    var result = new ApiAzureLandingZoneResourcesList().id(landingZoneId);
    landingZoneService
        .listResourcesWithPurposes(bearerToken, landingZoneId)
        .deployedResources()
        .forEach(
            (p, dp) ->
                result.addResourcesItem(
                    new ApiAzureLandingZoneResourcesPurposeGroup()
                        .purpose(p.toString())
                        .deployedResources(
                            dp.stream()
                                .map(r -> toApiAzureLandingZoneDeployedResource(r, p))
                                .toList())));
    return result;
  }

  public ApiResourceQuota getResourceQuota(
      BearerToken bearerToken, UUID landingZoneId, String azureResourceId) {
    return toApiResourceQuota(
        landingZoneId,
        landingZoneService.getResourceQuota(bearerToken, landingZoneId, azureResourceId));
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

  private ApiAzureLandingZoneList getAzureLandingZonesByBillingProfile(
      BearerToken bearerToken, UUID billingProfileId) {
    ApiAzureLandingZoneList result = new ApiAzureLandingZoneList();
    List<LandingZone> landingZones =
        landingZoneService.getLandingZonesByBillingProfile(bearerToken, billingProfileId);
    if (landingZones.size() > 0) {
      // The enforced logic is 1:1 relation between Billing Profile and a Landing Zone.
      // The landing zone service returns one record in the list if landing zone exists
      // for a given billing profile.
      if (landingZones.size() == 1) {
        result.addLandingzonesItem(toApiAzureLandingZone(landingZones.get(0)));
      } else {
        throw new ConflictException(
            String.format(
                "There are more than one landing zone found for the given billing profile: '%s'. Please"
                    + " check the landing zone deployment is correct.",
                billingProfileId));
      }
    }
    return result;
  }

  private ApiAzureLandingZone toApiAzureLandingZone(LandingZone landingZone) {
    return new ApiAzureLandingZone()
        .billingProfileId(landingZone.billingProfileId())
        .landingZoneId(landingZone.landingZoneId())
        .definition(landingZone.definition())
        .version(landingZone.version())
        .createdDate(landingZone.createdDate());
  }

  private ApiDeleteAzureLandingZoneJobResult toApiDeleteAzureLandingZoneJobResult(
      LandingZoneJobService.AsyncJobResult<DeletedLandingZone> jobResult) {
    var apiJobResult =
        new ApiDeleteAzureLandingZoneJobResult()
            .jobReport(MapperUtils.JobReportMapper.from(jobResult.getJobReport()))
            .errorReport(MapperUtils.ErrorReportMapper.from(jobResult.getApiErrorReport()));

    if (jobResult.getJobReport().getStatus().equals(JobReport.StatusEnum.SUCCEEDED)) {
      apiJobResult.landingZoneId(jobResult.getResult().landingZoneId());
      apiJobResult.resources(jobResult.getResult().deleteResources());
    }
    return apiJobResult;
  }

  private ApiDeleteAzureLandingZoneResult toApiDeleteAzureLandingZoneResult(
      LandingZoneJobService.AsyncJobResult<StartLandingZoneDeletion> jobResult) {
    return new ApiDeleteAzureLandingZoneResult()
        .jobReport(MapperUtils.JobReportMapper.from(jobResult.getJobReport()))
        .errorReport(MapperUtils.ErrorReportMapper.from(jobResult.getApiErrorReport()))
        .landingZoneId(jobResult.getResult().landingZoneId());
  }

  private ApiAzureLandingZoneDeployedResource toApiAzureLandingZoneDeployedResource(
      LandingZoneResource resource, LandingZonePurpose purpose) {
    if (purpose.getClass().equals(ResourcePurpose.class)) {
      return new ApiAzureLandingZoneDeployedResource()
          .resourceId(resource.resourceId())
          .resourceType(resource.resourceType())
          .tags(resource.tags())
          .region(resource.region());
    }
    if (purpose.getClass().equals(SubnetResourcePurpose.class)) {
      return new ApiAzureLandingZoneDeployedResource()
          .resourceParentId(resource.resourceParentId().orElse(null)) // Only available for subnets
          .resourceName(resource.resourceName().orElse(null)) // Only available for subnets
          .resourceType(resource.resourceType())
          .resourceId(resource.resourceId())
          .tags(resource.tags())
          .region(resource.region());
    }
    throw new LandingZoneUnsupportedPurposeException(
        String.format(
            "Support for purpose type %s is not implemented.", purpose.getClass().getSimpleName()));
  }

  private ApiResourceQuota toApiResourceQuota(UUID landingZoneId, ResourceQuota resourceQuota) {
    return new ApiResourceQuota()
        .landingZoneId(landingZoneId)
        .azureResourceId(resourceQuota.resourceId())
        .resourceType(resourceQuota.resourceType())
        .quotaValues(resourceQuota.quota());
  }
}
