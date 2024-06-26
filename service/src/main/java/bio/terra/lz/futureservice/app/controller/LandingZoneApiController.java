package bio.terra.lz.futureservice.app.controller;

import static bio.terra.lz.futureservice.app.controller.common.ResponseUtils.getAsyncResponseCode;

import bio.terra.common.exception.BadRequestException;
import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.lz.futureservice.app.service.LandingZoneAppService;
import bio.terra.lz.futureservice.common.utils.RequestQueryParamUtils;
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

  @Autowired
  public LandingZoneApiController(
      HttpServletRequest request,
      BearerTokenFactory bearerTokenFactory,
      LandingZoneAppService landingZoneAppService) {
    this.request = request;
    this.bearerTokenFactory = bearerTokenFactory;
    this.landingZoneAppService = landingZoneAppService;
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
    /*
    In certain cases, the billingProfileId parameter may be null, even if the original query parameter
    of the GET request contains a value for it. This situation can arise during Tomcat server request
    parameter validation, even before validating the type (UUID) of the parameter. For instance,
    the query string parameter’s value might be sanitized if it contains forbidden characters.
    It is important to distinguish between scenarios where the parameter was initially supplied
    and when it was subsequently sanitized.

    Example: Following value '%7bbase%7d%22%20or%20version()%20like%20%user' will be sanitized and
    current method will receive null.
     */
    if (RequestQueryParamUtils.isBillingProfileIdSanitized(
        billingProfileId, request.getQueryString())) {
      throw new BadRequestException("Value of the billingProfileId parameter is not valid.");
    }

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
}
