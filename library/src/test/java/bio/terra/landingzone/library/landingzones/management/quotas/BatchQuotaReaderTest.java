package bio.terra.landingzone.library.landingzones.management.quotas;

import static bio.terra.landingzone.library.landingzones.TestUtils.STUB_BATCH_ACCOUNT_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils;
import com.azure.json.JsonOptions;
import com.azure.json.implementation.DefaultJsonProvider;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.batch.models.BatchAccounts;
import com.azure.resourcemanager.batch.models.VirtualMachineFamilyCoreQuota;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BatchQuotaReaderTest {

  private static final int ACTIVE_JOB_SCHEDULE_QUOTA = 10;
  private static final Integer DEDICATED_CORE_QUOTA = 10;

  private static final Integer LOW_PRI_CORE_QUOTA = 5;
  private final Integer POOL_QUOTA = 5;

  @Mock private BatchManager batchManager;

  @Mock private BatchAccount batchAccount;

  private VirtualMachineFamilyCoreQuota vmQuota;

  @Mock private BatchAccounts batchAccounts;

  BatchQuotaReader batchQuotaReader;

  @BeforeEach
  void setUp() {
    batchQuotaReader = new BatchQuotaReader(batchManager);
  }

  private void setUpBatchManager() {
    when(batchManager.batchAccounts()).thenReturn(batchAccounts);
    when(batchAccounts.getById(STUB_BATCH_ACCOUNT_ID)).thenReturn(batchAccount);
    when(batchAccount.activeJobAndJobScheduleQuota()).thenReturn(ACTIVE_JOB_SCHEDULE_QUOTA);
    when(batchAccount.dedicatedCoreQuota()).thenReturn(DEDICATED_CORE_QUOTA);
    when(batchAccount.poolQuota()).thenReturn(POOL_QUOTA);
    when(batchAccount.id()).thenReturn(STUB_BATCH_ACCOUNT_ID);
    when(batchAccount.lowPriorityCoreQuota()).thenReturn(LOW_PRI_CORE_QUOTA);
  }

  private void setUpDedicatedCoreQuotaPerFamily() throws IOException {
    vmQuota = createVmFamilyCoreQuota();
    List<VirtualMachineFamilyCoreQuota> quotaPerVMFamily = List.of(vmQuota);
    when(batchAccount.dedicatedCoreQuotaPerVMFamily()).thenReturn(quotaPerVMFamily);
    when(batchAccount.dedicatedCoreQuotaPerVMFamilyEnforced()).thenReturn(true);
  }

  private VirtualMachineFamilyCoreQuota createVmFamilyCoreQuota() throws IOException {
    var familyCoreQuotaJson = "{\"name\":\"vmsku\",\"coreQuota\":10}";

    return VirtualMachineFamilyCoreQuota.fromJson(
        new DefaultJsonProvider().createReader(familyCoreQuotaJson, new JsonOptions()));
  }

  @Test
  void getResourceQuota_returnsValidQuotaForBatchAccount() throws IOException {

    setUpBatchManager();
    setUpDedicatedCoreQuotaPerFamily();

    ResourceQuota quota = batchQuotaReader.getResourceQuota(STUB_BATCH_ACCOUNT_ID);

    assertThat(quota.resourceId(), equalTo(STUB_BATCH_ACCOUNT_ID));
    assertThat(quota.quota(), hasEntry("activeJobAndJobScheduleQuota", ACTIVE_JOB_SCHEDULE_QUOTA));
    assertThat(quota.quota(), hasEntry("dedicatedCoreQuota", DEDICATED_CORE_QUOTA));
    assertThat(quota.quota(), hasEntry("lowPriorityCoreQuota", LOW_PRI_CORE_QUOTA));
    assertThat(quota.quota(), hasEntry("dedicatedCoreQuotaPerVMFamilyEnforced", true));
    assertThat(quota.quota(), hasEntry("poolQuota", POOL_QUOTA));
    Map<String, Object> quotaPerVmFamily =
        (Map<String, Object>) quota.quota().get("dedicatedCoreQuotaPerVMFamily");
    assertThat(quotaPerVmFamily, hasEntry("vmsku", 10));
  }

  @Test
  void getResourceQuota_noDedicatedCoreQuotaPerFamily() throws JsonProcessingException {

    setUpBatchManager();

    ResourceQuota quota = batchQuotaReader.getResourceQuota(STUB_BATCH_ACCOUNT_ID);

    assertThat(quota.resourceId(), equalTo(STUB_BATCH_ACCOUNT_ID));
    assertThat(quota.quota(), hasEntry("activeJobAndJobScheduleQuota", ACTIVE_JOB_SCHEDULE_QUOTA));
    assertThat(quota.quota(), hasEntry("dedicatedCoreQuota", DEDICATED_CORE_QUOTA));
    assertThat(quota.quota(), hasEntry("poolQuota", POOL_QUOTA));
  }

  @Test
  void getResourceType_returnsBatchType() {
    String type = batchQuotaReader.getResourceType();

    assertThat(type, equalTo(AzureResourceTypeUtils.AZURE_BATCH_TYPE));
  }
}
