package bio.terra.lz.futureservice.app.controller;

import static bio.terra.lz.futureservice.app.controller.common.ResponseUtils.getAsyncResponseCode;

import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.landingzone.terraform.TerraformService;
import bio.terra.lz.futureservice.app.service.LandingZoneAppService;
import bio.terra.lz.futureservice.generated.api.LandingZonesApi;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZone;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDefinitionList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResourcesList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiCreateAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiCreateLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneJobResult;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiResourceQuota;
import bio.terra.lz.futureservice.generated.model.ApiTerraformPlanOutput;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class LandingZoneApiController implements LandingZonesApi {
  private final HttpServletRequest request;
  private final BearerTokenFactory bearerTokenFactory;
  private final LandingZoneAppService landingZoneAppService;
  private final TerraformService terraformMain;

  @Autowired
  public LandingZoneApiController(
      HttpServletRequest request,
      BearerTokenFactory bearerTokenFactory,
      LandingZoneAppService landingZoneAppService,
      TerraformService terraformMain) {
    this.request = request;
    this.bearerTokenFactory = bearerTokenFactory;
    this.landingZoneAppService = landingZoneAppService;
    this.terraformMain = terraformMain;
  }

  @Override
  public ResponseEntity<ApiCreateLandingZoneResult> createAzureLandingZone(
      @RequestBody ApiCreateAzureLandingZoneRequestBody body) {
    String resultEndpoint =
        String.format(
            "%s/%s/%s", request.getServletPath(), "create-result", body.getJobControl().getId());
    ApiCreateLandingZoneResult result =
        landingZoneAppService.createAzureLandingZone(
            bearerTokenFactory.from(request), body, resultEndpoint);

    return new ResponseEntity<>(result, getAsyncResponseCode(result.getJobReport()));
  }

  @Override
  public ResponseEntity<ApiAzureLandingZoneResult> getCreateAzureLandingZoneResult(String jobId) {
    ApiAzureLandingZoneResult result =
        landingZoneAppService.getCreateAzureLandingZoneResult(
            bearerTokenFactory.from(request), jobId);
    return new ResponseEntity<>(result, getAsyncResponseCode(result.getJobReport()));
  }

  @Override
  public ResponseEntity<ApiAzureLandingZoneList> listAzureLandingZones(UUID billingProfileId) {
    ApiAzureLandingZoneList result =
        landingZoneAppService.listAzureLandingZones(
            bearerTokenFactory.from(request), billingProfileId);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiDeleteAzureLandingZoneJobResult> getDeleteAzureLandingZoneResult(
      UUID landingZoneId, String jobId) {
    ApiDeleteAzureLandingZoneJobResult response =
        landingZoneAppService.getDeleteAzureLandingZoneResult(
            bearerTokenFactory.from(request), landingZoneId, jobId);
    return new ResponseEntity<>(response, getAsyncResponseCode(response.getJobReport()));
  }

  @Override
  public ResponseEntity<ApiAzureLandingZoneDefinitionList> listAzureLandingZonesDefinitions() {
    ApiAzureLandingZoneDefinitionList result =
        landingZoneAppService.listAzureLandingZonesDefinitions(bearerTokenFactory.from(request));
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiDeleteAzureLandingZoneResult> deleteAzureLandingZone(
      UUID landingZoneId, ApiDeleteAzureLandingZoneRequestBody body) {
    String resultEndpoint =
        String.format(
            "%s/%s/%s", request.getServletPath(), "delete-result", body.getJobControl().getId());
    ApiDeleteAzureLandingZoneResult result =
        landingZoneAppService.deleteLandingZone(
            bearerTokenFactory.from(request), landingZoneId, body, resultEndpoint);
    return new ResponseEntity<>(result, getAsyncResponseCode(result.getJobReport()));
  }

  @Override
  public ResponseEntity<ApiAzureLandingZone> getAzureLandingZone(UUID landingZoneId) {
    ApiAzureLandingZone result =
        landingZoneAppService.getAzureLandingZone(bearerTokenFactory.from(request), landingZoneId);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiAzureLandingZoneResourcesList> listAzureLandingZoneResources(
      UUID landingZoneId) {
    ApiAzureLandingZoneResourcesList result =
        landingZoneAppService.listAzureLandingZoneResources(
            bearerTokenFactory.from(request), landingZoneId);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiResourceQuota> getResourceQuotaResult(
      UUID landingZoneId, String azureResourceId) {
    ApiResourceQuota result =
        landingZoneAppService.getResourceQuota(
            bearerTokenFactory.from(request), landingZoneId, azureResourceId);

    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTerraformPlanOutput> terraformPlan(UUID landingZoneId) {
    var result = terraformMain.terraformPlan(landingZoneId, bearerTokenFactory.from(request));

    return new ResponseEntity<>(new ApiTerraformPlanOutput().result(result), HttpStatus.OK);
  }
}
