package bio.terra.lz.futureservice.app.controller;

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
  public UnauthenticatedApiController() {
    currentVersion = new SystemVersion().build("1").gitTag("1");
  }

  @Override
  public ResponseEntity<SystemVersion> serviceVersion() {
    return new ResponseEntity<>(currentVersion, HttpStatus.OK);
  }
}
