package bio.terra.landingzone.library;

import bio.terra.landingzone.library.configuration.AzureCustomerUsageConfiguration;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.model.LandingZoneTarget;
import com.azure.core.credential.TokenCredential;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneManagerProvider {
  private AzureCustomerUsageConfiguration azureCustomerUsageConfiguration;
  private final AzureCredentialsProvider azureCredentialsProvider;
  private final LandingZoneAzureConfiguration azureConfiguration;

  @Autowired
  public LandingZoneManagerProvider(
      AzureCustomerUsageConfiguration azureCustomerUsageConfiguration,
      AzureCredentialsProvider azureCredentialsProvider,
      LandingZoneAzureConfiguration azureConfiguration) {
    this.azureCustomerUsageConfiguration = azureCustomerUsageConfiguration;
    this.azureCredentialsProvider = azureCredentialsProvider;
    this.azureConfiguration = azureConfiguration;
  }

  public LandingZoneManager createLandingZoneManager(LandingZoneTarget landingZoneTarget) {
    AzureProfile azureProfile = createAzureProfile(landingZoneTarget);
    return LandingZoneManager.createLandingZoneManager(
        buildTokenCredential(),
        azureProfile,
        landingZoneTarget.azureResourceGroupId(),
        azureCustomerUsageConfiguration.getUsageAttribute());
  }

  @NotNull
  public AzureProfile createAzureProfile(LandingZoneTarget landingZoneTarget) {
    return new AzureProfile(
        landingZoneTarget.azureTenantId(),
        landingZoneTarget.azureSubscriptionId(),
        azureConfiguration.getAzureEnvironment());
  }

  public AzureResourceManager createAzureResourceManagerClient(
      LandingZoneTarget landingZoneTarget) {
    AzureProfile azureProfile = createAzureProfile(landingZoneTarget);
    return AzureResourceManager.authenticate(buildTokenCredential(), azureProfile)
        .withSubscription(azureProfile.getSubscriptionId());
  }

  public TokenCredential buildTokenCredential() {
    return azureCredentialsProvider.getTokenCredential();
  }
}
