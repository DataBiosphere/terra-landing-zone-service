package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresql.models.PublicNetworkAccessEnum;
import com.azure.resourcemanager.postgresql.models.ServerPropertiesForDefaultCreate;
import com.azure.resourcemanager.postgresql.models.ServerVersion;
import com.azure.resourcemanager.postgresql.models.Sku;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePostgresqlDbStep extends BaseResourceCreateStep {
  public static final String POSTGRESQL_ID = "POSTGRESQL_ID";
  private static final Logger logger = LoggerFactory.getLogger(CreatePostgresqlDbStep.class);

  public CreatePostgresqlDbStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);
    // TODO: check if we can arrange all these dependencies in a different way
    // Most like we need the same setup for different steps. At least we need armManagers.
    var postgresName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_POSTGRESQL_SERVER_NAME_LENGTH);
    try {
      var postgres =
          armManagers
              .postgreSqlManager()
              .servers()
              .define(postgresName)
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup.name())
              .withProperties(
                  new ServerPropertiesForDefaultCreate()
                      .withAdministratorLogin(
                          parametersResolver.getValue(
                              CromwellBaseResourcesFactory.ParametersNames.POSTGRES_DB_ADMIN
                                  .name()))
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
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(RESOURCE_ALREADY_EXISTS, "Postgres", postgresName, resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "postgres", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    var postgresId = context.getWorkingMap().get(POSTGRESQL_ID, String.class);
    try {
      armManagers.postgreSqlManager().servers().deleteById(postgresId);
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        return StepResult.getStepResultSuccess();
      }
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }
}
