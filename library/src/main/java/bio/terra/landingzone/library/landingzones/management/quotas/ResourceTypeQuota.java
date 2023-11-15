package bio.terra.landingzone.library.landingzones.management.quotas;

import java.util.Map;

public record ResourceTypeQuota(
    String resourceType, String locationName, Map<String, Object> quota) {}
