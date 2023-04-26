package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.exception.ManagementException;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateRelayStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateRelayStep.class);
  public static final String RELAY_ID = "RELAY_ID";
  public static final String RELAY_RESOURCE_KEY = "RELAY";

  public CreateRelayStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    var relayId = context.getWorkingMap().get(RELAY_ID, String.class);
    try {
      armManagers.azureResourceManager().storageAccounts().deleteById(relayId);
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        return StepResult.getStepResultSuccess();
      }
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    var relayName = resourceNameGenerator.nextName(ResourceNameGenerator.MAX_RELAY_NS_NAME_LENGTH);
    var relay =
        armManagers
            .relayManager()
            .namespaces()
            .define(relayName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
            .withTags(
                Map.of(
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId.toString(),
                    LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                    ResourcePurpose.SHARED_RESOURCE.toString()))
            .create();
    context.getWorkingMap().put(RELAY_ID, relay.id());
    context
        .getWorkingMap()
        .put(
            RELAY_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(relay.id())
                .resourceType(relay.type())
                .tags(relay.tags())
                .region(relay.regionName())
                .resourceName(relay.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), relay.id(), getMRGName(context));
  }

  @Override
  protected String getResourceType() {
    return "Relay";
  }
}
