package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
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

public class CreateLandingZoneIdentityStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateLandingZoneIdentityStep.class);
  public static final String LANDING_ZONE_IDENTITY_ID = "LANDING_ZONE_IDENTITY_ID";
  public static final String LANDING_ZONE_IDENTITY_PRINCIPAL_ID =
      "LANDING_ZONE_IDENTITY_PRINCIPAL_ID";
  public static final String LANDING_ZONE_IDENTITY_RESOURCE_KEY = "LANDING_ZONE_IDENTITY";
  public static final String LANDING_ZONE_IDENTITY_CLIENT_ID = "LANDING_ZONE_IDENTITY_CLIENT_ID";

  public CreateLandingZoneIdentityStep(ResourceNameProvider resourceNameProvider) {
    super(resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    var identityName = resourceNameProvider.getName(getResourceType());

    var uami =
        armManagers
            .azureResourceManager()
            .identities()
            .define(identityName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
            .withTags(
                Map.of(
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId.toString(),
                    LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                    ResourcePurpose.POSTGRES_ADMIN.toString()))
            .create();

    context.getWorkingMap().put(LANDING_ZONE_IDENTITY_PRINCIPAL_ID, uami.principalId());
    context.getWorkingMap().put(LANDING_ZONE_IDENTITY_CLIENT_ID, uami.clientId());
    context.getWorkingMap().put(LANDING_ZONE_IDENTITY_ID, uami.id());
    context
        .getWorkingMap()
        .put(
            LANDING_ZONE_IDENTITY_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(uami.id())
                .resourceType(uami.type())
                .tags(uami.tags())
                .region(uami.regionName())
                .resourceName(uami.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), uami.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId, ArmManagers armManagers) {
    armManagers.azureResourceManager().identities().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "UserAssignedManagedIdentity";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(LANDING_ZONE_IDENTITY_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(getResourceType(), ResourceNameGenerator.UAMI_NAME_LENGTH));
  }
}
