package bio.terra.landingzone.library.landingzones.management.quotas;

import java.util.Map;

public record ResourceQuota(String resourceId, String resourceType, Map<String, Object> quota) {}
