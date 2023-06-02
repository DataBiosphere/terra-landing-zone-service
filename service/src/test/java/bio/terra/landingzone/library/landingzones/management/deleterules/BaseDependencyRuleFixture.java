package bio.terra.landingzone.library.landingzones.management.deleterules;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.ResourceToDelete;
import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.applicationinsights.ApplicationInsightsManager;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.azure.resourcemanager.monitor.MonitorManager;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.relay.RelayManager;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.resourcemanager.securityinsights.SecurityInsightsManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class BaseDependencyRuleFixture {
  protected ArmManagers armManagers;

  protected final String RESOURCE_NAME = "resourceName";
  protected final String RESOURCE_GROUP = "resourceGroup";

  @Mock protected BatchManager batchManager;
  @Mock protected AzureResourceManager azureResourceManager;
  @Mock protected PostgreSqlManager postgreSqlManager;
  @Mock protected RelayManager relayManager;
  @Mock protected LogAnalyticsManager logAnalyticsManager;
  @Mock protected MonitorManager monitorManager;
  @Mock protected ApplicationInsightsManager applicationInsightsManager;
  @Mock protected SecurityInsightsManager securityInsightsManager;

  @Mock protected ResourceToDelete resourceToDelete;
  @Mock protected GenericResource resource;

  @BeforeEach
  void setUpArmManager() {
    armManagers =
        new ArmManagers(
            azureResourceManager,
            relayManager,
            batchManager,
            postgreSqlManager,
            logAnalyticsManager,
            monitorManager,
            applicationInsightsManager,
            securityInsightsManager);
  }

  protected void setUpResourceToDelete() {
    when(resource.name()).thenReturn(RESOURCE_NAME);
    when(resource.resourceGroupName()).thenReturn(RESOURCE_GROUP);
    when(resourceToDelete.resource()).thenReturn(resource);
  }

  protected <T> PagedIterable<T> toMockPageIterable(List<T> items) {
    var pageIterable = mock(PagedIterable.class);
    when(pageIterable.stream()).thenReturn(items.stream());
    return pageIterable;
  }
}
