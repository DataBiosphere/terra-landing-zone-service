package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.postgresql.models.PublicNetworkAccessEnum;
import com.azure.resourcemanager.postgresql.models.ServerPropertiesForDefaultCreate;
import com.azure.resourcemanager.postgresql.models.ServerVersion;
import com.azure.resourcemanager.postgresql.models.Sku;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePostgresqlDbStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreatePostgresqlDbStep.class);
  public static final String POSTGRESQL_ID = "POSTGRESQL_ID";
  public static final String POSTGRESQL_RESOURCE_KEY = "POSTGRESQL";

  public CreatePostgresqlDbStep(
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

    var postgresName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_POSTGRESQL_SERVER_NAME_LENGTH);

    var postgres =
        armManagers
            .postgreSqlManager()
            .servers()
            .define(postgresName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
            .withProperties(
                new ServerPropertiesForDefaultCreate()
                    .withAdministratorLogin(
                        parametersResolver.getValue(
                            CromwellBaseResourcesFactory.ParametersNames.POSTGRES_DB_ADMIN.name()))
                    .withAdministratorLoginPassword(
                        parametersResolver.getValue(
                            CromwellBaseResourcesFactory.ParametersNames.POSTGRES_DB_PASSWORD
                                .name()))
                    .withVersion(ServerVersion.ONE_ONE)
                    .withPublicNetworkAccess(PublicNetworkAccessEnum.DISABLED))
            .withSku(
                new Sku()
                    .withName(
                        parametersResolver.getValue(
                            CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_SKU
                                .name())))
            .withTags(
                Map.of(
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId.toString(),
                    LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                    ResourcePurpose.SHARED_RESOURCE.toString()))
            .create();

    context.getWorkingMap().put(POSTGRESQL_ID, postgres.id());
    context
        .getWorkingMap()
        .put(
            POSTGRESQL_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(postgres.id())
                .resourceType(postgres.type())
                .tags(postgres.tags())
                .region(postgres.regionName())
                .resourceName(postgres.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), postgres.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.postgreSqlManager().servers().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "Postgres";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(POSTGRESQL_ID, String.class));
  }
}
