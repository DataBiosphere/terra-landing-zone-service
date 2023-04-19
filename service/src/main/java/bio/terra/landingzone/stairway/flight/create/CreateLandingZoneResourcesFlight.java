package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.LandingZoneStepsDefinitionProviderFactory;
import bio.terra.landingzone.stairway.flight.StepsDefinitionProvider;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;

public class CreateLandingZoneResourcesFlight extends Flight {

  private final StepsDefinitionProvider stepsDefinitionProvider;
  private final LandingZoneRequest landingZoneRequest;

  /**
   * All subclasses must provide a constructor with this signature.
   *
   * @param inputParameters FlightMap of the inputs for the flight
   * @param applicationContext Anonymous context meaningful to the application using Stairway
   */
  public CreateLandingZoneResourcesFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);

    landingZoneRequest =
        inputParameters.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);

    stepsDefinitionProvider =
        LandingZoneStepsDefinitionProviderFactory.create(landingZoneRequest.definition());

    final LandingZoneFlightBeanBag flightBeanBag =
        LandingZoneFlightBeanBag.getFromObject(applicationContext);
    addCreateSteps(flightBeanBag, inputParameters);
  }

  private void addCreateSteps(LandingZoneFlightBeanBag flightBeanBag, FlightMap inputParameters) {
    var resourceNameGenerator =
        new ResourceNameGenerator(landingZoneRequest.landingZoneId().toString());
    stepsDefinitionProvider
        .get(flightBeanBag.getAzureConfiguration(), resourceNameGenerator)
        .forEach(pair -> addStep(pair.getLeft(), pair.getRight()));
  }
}
