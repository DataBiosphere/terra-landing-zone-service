package bio.terra.landingzone.library.landingzones.management.quotas;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils;
import java.util.List;

public class QuotaProvider {
  private final ArmManagers armManagers;
  private final List<ResourceQuotaReader> quotaReaders;

  public QuotaProvider(ArmManagers armManagers) {
    this(armManagers, List.of(new BatchQuotaReader(armManagers.batchManager())));
  }

  QuotaProvider(ArmManagers armManagers, List<ResourceQuotaReader> quotaReaders) {
    this.armManagers = armManagers;
    this.quotaReaders = quotaReaders;
  }

  public ResourceQuota resourceQuota(String resourceId) {
    String resourceType = AzureResourceTypeUtils.resourceTypeFromResourceId(resourceId);

    ResourceQuotaReader reader =
        quotaReaders.stream()
            .filter(r -> resourceType.equalsIgnoreCase(r.getResourceType()))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResourceTypeNotSupportedException(
                        "Quota information for the resource type is not supported."));

    return reader.getResourceQuota(resourceId);
  }
}
