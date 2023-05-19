package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.network.models.PrivateLinkSubResourceName;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePrivateEndpointStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreatePrivateEndpointStep.class);
  public static final String PRIVATE_ENDPOINT_ID = "PRIVATE_ENDPOINT_ID";
  public static final String PRIVATE_ENDPOINT_RESOURCE_KEY = "PRIVATE_ENDPOINT";

  public CreatePrivateEndpointStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    String postgreSqlId =
        getParameterOrThrow(
            context.getWorkingMap(), CreatePostgresqlDbStep.POSTGRESQL_ID, String.class);
    String vNetId =
        getParameterOrThrow(context.getWorkingMap(), CreateVnetStep.VNET_ID, String.class);

    String privateEndpointName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_PRIVATE_ENDPOINT_NAME_LENGTH);
    String privateLinkServiceConnectionName =
        resourceNameGenerator.nextName(
            ResourceNameGenerator.MAX_PRIVATE_LINK_CONNECTION_NAME_LENGTH);

    var vNetwork = armManagers.azureResourceManager().networks().getById(vNetId);
    var privateEndpoint =
        armManagers
            .azureResourceManager()
            .privateEndpoints()
            .define(privateEndpointName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
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
    context
        .getWorkingMap()
        .put(
            PRIVATE_ENDPOINT_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(privateEndpoint.id())
                .resourceType(privateEndpoint.type())
                .tags(privateEndpoint.tags())
                .region(privateEndpoint.regionName())
                .resourceName(privateEndpoint.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), privateEndpoint.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.azureResourceManager().privateEndpoints().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "PrivateEndpoint";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(PRIVATE_ENDPOINT_ID, String.class));
  }
}
