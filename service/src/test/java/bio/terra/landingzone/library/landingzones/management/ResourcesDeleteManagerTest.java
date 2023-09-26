package bio.terra.landingzone.library.landingzones.management;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.PrivateEndpoint;
import com.azure.resourcemanager.network.models.PrivateEndpoints;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.resourcemanager.resources.models.GenericResources;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ResourcesDeleteManagerTest {
  private static final String LANDING_ZONE_ID = UUID.randomUUID().toString();
  private static final String RESOURCE_GROUP_NAME = "RESOURCE_GROUP";

  @Mock private ArmManagers armManagersMock;
  @Mock private DeleteRulesVerifier deleteRulesVerifierMock;

  @Mock private AzureResourceManager azureResourceManagerMock;
  @Mock private PrivateEndpoints privateEndpointsManagerMock;
  @Mock private GenericResources genericResourcesManagerMock;

  private ResourcesDeleteManager resourcesDeleteManager;

  @BeforeEach
  void setup() {
    mockArmManager();
    resourcesDeleteManager = new ResourcesDeleteManager(armManagersMock, deleteRulesVerifierMock);
  }

  @Test
  void delete_LZResourcesNotFound() throws LandingZoneRuleDeleteException {
    mockResourceListing(Collections.emptyList(), Collections.emptyList());

    resourcesDeleteManager.deleteLandingZoneResources(LANDING_ZONE_ID, RESOURCE_GROUP_NAME);

    verify(deleteRulesVerifierMock, times(1)).checkIfRulesAllowDelete(any());
    verify(genericResourcesManagerMock, never()).deleteById(any());
  }

  @Test
  void delete_SomeLZResourcesExist() throws LandingZoneRuleDeleteException {
    var genericResource = mock(GenericResource.class);
    when(genericResource.tags())
        .thenReturn(Map.of(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), LANDING_ZONE_ID));
    mockResourceListing(
        Collections.emptyList() /*ignoring private endpoints*/,
        Collections.singletonList(genericResource));

    resourcesDeleteManager.deleteLandingZoneResources(LANDING_ZONE_ID, RESOURCE_GROUP_NAME);

    verify(deleteRulesVerifierMock, times(1)).checkIfRulesAllowDelete(any());
    verify(genericResourcesManagerMock, times(1)).deleteById(any());
  }

  @Test
  void delete_LZResourceCouldNotBeDeletedBecauseItIsNotFound()
      throws LandingZoneRuleDeleteException {
    final String resourceId1 = "RESOURCE_ID_1";
    final String resourceId2 = "RESOURCE_ID_2";
    var genericResource1 =
        mockGenericResource(
            resourceId1, Map.of(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), LANDING_ZONE_ID));
    var genericResource2 =
        mockGenericResource(
            resourceId2, Map.of(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), LANDING_ZONE_ID));
    mockResourceListing(
        Collections.emptyList() /*ignoring private endpoints*/,
        Arrays.asList(genericResource1, genericResource2));

    var managementException = mock(ManagementException.class);
    var managementError = mock(ManagementError.class);
    when(managementError.getCode()).thenReturn("ResourceNotFound");
    when(managementException.getValue()).thenReturn(managementError);
    doThrow(managementException).when(genericResourcesManagerMock).deleteById(resourceId1);

    resourcesDeleteManager.deleteLandingZoneResources(LANDING_ZONE_ID, RESOURCE_GROUP_NAME);

    verify(deleteRulesVerifierMock, times(1)).checkIfRulesAllowDelete(any());
    verify(genericResourcesManagerMock, times(1)).deleteById(resourceId2);
    verify(genericResourcesManagerMock, times(1)).deleteById(resourceId1);
  }

  @Test
  void delete_CustomErrorDuringResourceDeletion() {
    final String resourceId1 = "RESOURCE_ID_1";
    final String customManagementExceptionCode = "CustomCode";
    var genericResource1 =
        mockGenericResource(
            resourceId1, Map.of(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), LANDING_ZONE_ID));

    mockResourceListing(
        Collections.emptyList() /*ignoring private endpoints*/,
        Collections.singletonList(genericResource1));

    var managementException = mock(ManagementException.class);
    var managementError = mock(ManagementError.class);
    when(managementError.getCode()).thenReturn(customManagementExceptionCode);
    when(managementException.getValue()).thenReturn(managementError);
    doThrow(managementException).when(genericResourcesManagerMock).deleteById(resourceId1);

    var exception =
        assertThrows(
            ManagementException.class,
            () ->
                resourcesDeleteManager.deleteLandingZoneResources(
                    LANDING_ZONE_ID, RESOURCE_GROUP_NAME));
    assertThat(exception.getValue().getCode(), equalTo(customManagementExceptionCode));
  }

  private void mockArmManager() {
    when(azureResourceManagerMock.genericResources()).thenReturn(genericResourcesManagerMock);
    when(azureResourceManagerMock.privateEndpoints()).thenReturn(privateEndpointsManagerMock);
    when(armManagersMock.azureResourceManager()).thenReturn(azureResourceManagerMock);
  }

  private void mockResourceListing(
      List<PrivateEndpoint> privateEndpoints, List<GenericResource> genericResources) {
    PagedIterable<PrivateEndpoint> pagedPrivateEndpointsMock = mockPagedIterable(privateEndpoints);
    when(privateEndpointsManagerMock.listByResourceGroup(RESOURCE_GROUP_NAME))
        .thenReturn(pagedPrivateEndpointsMock);

    PagedIterable<GenericResource> pagedGenericResourcesMock = mockPagedIterable(genericResources);
    when(genericResourcesManagerMock.listByResourceGroup(RESOURCE_GROUP_NAME))
        .thenReturn(pagedGenericResourcesMock);
  }

  private static GenericResource mockGenericResource(String id, Map<String, String> tags) {
    var genericResource = mock(GenericResource.class);
    when(genericResource.id()).thenReturn(id);
    when(genericResource.tags()).thenReturn(tags);
    return genericResource;
  }

  @SuppressWarnings("unchecked")
  private static <T> PagedIterable<T> mockPagedIterable(List<T> resources) {
    PagedIterable<T> pagedIterableMock = mock(PagedIterable.class);
    Answer<Stream<T>> answer = invocation -> resources.stream();
    when(pagedIterableMock.stream()).thenAnswer(answer);
    return pagedIterableMock;
  }
}
