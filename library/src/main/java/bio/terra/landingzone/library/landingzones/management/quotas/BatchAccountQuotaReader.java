package bio.terra.landingzone.library.landingzones.management.quotas;

import bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils;
import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.batch.models.BatchLocationQuota;
import java.util.Map;

public class BatchAccountQuotaReader implements ResourceTypeQuotaReader {

  private static final ClientLogger logger = new ClientLogger(BatchAccountQuotaReader.class);
  private final BatchManager batchManager;

  public BatchAccountQuotaReader(BatchManager batchManager) {
    this.batchManager = batchManager;
  }

  @Override
  public ResourceTypeQuota getResourceTypeQuota(String locationName) {
    logger.info("Getting quota information for batch accounts for location {}", locationName);

    // TODO: batchManager.locations().getQuotas only gives back the total number of accounts,
    // it does not expose the current consumption against the quota. So, I'm listing the
    // batch accounts here and using that as a proxy.
    // Is this something we can reliably do given our limited access to subscription data?
    var existingAccounts = batchManager.batchAccounts().list().stream().toList().size();

    return toResourceTypeQuota(
        batchManager.locations().getQuotas(locationName), existingAccounts, locationName);
  }

  @Override
  public String getResourceType() {
    return AzureResourceTypeUtils.AZURE_BATCH_TYPE;
  }

  private ResourceTypeQuota toResourceTypeQuota(
      BatchLocationQuota quotas, int existingAccounts, String locationName) {
    return new ResourceTypeQuota(
        "batchAccounts",
        locationName,
        Map.of("totalBatchAccounts", quotas.accountQuota(), "existingAccounts", existingAccounts));
  }
}
