package bio.terra.landingzone.service.bpm.exception;

import bio.terra.common.exception.NotFoundException;

public class BillingProfileNotFoundException extends NotFoundException {
    public BillingProfileNotFoundException(String message) {
        super(message);
    }

    public BillingProfileNotFoundException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
