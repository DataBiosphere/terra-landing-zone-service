package bio.terra.landingzone.library.landingzones.deployment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.azure.resourcemanager.resources.fluentcore.model.Creatable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ResourcesTagMapWrapperTest {

  private ResourcesTagMapWrapper resourcesTagMapWrapper;
  private String landingZoneId;

  private MockResource resource;

  private MockResource secondResource;

  @BeforeEach
  void setUp() {
    resource = mock(MockResource.class);
    secondResource = mock(MockResource.class);
    landingZoneId = UUID.randomUUID().toString();
    resourcesTagMapWrapper = new ResourcesTagMapWrapper(landingZoneId);
  }

  @Test
  void putPurpose_mapContainsResourceWithoutTags() {
    resourcesTagMapWrapper.putResource(resource);

    assertThat(resourcesTagMapWrapper.getResourcesTagsMap().containsKey(resource), equalTo(true));
    assertThat(resourcesTagMapWrapper.getResourcesTagsMap().size(), equalTo(1));
    assertThat(resourcesTagMapWrapper.getResourcesTagsMap().get(resource), equalTo(null));
  }

  @Test
  void putWithLandingZone_mapContainsLandingZoneTag() {
    resourcesTagMapWrapper.putWithLandingZone(resource);

    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId);

    assertThat(
        resourcesTagMapWrapper.getResourceTagsMap(resource).entrySet(),
        equalTo(expectedMap.entrySet()));
  }

  @Test
  void putWithPurpose_mapContainsPurposeAndLZIdTags() {
    resourcesTagMapWrapper.putWithPurpose(resource, ResourcePurpose.SHARED_RESOURCE);

    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        ResourcePurpose.SHARED_RESOURCE.toString());
    expectedMap.put(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId);

    assertThat(
        resourcesTagMapWrapper.getResourceTagsMap(resource).entrySet(),
        equalTo(expectedMap.entrySet()));
  }

  @Test
  void putWithPurpose_multipleResourcesMapContainsPurposeAndLZIdTags() {
    resourcesTagMapWrapper.putWithPurpose(resource, ResourcePurpose.SHARED_RESOURCE);
    resourcesTagMapWrapper.putWithPurpose(secondResource, ResourcePurpose.SHARED_RESOURCE);

    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        ResourcePurpose.SHARED_RESOURCE.toString());
    expectedMap.put(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId);

    assertThat(
        resourcesTagMapWrapper.getResourceTagsMap(resource).entrySet(),
        equalTo(expectedMap.entrySet()));
    assertThat(
        resourcesTagMapWrapper.getResourceTagsMap(secondResource).entrySet(),
        equalTo(expectedMap.entrySet()));
  }

  @Test
  void putWithPurpose_afterPutWithLandingZoneContainsOneEntryForLZId() {
    resourcesTagMapWrapper.putWithLandingZone(resource);
    resourcesTagMapWrapper.putWithPurpose(resource, ResourcePurpose.SHARED_RESOURCE);

    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        ResourcePurpose.SHARED_RESOURCE.toString());
    expectedMap.put(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId);

    assertThat(
        resourcesTagMapWrapper.getResourceTagsMap(resource).entrySet(),
        equalTo(expectedMap.entrySet()));
  }

  interface MockResource extends Creatable, Resource.DefinitionWithTags {}
}
