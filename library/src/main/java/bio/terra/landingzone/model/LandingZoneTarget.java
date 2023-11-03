package bio.terra.landingzone.model;

import bio.terra.profile.model.ProfileModel;

public record LandingZoneTarget(
    String azureTenantId, String azureSubscriptionId, String azureResourceGroupId) {

  public static LandingZoneTarget fromBillingProfile(ProfileModel profile) {
    return new LandingZoneTarget(
        profile.getTenantId().toString(),
        profile.getSubscriptionId().toString(),
        profile.getManagedResourceGroupId());
  }
}
