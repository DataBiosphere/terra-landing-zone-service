package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetManagedResourceGroupInfo;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.monitor.models.TagsResource;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.resourcemanager.resources.models.GenericResources;
import com.azure.resourcemanager.resources.models.TagOperations;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class BaseReferencedResourceStepTest {

  private static final String RESOURCE_ID = "resourceId";

  @Captor private ArgumentCaptor<Map<String, String>> tagsCaptor;

  private TagsResource tagsResource;
  @Mock private TagOperations tagOperations;
  @Mock private ArmManagers armManagers;
  @Mock private AzureResourceManager azureResourceManager;
  @Mock private GenericResources genericResources;
  @Mock private GenericResource resource;
  @Mock private PagedIterable<GenericResource> genericResourcesPage;

  private Map<String, String> tags;
  private ArmResourceType armResourceType;
  private final UUID landingZoneId = UUID.randomUUID();

  @Mock private FlightContext context;

  @BeforeEach
  void setUp() {
    armResourceType = ArmResourceType.STORAGE_ACCOUNT;
    tags = Map.of();
    setUpMocks();
  }

  @Test
  void doStep_resourceTypeIsFound_setsLzTagAndSucceeds() throws InterruptedException {
    BaseReferencedResourceStep referencedResourceStep =
        new ReferencedResourceStep(armManagers, tags, armResourceType, false);

    StepResult result = referencedResourceStep.doStep(context);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));

    // verify the tags captured contain the landing zone id
    Map<String, String> tags = tagsCaptor.getValue();
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()), equalTo(landingZoneId.toString()));
  }

  protected void verifyBasicTags(Map<String, String> tags, UUID landingZoneId) {
    // assertNotNull(tags);
    // assertTrue(tags.containsKey(LandingZoneTagKeys.LANDING_ZONE_ID.toString()));
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()), equalTo(landingZoneId.toString()));
    // assertTrue(tags.containsKey(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString()));
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString()),
        equalTo(ResourcePurpose.SHARED_RESOURCE.toString()));
  }

  private void setUpMocks() {
    FlightMap flightMap = new FlightMap();
    when(context.getWorkingMap()).thenReturn(flightMap);
    flightMap.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneId);
    flightMap.put(
        GetManagedResourceGroupInfo.TARGET_MRG_KEY,
        new TargetManagedResourceGroup("mrg", "armregion"));

    FlightMap parameters = new FlightMap();
    LandingZoneRequest request =
        new LandingZoneRequest(
            "definition", "version", Map.of(), UUID.randomUUID(), Optional.of(landingZoneId));
    when(context.getInputParameters()).thenReturn(parameters);
    parameters.put(LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, request);

    when(armManagers.azureResourceManager()).thenReturn(azureResourceManager);
    when(azureResourceManager.genericResources()).thenReturn(genericResources);
    when(resource.resourceType()).thenReturn("storageAccounts");
    when(resource.resourceProviderNamespace()).thenReturn("Microsoft.Storage");
    var resources = List.of(resource);
    when(genericResourcesPage.iterator()).thenReturn(resources.iterator());
    when(genericResources.listByResourceGroup("mrg")).thenReturn(genericResourcesPage);
    when(azureResourceManager.tagOperations()).thenReturn(tagOperations);
    when(tagOperations.updateTags(any(GenericResource.class), tagsCaptor.capture()))
        .thenReturn(null);
  }

  @Test
  void undoStep() {}

  // Test implementation of BaseReferencedResourceStep
  private static class ReferencedResourceStep extends BaseReferencedResourceStep {

    private final Map<String, String> tags;
    private final ArmResourceType armResourceType;
    private final boolean isShared;

    public ReferencedResourceStep(
        ArmManagers armManagers,
        Map<String, String> tags,
        ArmResourceType armResourceType,
        boolean isShared) {
      super(armManagers);
      this.tags = tags;
      this.armResourceType = armResourceType;
      this.isShared = isShared;
    }

    @Override
    protected boolean isSharedResource() {
      return isShared;
    }

    @Override
    protected Map<String, String> getLandingZoneResourceTags(
        FlightContext context, String resourceId) {
      return tags;
    }

    @Override
    protected ArmResourceType getArmResourceType() {
      return armResourceType;
    }
  }
}
