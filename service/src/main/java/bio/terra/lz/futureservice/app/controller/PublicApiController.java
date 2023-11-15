package bio.terra.lz.futureservice.app.controller;

import bio.terra.lz.futureservice.app.configuration.VersionConfiguration;
import bio.terra.lz.futureservice.app.service.StatusService;
import bio.terra.lz.futureservice.generated.api.PublicApi;
import bio.terra.lz.futureservice.generated.model.ApiSystemStatus;
import bio.terra.lz.futureservice.generated.model.ApiSystemVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {

  private final ApiSystemVersion currentVersion;
  private final StatusService statusService;

  @Autowired
  public PublicApiController(
      StatusService statusService, VersionConfiguration versionConfiguration) {
    this.statusService = statusService;
    currentVersion =
        new ApiSystemVersion()
            .gitTag(versionConfiguration.getGitTag())
            .gitHash(versionConfiguration.getGitHash())
            .github(
                "https://github.com/DataBiosphere/terra-landing-zone-service/commit/"
                    + versionConfiguration.getGitHash())
            .build(versionConfiguration.getBuild());
  }

  @Override
  public ResponseEntity<ApiSystemVersion> serviceVersion() {
    return new ResponseEntity<>(currentVersion, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiSystemStatus> serviceStatus() {
    ApiSystemStatus systemStatus = statusService.getCurrentStatus();
    HttpStatus httpStatus = systemStatus.isOk() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    return new ResponseEntity<>(systemStatus, httpStatus);
  }
}
