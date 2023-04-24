package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AggregateLandingZoneResourcesStep implements Step {
  private List<String> deployedResourcesKeys =
      List.of(
          CreateVnetStep.VNET_RESOURCE_KEY,
          CreateBatchAccountStep.BATCH_ACCOUNT_RESOURCE_KEY,
          CreateStorageAccountStep.STORAGE_ACCOUNT_RESOURCE_KEY,
          CreatePostgresqlDbStep.POSTGRESQL_RESOURCE_KEY,
          CreatePrivateEndpointStep.PRIVATE_ENDPOINT_RESOURCE_KEY,
          CreateRelayStep.RELAY_RESOURCE_KEY);

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var landingZoneId =
        context.getInputParameters().get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    // persist final landing zone creation flight result
    var deployedLandingZone =
        DeployedLandingZone.builder()
            .id(landingZoneId)
            .deployedResources(getDeployedResources(context))
            .build();
    context.getWorkingMap().put(JobMapKeys.RESPONSE.getKeyName(), deployedLandingZone);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }

  private List<LandingZoneResource> getDeployedResources(FlightContext context) {
    var result = new ArrayList<LandingZoneResource>();
    deployedResourcesKeys.forEach(
        key -> {
          var deployedResource = context.getWorkingMap().get(key, LandingZoneResource.class);
          if (deployedResource != null) {
            result.add(deployedResource);
          }
        });
    return result;
  }
}
