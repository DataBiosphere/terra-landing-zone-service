package bio.terra.landingzone.stairway.flight.create;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.service.iam.SamService;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateSamResourceStep implements Step {
    private static final Logger logger = LoggerFactory.getLogger(CreateSamResourceStep.class);

    private final SamService samService;

    public CreateSamResourceStep(
            SamService samService) {
        this.samService = samService;
    }

    @Override
    public StepResult doStep(FlightContext context)
            throws InterruptedException, RetryException {
        var bearerToken =
                context.getInputParameters().get(LandingZoneFlightMapKeys.BEARER_TOKEN, BearerToken.class);
        var requestedLandingZone =
                context.getInputParameters().get(LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);

        var landingZoneId =
                context
                        .getWorkingMap()
                        .get(LandingZoneFlightMapKeys.DEPLOYED_AZURE_LANDING_ZONE_ID, String.class);



        samService.createLandingZone(bearerToken, requestedLandingZone.billingProfileId().toString(), landingZoneId);
        return StepResult.getStepResultSuccess();
    }

    @Override
    public StepResult undoStep(FlightContext context) throws InterruptedException {
        var bearerToken =
                context.getInputParameters().get(LandingZoneFlightMapKeys.BEARER_TOKEN, BearerToken.class);

        var landingZoneId =
                context
                        .getWorkingMap()
                        .get(LandingZoneFlightMapKeys.DEPLOYED_AZURE_LANDING_ZONE_ID, String.class);
        samService.deleteLandingZone(bearerToken, landingZoneId);
        return StepResult.getStepResultSuccess();
    }

}
