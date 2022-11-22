package bio.terra.landingzone.library.landingzones.management.quotas;

public interface ResourceQuotaReader {
  ResourceQuota getResourceQuota(String resourceId);

  String getResourceType();
}
