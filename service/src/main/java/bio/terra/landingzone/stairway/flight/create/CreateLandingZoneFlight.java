package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;

/** Flight for creation of a Landing Zone */
public class CreateLandingZoneFlight extends Flight {

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
  public CreateLandingZoneFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);
    final LandingZoneFlightBeanBag flightBeanBag =
        LandingZoneFlightBeanBag.getFromObject(applicationContext);

    //    final ExternalResource resource =
    //        inputParameters.get(LandingZoneFlightMapKeys.ResourceKeys.RESOURCE,
    // ExternalResource.class);
    //    final AuthenticatedUserRequest userRequest =
    //        inputParameters.get(JobMapKeys.AUTH_USER_INFO.getKeyName(),
    // AuthenticatedUserRequest.class);

    // seems we don't need any additional steps here only that which are available on resource level

    // resource.addCreateSteps(this, flightBeanBag);
    addCreateSteps(flightBeanBag);
  }

  private void addCreateSteps(LandingZoneFlightBeanBag flightBeanBag) {

    addStep(
        new CreateAzureExternalLandingZoneStep(flightBeanBag.getAzureLandingZoneManagerProvider()),
        RetryRules.cloud());

    addStep(
        new CreateAzureLandingZoneDbRecordStep(flightBeanBag.getLandingZoneDao()),
        RetryRules.shortDatabase());
  }
}
