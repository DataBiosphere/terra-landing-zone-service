package bio.terra.landingzone.library.landingzones.management.quotas;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils;
import java.util.List;

public class QuotaProvider {
  private final ArmManagers armManagers;
  private final List<ResourceQuotaReader> resourceQuotaReaders;
  private final List<ResourceTypeQuotaReader> resourceTypeQuotaReaders;

  public QuotaProvider(ArmManagers armManagers) {
    this(
        armManagers,
        List.of(new BatchQuotaReader(armManagers.batchManager())),
        List.of(new BatchAccountQuotaReader(armManagers.batchManager())));
  }

  QuotaProvider(
      ArmManagers armManagers,
      List<ResourceQuotaReader> resourceQuotaReaders,
      List<ResourceTypeQuotaReader> resourceTypeQuotaReaders) {
    this.armManagers = armManagers;
    this.resourceQuotaReaders = resourceQuotaReaders;
    this.resourceTypeQuotaReaders = resourceTypeQuotaReaders;
  }

  public ResourceTypeQuota resourceTypeQuota(String resourceType, String locationName) {
    ResourceTypeQuotaReader reader =
        resourceTypeQuotaReaders.stream()
            .filter(r -> resourceType.equalsIgnoreCase(r.getResourceType()))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResourceTypeNotSupportedException(
                        "Quota information for the resource type is not supported."));

    return reader.getResourceTypeQuota(locationName);
  }

  public ResourceQuota resourceQuota(String resourceId) {
    String resourceType = AzureResourceTypeUtils.resourceTypeFromResourceId(resourceId);

    ResourceQuotaReader reader =
        resourceQuotaReaders.stream()
            .filter(r -> resourceType.equalsIgnoreCase(r.getResourceType()))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResourceTypeNotSupportedException(
                        "Quota information for the resource type is not supported."));

    return reader.getResourceQuota(resourceId);
  }
}
