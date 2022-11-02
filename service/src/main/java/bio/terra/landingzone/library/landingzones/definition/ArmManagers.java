package bio.terra.landingzone.library.landingzones.definition;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.azure.resourcemanager.monitor.MonitorManager;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.relay.RelayManager;

/** Record with the ARM clients required for deployments */
public record ArmManagers(
    AzureResourceManager azureResourceManager,
    RelayManager relayManager,
    BatchManager batchManager,
    PostgreSqlManager postgreSqlManager,
    LogAnalyticsManager logAnalyticsManager,
    MonitorManager monitorManager) {}
