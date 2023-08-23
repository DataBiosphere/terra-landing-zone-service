package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ActiveDirectoryAuthEnum;
import com.azure.resourcemanager.postgresqlflexibleserver.models.AuthConfig;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Backup;
import com.azure.resourcemanager.postgresqlflexibleserver.models.CreateMode;
import com.azure.resourcemanager.postgresqlflexibleserver.models.GeoRedundantBackupEnum;
import com.azure.resourcemanager.postgresqlflexibleserver.models.HighAvailability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.HighAvailabilityMode;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Network;
import com.azure.resourcemanager.postgresqlflexibleserver.models.PasswordAuthEnum;
import com.azure.resourcemanager.postgresqlflexibleserver.models.PrincipalType;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerVersion;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Sku;
import com.azure.resourcemanager.postgresqlflexibleserver.models.SkuTier;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Storage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class CreatePostgresqlDbStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreatePostgresqlDbStep.class);
  public static final String POSTGRESQL_ID = "POSTGRESQL_ID";
  public static final String POSTGRESQL_RESOURCE_KEY = "POSTGRESQL";

  public CreatePostgresqlDbStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameProvider resourceNameProvider) {
    super(armManagers, parametersResolver, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var postgresName = resourceNameProvider.getName(getResourceType());

    var postgres = createServer(context, armManagers, postgresName);

    //TODO can this be done with one call - or even better, in the same call as creating the server?
    //Enable pg-bouncer
    armManagers
        .postgreSqlManager()
        .configurations()
        .define("pgbouncer.enabled")
        .withExistingFlexibleServer(getMRGName(context), postgresName)
        .withValue("true")
        .withSource("user-override")
        .create();
    armManagers
            .postgreSqlManager()
            .configurations()
            .define("metrics.pgbouncer_diagnostics")
            .withExistingFlexibleServer(getMRGName(context), postgresName)
            .withValue("on")
            .withSource("user-override")
            .create();
    armManagers
            .postgreSqlManager()
            .configurations()
            .define("pgbouncer.ignore_startup_parameters")
            .withExistingFlexibleServer(getMRGName(context), postgresName)
            .withValue("extra_float_digits")
            .withSource("user-override")
            .create();
    createAdminUser(context, armManagers, postgresName);

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

  private Server createServer(FlightContext context, ArmManagers armManagers, String postgresName) {
    try {
      var landingZoneId =
          getParameterOrThrow(
              context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

      var vNetId =
          getParameterOrThrow(context.getWorkingMap(), CreateVnetStep.VNET_ID, String.class);
      var dnsId =
          getParameterOrThrow(
              context.getWorkingMap(), CreatePostgresqlDNSStep.POSTGRESQL_DNS_ID, String.class);

      return armManagers
          .postgreSqlManager()
          .servers()
          .define(postgresName)
          .withRegion(getMRGRegionName(context))
          .withExistingResourceGroup(getMRGName(context))
          .withVersion(
              ServerVersion.fromString(
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_VERSION.name())))
          .withSku(
              new Sku()
                  .withName(
                      parametersResolver.getValue(
                          CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_SKU.name()))
                  .withTier(
                      SkuTier.fromString(
                          parametersResolver.getValue(
                              CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_SKU_TIER
                                  .name()))))
          .withNetwork(
              new Network()
                  .withDelegatedSubnetResourceId(
                      vNetId + "/subnets/" + CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET)
                  .withPrivateDnsZoneArmResourceId(dnsId))
          .withAuthConfig(
              new AuthConfig()
                  .withPasswordAuth(PasswordAuthEnum.DISABLED)
                  .withActiveDirectoryAuth(ActiveDirectoryAuthEnum.ENABLED))
          .withBackup(
              new Backup()
                  .withGeoRedundantBackup(GeoRedundantBackupEnum.DISABLED)
                  .withBackupRetentionDays(
                      Integer.parseInt(
                          parametersResolver.getValue(
                              CromwellBaseResourcesFactory.ParametersNames
                                  .POSTGRES_SERVER_BACKUP_RETENTION_DAYS
                                  .name()))))
          .withCreateMode(CreateMode.DEFAULT)
          .withHighAvailability(new HighAvailability().withMode(HighAvailabilityMode.DISABLED))
          .withStorage(
              new Storage()
                  .withStorageSizeGB(
                      Integer.parseInt(
                          parametersResolver.getValue(
                              CromwellBaseResourcesFactory.ParametersNames
                                  .POSTGRES_SERVER_STORAGE_SIZE_GB
                                  .name()))))
          .withTags(
              Map.of(
                  LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                  landingZoneId.toString(),
                  LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                  ResourcePurpose.SHARED_RESOURCE.toString(),
                      LandingZoneTagKeys.PGBOUNCER_ENABLED.toString(), "true"))
          .create();
    } catch (ManagementException e) {
      // resource may already exist if this step is being retried
      if (e.getResponse() != null
          && HttpStatus.CONFLICT.value() == e.getResponse().getStatusCode()) {
        return armManagers
            .postgreSqlManager()
            .servers()
            .getByResourceGroup(getMRGName(context), postgresName);
      } else {
        throw e;
      }
    }
  }

  private void createAdminUser(
      FlightContext context, ArmManagers armManagers, String postgresName) {
    var uami =
        context
            .getWorkingMap()
            .get(
                CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_RESOURCE_KEY,
                LandingZoneResource.class);
    var uamiPrincipalId =
        context
            .getWorkingMap()
            .get(CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_PRINCIPAL_ID, String.class);

    armManagers
        .postgreSqlManager()
        .administrators()
        .define(uamiPrincipalId)
        .withExistingFlexibleServer(getMRGName(context), postgresName)
        .withPrincipalName(uami.resourceName().orElseThrow())
        .withPrincipalType(PrincipalType.SERVICE_PRINCIPAL)
        .create();
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

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_POSTGRESQL_SERVER_NAME_LENGTH));
  }
}
