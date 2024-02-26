package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.network.models.PrivateEndpoint;
import com.azure.resourcemanager.network.models.PrivateLinkSubResourceName;
import java.util.List;
import java.util.Optional;

public class CreateStorageAccountPrivateEndpointStep extends BaseResourceCreateStep {
  public static final String STORAGE_ACCOUNT_PRIVATE_ENDPOINT_ID =
      "STORAGE_ACCOUNT_PRIVATE_ENDPOINT_ID";

  public CreateStorageAccountPrivateEndpointStep(
      ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var storageAccountId =
        getParameterOrThrow(
            context.getWorkingMap(), CreateStorageAccountStep.STORAGE_ACCOUNT_ID, String.class);

    var vnetId = getParameterOrThrow(context.getWorkingMap(), CreateVnetStep.VNET_ID, String.class);

    var privateEndpointName = resourceNameProvider.getName(getResourceType());
    PrivateEndpoint privateEndpoint =
        armManagers
            .azureResourceManager()
            .privateEndpoints()
            .define(privateEndpointName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
            .withSubnetId(vnetId + "/subnets/COMPUTE_SUBNET")
            .definePrivateLinkServiceConnection(privateEndpointName)
            .withResourceId(storageAccountId)
            .withSubResource(PrivateLinkSubResourceName.STORAGE_BLOB) // primary blob
            .attach()
            .create();

    context.getWorkingMap().put(STORAGE_ACCOUNT_PRIVATE_ENDPOINT_ID, privateEndpoint.id());
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.azureResourceManager().privateEndpoints().deleteById(resourceId);
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_PRIVATE_ENDPOINT_NAME_LENGTH));
  }

  @Override
  protected String getResourceType() {
    return "PrivateEndpoint";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(
        context.getWorkingMap().get(STORAGE_ACCOUNT_PRIVATE_ENDPOINT_ID, String.class));
  }
}
