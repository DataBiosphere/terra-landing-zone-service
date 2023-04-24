package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.exception.ManagementException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateRelayStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateRelayStep.class);
  public static final String RELAY_ID = "RELAY_ID";
  public static final String RELAY_RESOURCE_KEY = "RELAY";

  public CreateRelayStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
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
    var relayName = resourceNameGenerator.nextName(ResourceNameGenerator.MAX_RELAY_NS_NAME_LENGTH);

    var relay =
        armManagers
            .relayManager()
            .namespaces()
            .define(relayName)
            .withRegion(resourceGroup.region())
            .withExistingResourceGroup(resourceGroup.name())
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
    logger.info(RESOURCE_CREATED, getResourceType(), relay.id(), resourceGroup.name());
  }

  @Override
  protected String getResourceType() {
    return "Relay";
  }
}
