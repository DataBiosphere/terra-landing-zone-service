package bio.terra.landingzone.model;

public record LandingZoneTarget(
    String azureTenantId, String azureSubscriptionId, String azureResourceGroupId) {}
