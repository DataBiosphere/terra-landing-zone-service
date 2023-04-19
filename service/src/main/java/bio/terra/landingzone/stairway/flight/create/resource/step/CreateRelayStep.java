package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateRelayStep extends BaseResourceCreateStep {
  public static final String RELAY_ID = "RELAY_ID";
  private static final Logger logger = LoggerFactory.getLogger(CreateRelayStep.class);

  public CreateRelayStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);
    var relayName = resourceNameGenerator.nextName(ResourceNameGenerator.MAX_RELAY_NS_NAME_LENGTH);
    try {
      var relay =
          armManagers
              .relayManager()
              .namespaces()
              .define(relayName)
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup.name())
              .create();
      context.getWorkingMap().put(RELAY_ID, relay.id());
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(RESOURCE_ALREADY_EXISTS, "Relay", relayName, resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "relay", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
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
}
