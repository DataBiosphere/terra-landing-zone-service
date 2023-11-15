package bio.terra.landingzone.library.landingzones.management.quotas;

public interface ResourceTypeQuotaReader {

  ResourceTypeQuota getResourceTypeQuota(String locationName);

  String getResourceType();
}
