package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
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
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.resourcemanager.resources.models.GenericResources;
import com.azure.resourcemanager.resources.models.TagOperations;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

  @Mock private TagOperations tagOperations;
  @Mock private ArmManagers armManagers;
  @Mock private AzureResourceManager azureResourceManager;
  @Mock private GenericResources genericResources;
  @Mock private GenericResource resource;
  @Mock private PagedIterable<GenericResource> genericResourcesPage;

  private final ArmResourceType armResourceType = ArmResourceType.STORAGE_ACCOUNT;
  private final UUID landingZoneId = UUID.randomUUID();

  @Mock private FlightContext context;
  private FlightMap workingFlightMap;

  @Test
  void doStep_resourceTypeIsFound_setsLzTagAndSucceeds() throws InterruptedException {
    setUpFlightWorkingMap();
    setUpMockFlightParameters();
    setUpMockResourceListing();
    setUpTagsCaptor();

    BaseReferencedResourceStep referencedResourceStep = createReferenceResourceStep(false);

    StepResult result = referencedResourceStep.doStep(context);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));

    // verify the tags captured contain the landing zone id
    Map<String, String> tags = tagsCaptor.getValue();
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()), equalTo(landingZoneId.toString()));
  }

  @Test
  void doStep_resourceTypeIsNotFound_stepFailsAndThrowsException() {
    setUpMockFlightParameters();

    // set up sets a storage account as the only resource, so looking for aks should fail
    BaseReferencedResourceStep referencedResourceStep =
        new ReferencedResourceStep(armManagers, Map.of(), ArmResourceType.AKS, false);

    assertThrows(RuntimeException.class, () -> referencedResourceStep.doStep(context));
  }

  @Test
  void doStep_resourceTypeIsFoundAndIsShared_succeedsSetsLzAndShareTags()
      throws InterruptedException {
    setUpFlightWorkingMap();
    setUpMockFlightParameters();
    setUpMockResourceListing();
    setUpTagsCaptor();

    BaseReferencedResourceStep referencedResourceStep = createReferenceResourceStep(true);

    StepResult result = referencedResourceStep.doStep(context);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));

    Map<String, String> tags = tagsCaptor.getValue();
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()), equalTo(landingZoneId.toString()));
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString()),
        equalTo(ResourcePurpose.SHARED_RESOURCE.name()));
  }

  @Test
  void doStep_resourceHasLandingZoneTag_throwException() {
    setUpFlightWorkingMap();
    setUpMockFlightParameters();
    setUpMockResourceListing();

    BaseReferencedResourceStep referencedResourceStep = createReferenceResourceStep(false);

    when(resource.tags())
        .thenReturn(
            Map.of(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), UUID.randomUUID().toString()));

    assertThrows(RuntimeException.class, () -> referencedResourceStep.doStep(context));
  }

  @Test
  void doStep_resourceHasExistingTags_tagsAreMerged() throws InterruptedException {
    setUpFlightWorkingMap();
    setUpMockFlightParameters();
    setUpMockResourceListing();

    BaseReferencedResourceStep referencedResourceStep = createReferenceResourceStep(false);

    Map<String, String> existingTags = Map.of("existingTag", "existingValue");
    when(resource.tags()).thenReturn(existingTags);

    setUpTagsCaptor();

    StepResult result = referencedResourceStep.doStep(context);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));

    Map<String, String> tags = tagsCaptor.getValue();
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()), equalTo(landingZoneId.toString()));
    assertThat(tags.get("existingTag"), equalTo("existingValue"));
  }

  @Test
  void doStep_resourceIsTagged_resourceIdIsSetInContext() throws InterruptedException {
    setUpFlightWorkingMap();
    setUpMockFlightParameters();
    setUpMockResourceListing();
    setUpTagsCaptor();

    BaseReferencedResourceStep referencedResourceStep = createReferenceResourceStep(false);

    when(resource.id()).thenReturn(RESOURCE_ID);

    StepResult result = referencedResourceStep.doStep(context);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        context
            .getWorkingMap()
            .get(BaseReferencedResourceStep.REFERENCED_RESOURCE_ID, String.class),
        equalTo(RESOURCE_ID));
  }

  @Test
  void doStep_resourceLzIdTagIsTheSameAsLzIdInContext_succeeds() throws InterruptedException {
    setUpFlightWorkingMap();
    setUpMockFlightParameters();
    setUpMockResourceListing();
    setUpTagsCaptor();

    BaseReferencedResourceStep referencedResourceStep = createReferenceResourceStep(false);

    when(resource.tags())
        .thenReturn(
            Map.of(
                LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                landingZoneId.toString(),
                LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                ResourcePurpose.SHARED_RESOURCE.name()));

    StepResult result = referencedResourceStep.doStep(context);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
  }

  @Test
  void doStep_resourceLzIdTagIsDifferentFromLzIdInContext_throwsException() {
    setUpFlightWorkingMap();
    setUpMockFlightParameters();
    setUpMockResourceListing();

    BaseReferencedResourceStep referencedResourceStep = createReferenceResourceStep(false);

    when(resource.tags())
        .thenReturn(
            Map.of(
                LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                UUID.randomUUID().toString(),
                LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                ResourcePurpose.SHARED_RESOURCE.name()));

    assertThrows(RuntimeException.class, () -> referencedResourceStep.doStep(context));
  }

  @Test
  void undoStep_resourceIdIsSetInContext_addedTagsAreRemoved() throws InterruptedException {
    setUpFlightWorkingMap();
    setUpMockFlightParameters();
    setUpMockGetResourceById();
    setUpTagsCaptor();

    BaseReferencedResourceStep referencedResourceStep =
        new ReferencedResourceStep(
            armManagers,
            Map.of(
                SubnetResourcePurpose.AKS_NODE_POOL_SUBNET.name(),
                SubnetResourcePurpose.AKS_NODE_POOL_SUBNET.name()),
            armResourceType,
            true);

    workingFlightMap.put(BaseReferencedResourceStep.REFERENCED_RESOURCE_ID, RESOURCE_ID);
    when(resource.tags())
        .thenReturn(
            Map.of(
                LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                landingZoneId.toString(),
                LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                ResourcePurpose.SHARED_RESOURCE.name(),
                "existingTag",
                "existingValue"));

    StepResult result = referencedResourceStep.undoStep(context);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        context
            .getWorkingMap()
            .get(BaseReferencedResourceStep.REFERENCED_RESOURCE_ID, String.class),
        equalTo(RESOURCE_ID));

    result = referencedResourceStep.undoStep(context);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    Map<String, String> tags = tagsCaptor.getValue();
    assertThat(tags.get("existingTag"), equalTo("existingValue"));
    assertThat(tags.keySet().size(), equalTo(1));
  }

  @Test
  void undoStep_resourceIdNotSetInContext_succeeds() throws InterruptedException {
    setUpFlightWorkingMap();

    BaseReferencedResourceStep referencedResourceStep = createReferenceResourceStep(false);

    StepResult result = referencedResourceStep.undoStep(context);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
  }

  private BaseReferencedResourceStep createReferenceResourceStep(boolean isShared) {
    return new ReferencedResourceStep(armManagers, Map.of(), armResourceType, isShared);
  }

  private void setUpTagsCaptor() {
    when(azureResourceManager.tagOperations()).thenReturn(tagOperations);
    when(tagOperations.updateTags(any(GenericResource.class), tagsCaptor.capture()))
        .thenReturn(null);
  }

  private void setUpFlightWorkingMap() {
    workingFlightMap = new FlightMap();
    when(context.getWorkingMap()).thenReturn(workingFlightMap);
    workingFlightMap.put(
        GetManagedResourceGroupInfo.TARGET_MRG_KEY,
        new TargetManagedResourceGroup("mrg", "armregion"));
  }

  private void setUpMockResourceListing() {
    when(armManagers.azureResourceManager()).thenReturn(azureResourceManager);
    when(azureResourceManager.genericResources()).thenReturn(genericResources);
    when(resource.type()).thenReturn("Microsoft.Storage/storageAccounts");
    var resources = List.of(resource);
    when(genericResourcesPage.iterator()).thenReturn(resources.iterator());
    when(genericResources.listByResourceGroup("mrg")).thenReturn(genericResourcesPage);
  }

  private void setUpMockGetResourceById() {
    when(armManagers.azureResourceManager()).thenReturn(azureResourceManager);
    when(azureResourceManager.genericResources()).thenReturn(genericResources);
    when(genericResources.getById(RESOURCE_ID)).thenReturn(resource);
  }

  private void setUpMockFlightParameters() {
    FlightMap parameters = new FlightMap();
    LandingZoneRequest request =
        new LandingZoneRequest(
            "definition", "version", Map.of(), UUID.randomUUID(), Optional.of(landingZoneId));
    when(context.getInputParameters()).thenReturn(parameters);
    parameters.put(LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, request);
    parameters.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneId);
  }

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
