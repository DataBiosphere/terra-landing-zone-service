package bio.terra.landingzone.service.landingzone.azure.exception;

import bio.terra.common.exception.NotFoundException;

public class AzureLandingZoneDefinitionNotFound extends NotFoundException {
  public AzureLandingZoneDefinitionNotFound(String message) {
    super(message);
  }
}
