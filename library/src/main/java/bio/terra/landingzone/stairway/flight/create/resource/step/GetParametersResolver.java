package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ParametersResolverProvider;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;

public class GetParametersResolver implements Step {

  private final ParametersResolverProvider parametersResolverProvider;

  public GetParametersResolver(ParametersResolverProvider parametersResolverProvider) {
    this.parametersResolverProvider = parametersResolverProvider;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    TargetManagedResourceGroup targetMrg =
        FlightUtils.getRequired(
            context.getWorkingMap(),
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            TargetManagedResourceGroup.class);
    LandingZoneRequest requestParameters =
        FlightUtils.getRequired(
            context.getInputParameters(),
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            LandingZoneRequest.class);

    var parametersResolver =
        parametersResolverProvider.create(requestParameters.parameters(), targetMrg.region());
    context
        .getWorkingMap()
        .put(LandingZoneFlightMapKeys.CREATE_LANDING_ZONE_PARAMETERS_RESOLVER, parametersResolver);

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
