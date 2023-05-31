package bio.terra.landingzone.stairway.flight.create.resource.step;

import static bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator.UAMI_NAME_LENGTH;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
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

  public CreateLandingZoneIdentityStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    var identityName = resourceNameGenerator.nextName(UAMI_NAME_LENGTH);

    var uami =
        armManagers
            .azureResourceManager()
            .identities()
            .define(identityName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
            .withTags(
                Map.of(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId.toString()))
            .create();

    context.getWorkingMap().put(LANDING_ZONE_IDENTITY_PRINCIPAL_ID, uami.principalId());
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
  protected void deleteResource(String resourceId) {
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
}
