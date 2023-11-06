package bio.terra.lz.futureservice.app.controller;

import bio.terra.lz.futureservice.app.configuration.VersionConfiguration;
import bio.terra.lz.futureservice.generated.api.UnauthenticatedApi;
import bio.terra.lz.futureservice.generated.model.SystemVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UnauthenticatedApiController implements UnauthenticatedApi {

  private final SystemVersion currentVersion;

  @Autowired
  public UnauthenticatedApiController(VersionConfiguration versionConfiguration) {
    currentVersion =
        new SystemVersion()
            .gitTag(versionConfiguration.getGitTag())
            .gitHash(versionConfiguration.getGitHash())
            .github(
                "https://github.com/DataBiosphere/terra-landing-zone-service/commit/"
                    + versionConfiguration.getGitHash())
            .build(versionConfiguration.getBuild());
  }

  @Override
  public ResponseEntity<SystemVersion> serviceVersion() {
    return new ResponseEntity<>(currentVersion, HttpStatus.OK);
  }
}
