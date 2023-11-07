package bio.terra.lz.futureservice.app.controller;

import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.lz.futureservice.app.controller.common.ResponseUtils;
import bio.terra.lz.futureservice.app.service.LandingZoneAppService;
import bio.terra.lz.futureservice.generated.api.LandingZonesApi;
import bio.terra.lz.futureservice.generated.model.ApiCreateAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiCreateLandingZoneResult;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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

    return new ResponseEntity<>(result, ResponseUtils.getAsyncResponseCode(result.getJobReport()));
  }
}
