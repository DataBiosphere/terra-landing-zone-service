package bio.terra.landingzone.library.landingzones.management.exception;

import bio.terra.common.exception.NotFoundException;

public class ManagedResourceGroupNotFoundException extends NotFoundException {

  public ManagedResourceGroupNotFoundException(String message) {
    super(message);
  }
}
