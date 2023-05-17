package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.landingzone.stairway.flight.utils.ProtectedDataAzureStorageHelper;
import bio.terra.stairway.FlightContext;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects the Log Analytics Workspace in a protected data landing zone to an external
 * administrative storage account for long term storage of logs.
 */
public class ConnectLongTermLogStorageStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(ConnectLongTermLogStorageStep.class);
  private static final int MAX_DATA_EXPORT_NAME_LENGTH = 63;
  public static final String DATA_EXPORT_ID = "DATA_EXPORT_ID";
  public static final String DATA_EXPORT_RESOURCE_KEY = "DATA_EXPORT";

  private final List<String> tableNames;
  private final ProtectedDataAzureStorageHelper azureStorageHelper;

  public ConnectLongTermLogStorageStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator,
      List<String> tableNames,
      ProtectedDataAzureStorageHelper azureStorageHelper) {
    super(armManagers, parametersResolver, resourceNameGenerator);
    this.tableNames = tableNames;
    this.azureStorageHelper = azureStorageHelper;

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
    var result =
        azureStorageHelper.createLogAnalyticsDataExport(
            exportName,
            getMRGName(context),
            logAnalyticsWorkspaceResourceName.get(),
            tableNames,
            destinationStorageAccountResourceId);

    context.getWorkingMap().put(DATA_EXPORT_ID, result.id());
    context
        .getWorkingMap()
        .put(
            DATA_EXPORT_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(result.id())
                .resourceType(result.type())
                .resourceName(result.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), result.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    azureStorageHelper.deleteDataExport(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "LongTermLogStorage";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(DATA_EXPORT_ID, String.class));
  }
}
