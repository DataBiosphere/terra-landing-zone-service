package bio.terra.landingzone.stairway.flight.create;

import static bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys.RESOURCE_GROUP_TAGS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ReadResourceGroupTagsStepTest {

  private static final String RESOURCE_GROUP_NAME = "myRg";
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
  private ReadResourceGroupTagsStep resourceGroupTagsStep;
  @Mock private AzureResourceManager azureResourceManager;
  @Mock private ResourceGroup resourceGroup;
  @Mock private FlightContext flightContext;
  @Mock private ResourceGroups resourceGroups;
  @Mock private FlightMap flightMap;

  @Mock private LandingZoneManagerProvider landingZoneManagerProvider;

  private ProfileModel billingProfile;

  @BeforeEach
  void setUp() {
    resourceGroupTagsStep =
        new ReadResourceGroupTagsStep(landingZoneManagerProvider, new ObjectMapper());
    billingProfile =
        new ProfileModel()
            .managedResourceGroupId(RESOURCE_GROUP_NAME)
            .tenantId(TENANT_ID)
            .subscriptionId(SUBSCRIPTION_ID);

    when(landingZoneManagerProvider.createAzureResourceManagerClient(any()))
        .thenReturn(azureResourceManager);
    when(azureResourceManager.resourceGroups()).thenReturn(resourceGroups);
    when(resourceGroups.getByName(RESOURCE_GROUP_NAME)).thenReturn(resourceGroup);
    when(flightContext.getWorkingMap()).thenReturn(flightMap);
    when(flightMap.get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class))
        .thenReturn(billingProfile);
    when(flightMap.getRaw(LandingZoneFlightMapKeys.BILLING_PROFILE))
        .thenReturn(billingProfile.toString());
  }

  @Test
  void doStep_resourceGroupWithTags() throws InterruptedException {
    Map<String, String> tags = new HashMap<>();
    tags.put("mytag", "myvalue");
    when(resourceGroup.tags()).thenReturn(tags);
    resourceGroupTagsStep.doStep(flightContext);

    verify(flightMap, times(1)).put(eq(RESOURCE_GROUP_TAGS), any());
  }

  @Test
  void doStep_resourceGroupWithNoTags() throws InterruptedException {
    Map<String, String> tags = new HashMap<>();
    when(resourceGroup.tags()).thenReturn(tags);
    resourceGroupTagsStep.doStep(flightContext);

    verify(flightMap, times(0)).put(eq(RESOURCE_GROUP_TAGS), any());
  }
}
