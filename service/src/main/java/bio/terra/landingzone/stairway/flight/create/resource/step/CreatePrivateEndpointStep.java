package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.network.models.PrivateLinkSubResourceName;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePrivateEndpointStep extends BaseResourceCreateStep {
  public static final String PRIVATE_ENDPOINT_ID = "PRIVATE_ENDPOINT_ID";
  private static final Logger logger = LoggerFactory.getLogger(CreatePrivateEndpointStep.class);

  public CreatePrivateEndpointStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  // TODO: check dependencies method!?

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);
    String postgreSqlId =
        getParameterOrThrow(
            context.getWorkingMap(), CreatePostgresqlDbStep.POSTGRESQL_ID, String.class);
    String privateEndpointName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_PRIVATE_ENDPOINT_NAME_LENGTH);
    String privateLinkServiceConnectionName =
        resourceNameGenerator.nextName(
            ResourceNameGenerator.MAX_PRIVATE_LINK_CONNECTION_NAME_LENGTH);
    try {
      String vNetId =
          getParameterOrThrow(context.getWorkingMap(), CreateVnetStep.VNET_ID, String.class);
      var vNetwork = armManagers.azureResourceManager().networks().getById(vNetId);
      var privateEndpoint =
          armManagers
              .azureResourceManager()
              .privateEndpoints()
              .define(privateEndpointName)
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup)
              .withSubnetId(
                  vNetwork
                      .subnets()
                      .get(CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET.name())
                      .id())
              .definePrivateLinkServiceConnection(privateLinkServiceConnectionName)
              .withResourceId(postgreSqlId)
              .withSubResource(PrivateLinkSubResourceName.fromString("postgresqlServer"))
              .attach()
              .create();
      context.getWorkingMap().put(PRIVATE_ENDPOINT_ID, privateEndpoint.id());
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(
            RESOURCE_ALREADY_EXISTS, "Private endpoint", privateEndpointName, resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "private endpoint", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    var privateEndpointId = context.getWorkingMap().get(PRIVATE_ENDPOINT_ID, String.class);
    try {
      armManagers.azureResourceManager().privateEndpoints().deleteById(privateEndpointId);
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        return StepResult.getStepResultSuccess();
      }
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }
}
