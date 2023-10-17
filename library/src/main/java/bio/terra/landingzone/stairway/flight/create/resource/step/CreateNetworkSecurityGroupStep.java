package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.network.models.NetworkSecurityGroup;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class CreateNetworkSecurityGroupStep extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateNetworkSecurityGroupStep.class);
  public static final String NSG_ID = "NSG_ID";
  public static final String NSG_RESOURCE_KEY = "NSG";

  public CreateNetworkSecurityGroupStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameProvider resourceNameProvider) {
    super(armManagers, parametersResolver, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var nsgName = resourceNameProvider.getName(getResourceType());

    NetworkSecurityGroup nsg = null;
    try {
      nsg =
          armManagers
              .azureResourceManager()
              .networkSecurityGroups()
              .define(nsgName)
              .withRegion(getMRGRegionName(context))
              .withExistingResourceGroup(getMRGName(context))
              .withTags(
                  Map.of(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId.toString()))
              .create();
    } catch (ManagementException e) {
      // resource may already exist if this step is being retried
      if (e.getResponse() != null
          && HttpStatus.CONFLICT.value() == e.getResponse().getStatusCode()) {
        nsg =
            armManagers
                .azureResourceManager()
                .networkSecurityGroups()
                .getByResourceGroup(getMRGName(context), nsgName);
      } else {
        throw e;
      }
    }

    context.getWorkingMap().put(NSG_ID, nsg.id());
    context
        .getWorkingMap()
        .put(
            NSG_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(nsg.id())
                .resourceType(nsg.type())
                .tags(nsg.tags())
                .region(nsg.regionName())
                .resourceName(nsg.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), nsg.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.azureResourceManager().networkSecurityGroups().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "NetworkSecurityGroup";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(NSG_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(getResourceType(), ResourceNameGenerator.MAX_NSG_NAME_LENGTH));
  }
}
