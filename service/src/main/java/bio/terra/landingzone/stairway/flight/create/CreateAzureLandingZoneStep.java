package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDefinitionNotFound;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAzureLandingZoneStep implements Step {
  private static final Logger logger = LoggerFactory.getLogger(CreateAzureLandingZoneStep.class);

  private final LandingZoneManagerProvider landingZoneManagerProvider;

  public CreateAzureLandingZoneStep(LandingZoneManagerProvider landingZoneManagerProvider) {
    this.landingZoneManagerProvider = landingZoneManagerProvider;
  }

  @Override
  public StepResult doStep(FlightContext context) throws RetryException {
    FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(
        inputMap,
        LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
        LandingZoneFlightMapKeys.BILLING_PROFILE,
        LandingZoneFlightMapKeys.LANDING_ZONE_ID);

    var requestedLandingZone =
        inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);
    var billingProfile = inputMap.get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);
    var landingZoneId = inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    try {
      DeployedLandingZone deployedLandingZone =
          createLandingZone(
              landingZoneId,
              requestedLandingZone,
              landingZoneManagerProvider.createLandingZoneManager(
                  LandingZoneTarget.fromBillingProfile(billingProfile)));

      persistResponse(context, deployedLandingZone);

      logger.info(
          "Successfully created Azure landing zone. id='{}', deployed resources='{}'",
          deployedLandingZone.id(),
          listDeployedResourcesAsCsv(deployedLandingZone.deployedResources()));
    } catch (LandingZoneDefinitionNotFound e) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    } catch (Exception e) {
      // TODO SG: check if we can retry?
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    return null;
  }

  private String listDeployedResourcesAsCsv(List<LandingZoneResource> deployedResources) {
    if (deployedResources == null || deployedResources.isEmpty()) {
      return "No resource found!";
    }
    return deployedResources.stream()
        .map(LandingZoneResource::resourceId)
        .collect(Collectors.joining());
  }

  private void persistResponse(FlightContext context, DeployedLandingZone deployedLandingZone) {
    FlightMap workingMap = context.getWorkingMap();
    workingMap.put(JobMapKeys.RESPONSE.getKeyName(), deployedLandingZone);
  }

  private DeployedLandingZone createLandingZone(
      UUID landingZoneId,
      LandingZoneRequest landingZoneRequest,
      LandingZoneManager landingZoneManager) {
    List<DeployedResource> deployedResources =
        landingZoneManager.deployLandingZone(
            landingZoneId.toString(),
            landingZoneRequest.definition(),
            DefinitionVersion.fromString(landingZoneRequest.version()),
            landingZoneRequest.parameters());

    logger.info(
        "Azure Landing Zone definition with the following "
            + "parameters: definition={}, version={} successfully created.",
        landingZoneRequest.definition(),
        landingZoneRequest.version());
    return DeployedLandingZone.builder()
        .id(landingZoneId.toString())
        .deployedResources(
            deployedResources.stream()
                .map(
                    dr ->
                        LandingZoneResource.builder()
                            .resourceId(dr.resourceId())
                            .resourceType(dr.resourceType())
                            .tags(dr.tags())
                            .region(dr.region())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }
}
