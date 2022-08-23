package bio.terra.landingzone.service.landingzone.azure.exception;

import bio.terra.common.exception.NotFoundException;

public class LandingZoneDefinitionNotFound extends NotFoundException {
  public LandingZoneDefinitionNotFound(String message) {
    super(message);
  }
}
