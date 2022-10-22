package bio.terra.landingzone.stairway.flight.create;

import static bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys.RESOURCE_GROUP_TAGS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.azure.resourcemanager.resources.models.TagOperations;
import com.fasterxml.jackson.core.JsonProcessingException;
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

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class SetResourceGroupTagsStepTest {

  private static final String RESOURCE_GROUP_NAME = "myRg";
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
  private SetResourceGroupTagsStep setResourceGroupTagsStep;

  @Mock private LandingZoneManagerProvider landingZoneManagerProvider;
  private ProfileModel billingProfile;
  @Mock private AzureResourceManager azureResourceManager;

  @Mock private ResourceGroups resourceGroups;

  @Mock private FlightContext flightContext;

  @Mock private ResourceGroup resourceGroup;

  @Mock private FlightMap flightMap;

  private ObjectMapper objectMapper;
  @Mock private TagOperations tagOperations;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    setResourceGroupTagsStep =
        new SetResourceGroupTagsStep(landingZoneManagerProvider, objectMapper);
    billingProfile =
        new ProfileModel()
            .managedResourceGroupId(RESOURCE_GROUP_NAME)
            .tenantId(TENANT_ID)
            .subscriptionId(SUBSCRIPTION_ID);

    when(landingZoneManagerProvider.createAzureResourceManagerClient(any()))
        .thenReturn(azureResourceManager);
    when(flightContext.getWorkingMap()).thenReturn(flightMap);
    when(flightMap.get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class))
        .thenReturn(billingProfile);
    when(flightMap.getRaw(LandingZoneFlightMapKeys.BILLING_PROFILE))
        .thenReturn(billingProfile.toString());
  }

  @Test
  void doStep_preExistingTags() throws JsonProcessingException, InterruptedException {
    Map<String, String> tags = new HashMap<>();
    tags.put("MyTag", "Value");

    when(azureResourceManager.resourceGroups()).thenReturn(resourceGroups);
    when(resourceGroups.getByName(RESOURCE_GROUP_NAME)).thenReturn(resourceGroup);
    when(flightMap.get(RESOURCE_GROUP_TAGS, String.class))
        .thenReturn(objectMapper.writeValueAsString(tags));
    when(azureResourceManager.tagOperations()).thenReturn(tagOperations);

    setResourceGroupTagsStep.doStep(flightContext);

    verify(tagOperations, times(1)).updateTags(resourceGroup, tags);
  }

  @Test
  void doStep_preExistingTagsAndNewTags() throws JsonProcessingException, InterruptedException {
    Map<String, String> preExisting = new HashMap<>();
    preExisting.put("MyTag", "Value");

    when(azureResourceManager.resourceGroups()).thenReturn(resourceGroups);
    when(resourceGroups.getByName(RESOURCE_GROUP_NAME)).thenReturn(resourceGroup);
    when(flightMap.get(RESOURCE_GROUP_TAGS, String.class))
        .thenReturn(objectMapper.writeValueAsString(preExisting));
    when(azureResourceManager.tagOperations()).thenReturn(tagOperations);

    Map<String, String> newTags = new HashMap<>();
    newTags.put("MyNewTag", "Value");
    when(resourceGroup.tags()).thenReturn(newTags);

    setResourceGroupTagsStep.doStep(flightContext);

    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.putAll(preExisting);
    expectedMap.putAll(newTags);

    verify(tagOperations, times(1)).updateTags(resourceGroup, expectedMap);
  }

  @Test
  void doStep_withEmptyPreExistingTags() throws JsonProcessingException, InterruptedException {
    Map<String, String> tags = new HashMap<>();

    when(flightMap.get(RESOURCE_GROUP_TAGS, String.class))
        .thenReturn(objectMapper.writeValueAsString(tags));

    setResourceGroupTagsStep.doStep(flightContext);

    verify(tagOperations, times(0)).updateTags(any(Resource.class), any());
  }

  @Test
  void doStep_withoutPreExistingTagsInWorkingMap() throws InterruptedException {

    setResourceGroupTagsStep.doStep(flightContext);

    verify(tagOperations, times(0)).updateTags(any(Resource.class), any());
  }
}
