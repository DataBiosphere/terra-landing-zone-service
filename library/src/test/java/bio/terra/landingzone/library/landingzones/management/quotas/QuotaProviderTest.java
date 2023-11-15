package bio.terra.landingzone.library.landingzones.management.quotas;

import static bio.terra.landingzone.library.landingzones.TestUtils.STUB_AKS_ID;
import static bio.terra.landingzone.library.landingzones.TestUtils.STUB_BATCH_ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class QuotaProviderTest {
  private QuotaProvider quotaProvider;

  @Mock private ArmManagers armManagers;

  @Mock private ResourceQuotaReader resourceQuotaReader;
  @Mock private ResourceTypeQuotaReader resourceTypeQuotaReader;

  @BeforeEach
  void setUp() {
    quotaProvider =
        new QuotaProvider(
            armManagers, List.of(resourceQuotaReader), List.of(resourceTypeQuotaReader));
  }

  @Test
  void resourceQuota_readerAvailableForType_quotaReaderIsCalled() {
    when(resourceQuotaReader.getResourceType()).thenReturn(AzureResourceTypeUtils.AZURE_BATCH_TYPE);

    quotaProvider.resourceQuota(STUB_BATCH_ACCOUNT_ID);

    verify(resourceQuotaReader, times(1)).getResourceQuota(STUB_BATCH_ACCOUNT_ID);
  }

  @Test
  void resourceQuota_readerNotAvailableForType_throwsException() {
    when(resourceQuotaReader.getResourceType()).thenReturn(AzureResourceTypeUtils.AZURE_BATCH_TYPE);

    assertThrows(
        ResourceTypeNotSupportedException.class, () -> quotaProvider.resourceQuota(STUB_AKS_ID));
  }
}
