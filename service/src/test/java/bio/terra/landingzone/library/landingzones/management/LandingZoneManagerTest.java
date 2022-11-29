package bio.terra.landingzone.library.landingzones.management;

import static bio.terra.landingzone.library.landingzones.TestUtils.STUB_BATCH_ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionProvider;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployments;
import bio.terra.landingzone.library.landingzones.management.quotas.QuotaProvider;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LandingZoneManagerTest {

  private static final String STUB_LZ_ID = UUID.randomUUID().toString();
  private LandingZoneManager landingZoneManager;

  @Mock private LandingZoneDefinitionProvider landingZoneDefinitionProvider;
  @Mock private LandingZoneDeployments landingZoneDeployments;
  @Mock private AzureResourceManager resourceManager;
  @Mock private ResourceGroup resourceGroup;
  @Mock private ResourcesReader resourceReader;
  @Mock private QuotaProvider quotaProvider;
  @Mock private ResourcesDeleteManager deleteManager;

  @BeforeEach
  void setUp() {
    landingZoneManager =
        new LandingZoneManager(
            landingZoneDefinitionProvider,
            landingZoneDeployments,
            resourceManager,
            resourceGroup,
            resourceReader,
            quotaProvider,
            deleteManager);
  }

  @Test
  void resourceQuota_landingZoneContainsResource_quotaProviderIsCalled() {
    when(resourceReader.listAllResources(STUB_LZ_ID))
        .thenReturn(
            List.of(
                new DeployedResource(
                    STUB_BATCH_ACCOUNT_ID,
                    AzureResourceTypeUtils.AZURE_BATCH_TYPE,
                    null,
                    "eastus")));

    landingZoneManager.resourceQuota(STUB_LZ_ID, STUB_BATCH_ACCOUNT_ID);

    verify(quotaProvider, times(1)).resourceQuota(STUB_BATCH_ACCOUNT_ID);
  }

  @Test
  void resourceQuota_landingZoneNotContainsResource_throwsException() {
    assertThrows(
        IllegalStateException.class,
        () -> landingZoneManager.resourceQuota(STUB_LZ_ID, STUB_BATCH_ACCOUNT_ID));
  }
}
