package bio.terra.landingzone.stairway.flight.utils;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.loganalytics.models.DataExport;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.List;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtectedDataAzureStorageHelper {
  private static final Logger logger =
      LoggerFactory.getLogger(ProtectedDataAzureStorageHelper.class);

  private final ArmManagers armManagers;

  public ProtectedDataAzureStorageHelper(ArmManagers armManagers) {
    this.armManagers = armManagers;
  }

  public String getResourceGroupRegion(String resourceGroupName) {
    ResourceGroup resourceGroup =
        armManagers.azureResourceManager().resourceGroups().getByName(resourceGroupName);
    return resourceGroup.region().name();
  }

  public DataExport createLogAnalyticsDataExport(
      String exportName,
      String mrgName,
      String logAnalyticsWorkspaceResourceName,
      List<String> tableNames,
      String destinationStorageAccountResourceId) {
    return armManagers
        .logAnalyticsManager()
        .dataExports()
        .define(exportName)
        .withExistingWorkspace(mrgName, logAnalyticsWorkspaceResourceName)
        .withTableNames(tableNames)
        .withResourceId(destinationStorageAccountResourceId)
        .withEnable(true)
        .create();
  }

  public void deleteDataExport(String dataExportResourceId) {
    try {
      armManagers.logAnalyticsManager().dataExports().deleteById(dataExportResourceId);
    } catch (ManagementException e) {
      if (StringUtils.equals(e.getValue().getCode(), "ResourceNotFound")) {
        logger.error(
            "Log analytics data export doesn't exist or has been already deleted. Id={}",
            dataExportResourceId);
        return;
      }
      throw e;
    }
  }
}
