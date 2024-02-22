package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresqlflexibleserver.models.PrincipalType;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class CreatePostgresqlDbAdminStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreatePostgresqlDbStep.class);
  private static final String POSTGRESQL_ADMIN_ID = "POSTGRESQL_ADMIN_ID";

  public CreatePostgresqlDbAdminStep(
      ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return Collections.emptyList();
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var uami =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_RESOURCE_KEY,
            LandingZoneResource.class);
    var uamiPrincipalId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_PRINCIPAL_ID,
            String.class);
    var postgresName =
        getParameterOrThrow(
            context.getWorkingMap(), CreatePostgresqlDbStep.POSTGRESQL_NAME, String.class);

    try {
      var administrator =
          armManagers
              .postgreSqlManager()
              .administrators()
              .define(uamiPrincipalId)
              .withExistingFlexibleServer(getMRGName(context), postgresName)
              .withPrincipalName(uami.resourceName().orElseThrow())
              .withPrincipalType(PrincipalType.SERVICE_PRINCIPAL)
              .create();
      context.getWorkingMap().put(POSTGRESQL_ADMIN_ID, administrator.id());
    } catch (ManagementException e) {
      if (e.getResponse() != null
          && HttpStatus.CONFLICT.value() == e.getResponse().getStatusCode()) {
        logger.info(
            "Admin {} already exists for the database {}, skipping creation",
            uami.resourceId(),
            postgresName);
      } else {
        throw e;
      }
    }
  }

  @Override
  protected void deleteResource(String resourceId) {
    if (resourceId == null) {
      return;
    }

    armManagers.postgreSqlManager().administrators().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "PostgresAdmin";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(POSTGRESQL_ADMIN_ID, String.class));
  }
}
