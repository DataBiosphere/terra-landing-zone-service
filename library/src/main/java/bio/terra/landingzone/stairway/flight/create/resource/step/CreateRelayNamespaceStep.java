package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateRelayNamespaceStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateRelayNamespaceStep.class);
  public static final String RELAY_NAMESPACE_ID = "RELAY_NAMESPACE_ID";
  public static final String RELAY_NAMESPACE_RESOURCE_KEY = "RELAY_NAMESPACE";

  public CreateRelayNamespaceStep(
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

    var relayName = resourceNameProvider.getName(getResourceType());
    var relayNamespace =
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
    context.getWorkingMap().put(RELAY_NAMESPACE_ID, relayNamespace.id());
    context
        .getWorkingMap()
        .put(
            RELAY_NAMESPACE_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(relayNamespace.id())
                .resourceType(relayNamespace.type())
                .tags(relayNamespace.tags())
                .region(relayNamespace.regionName())
                .resourceName(relayNamespace.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), relayNamespace.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.relayManager().namespaces().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "RelayNamespace";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(RELAY_NAMESPACE_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_RELAY_NS_NAME_LENGTH));
  }
}
