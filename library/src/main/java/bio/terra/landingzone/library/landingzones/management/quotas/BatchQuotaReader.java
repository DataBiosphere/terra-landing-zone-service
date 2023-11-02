package bio.terra.landingzone.library.landingzones.management.quotas;

import bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils;
import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.batch.models.VirtualMachineFamilyCoreQuota;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchQuotaReader implements ResourceQuotaReader {
  private final BatchManager batchManager;
  private static final ClientLogger logger = new ClientLogger(BatchQuotaReader.class);

  public BatchQuotaReader(BatchManager batchManager) {
    this.batchManager = batchManager;
  }

  @Override
  public ResourceQuota getResourceQuota(String resourceId) {
    logger.info("Getting quota information for batch resource:{}", resourceId);
    try {
      return toResourceQuota(batchManager.batchAccounts().getById(resourceId));
    } catch (Throwable ex) {
      logger.error("Failed to retrieve get quota information for the batch account.", ex);
      throw ex;
    }
  }

  @Override
  public String getResourceType() {
    return AzureResourceTypeUtils.AZURE_BATCH_TYPE;
  }

  private ResourceQuota toResourceQuota(BatchAccount batchAccount) {
    Map<String, Object> quotaInformation = new HashMap<>();
    quotaInformation.put(
        "activeJobAndJobScheduleQuota", batchAccount.activeJobAndJobScheduleQuota());
    quotaInformation.put("lowPriorityCoreQuota", batchAccount.lowPriorityCoreQuota());
    quotaInformation.put("dedicatedCoreQuota", batchAccount.dedicatedCoreQuota());
    quotaInformation.put(
        "dedicatedCoreQuotaPerVMFamilyEnforced",
        batchAccount.dedicatedCoreQuotaPerVMFamilyEnforced());
    quotaInformation.put(
        "dedicatedCoreQuotaPerVMFamily",
        batchAccount.dedicatedCoreQuotaPerVMFamily().stream()
            .collect(
                Collectors.toMap(
                    VirtualMachineFamilyCoreQuota::name,
                    VirtualMachineFamilyCoreQuota::coreQuota)));
    quotaInformation.put("poolQuota", batchAccount.poolQuota());

    return new ResourceQuota(batchAccount.id(), batchAccount.type(), quotaInformation);
  }
}
