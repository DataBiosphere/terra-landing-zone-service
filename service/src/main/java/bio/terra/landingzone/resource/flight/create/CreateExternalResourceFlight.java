package bio.terra.landingzone.resource.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.model.AuthenticatedUserRequest;
import bio.terra.landingzone.resource.ExternalResource;
import bio.terra.landingzone.resource.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;

/** Flight for creation of External resources such as Azure Landing Zone */
public class CreateExternalResourceFlight extends Flight {

  @Override
  public void addStep(Step step, RetryRule retryRule) {
    super.addStep(step, retryRule);
  }

  /**
   * All subclasses must provide a constructor with this signature.
   *
   * @param inputParameters FlightMap of the inputs for the flight
   * @param applicationContext Anonymous context meaningful to the application using Stairway
   */
  public CreateExternalResourceFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);
    final LandingZoneFlightBeanBag flightBeanBag =
        LandingZoneFlightBeanBag.getFromObject(applicationContext);

    final ExternalResource resource =
        inputParameters.get(LandingZoneFlightMapKeys.ResourceKeys.RESOURCE, ExternalResource.class);
    final AuthenticatedUserRequest userRequest =
        inputParameters.get(JobMapKeys.AUTH_USER_INFO.getKeyName(), AuthenticatedUserRequest.class);

    // seems we don't need any additional steps here only that which are available on resource level

    resource.addCreateSteps(this, flightBeanBag);
  }
}
