package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.exception.ManagementException;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects the Log Analytics Workspace in a protected data landing zone to an external
 * administrative storage account for long term storage of logs.
 */
public class ConnectLongTermLogStorageStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(ConnectLongTermLogStorageStep.class);
  private static final int MAX_DATA_EXPORT_NAME_LENGTH = 63;

  private final List<String> tableNames;

  public ConnectLongTermLogStorageStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator,
      List<String> tableNames) {
    super(armManagers, parametersResolver, resourceNameGenerator);
    this.tableNames = tableNames;

    if (tableNames.isEmpty()) {
      throw new LandingZoneCreateException(
          "Long term storage export must have at least one table name for export");
    }
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var logAnalyticsWorkspace =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_RESOURCE_KEY,
            LandingZoneResource.class);

    var logAnalyticsWorkspaceResourceName = logAnalyticsWorkspace.resourceName();
    if (logAnalyticsWorkspaceResourceName.isEmpty()) {
      throw new MissingRequiredFieldsException("LogAnalyticsWorkspace resource name is not set.");
    }

    var destinationStorageAccountResourceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            LandingZoneFlightMapKeys.PROTECTED_DATA_LTS_STORAGE_ACCT_ID,
            String.class);

    var exportName = resourceNameGenerator.nextName(MAX_DATA_EXPORT_NAME_LENGTH);
    armManagers
        .logAnalyticsManager()
        .dataExports()
        .define(exportName)
        .withExistingWorkspace(getMRGName(context), logAnalyticsWorkspaceResourceName.get())
        .withTableNames(tableNames)
        .withResourceId(destinationStorageAccountResourceId)
        .withEnable(true)
        .create();
  }

  @Override
  protected void deleteResource(String resourceId) {
    try {
      armManagers.logAnalyticsManager().dataExports().deleteById(resourceId);
    } catch (ManagementException e) {
      if (StringUtils.equals(e.getValue().getCode(), "ResourceNotFound")) {
        logger.error(
            "Log analytics data export doesn't exist or has been already deleted. Id={}",
            resourceId);
        return;
      }
      throw e;
    }
  }

  @Override
  protected String getResourceType() {
    return "LongTermLogStorage";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.empty();
  }
}
