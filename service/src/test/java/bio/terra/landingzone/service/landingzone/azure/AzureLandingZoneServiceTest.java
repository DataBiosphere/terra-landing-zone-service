package bio.terra.landingzone.service.landingzone.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cloudres.azure.landingzones.definition.DefinitionVersion;
import bio.terra.cloudres.azure.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.cloudres.azure.landingzones.definition.factories.LandingZoneDefinitionFactory;
import bio.terra.cloudres.azure.landingzones.deployment.DeployedResource;
import bio.terra.cloudres.azure.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.cloudres.azure.landingzones.deployment.ResourcePurpose;
import bio.terra.cloudres.azure.landingzones.management.LandingZoneManager;
import bio.terra.cloudres.azure.landingzones.management.ResourcesReader;
import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.service.landingzone.azure.exception.AzureLandingZoneDefinitionNotFound;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneResource;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AzureLandingZoneServiceTest {
  private static final String VNET_1 = "vnet_1";
  private static final String VNET_SUBNET_1 = "vnet_subnet_1";
  private static final String VIRTUAL_NETWORK = "VirtualNetwork";
  private static final String SUBNET = "Subnet";
  private static final String REGION = "westus";
  private static final String TAG_NAME = "TAG";
  private static final String TAG_VALUE = "VALUE";
  public static final Map<String, String> TAGS = Map.of(TAG_NAME, TAG_VALUE);

  private AzureLandingZoneService azureLandingZoneService;

  @Mock private LandingZoneManager landingZoneManager;
  @Mock private LandingZoneDao landingZoneDao;

  @BeforeEach
  public void setup() {
    azureLandingZoneService = new AzureLandingZoneService(landingZoneDao);
  }

  @Test
  public void createLandingZoneSuccess() {
    var mockFactory1 = Mockito.mock(LandingZoneDefinitionFactory.class);
    Mockito.when(mockFactory1.availableVersions())
        .thenReturn(List.of(DefinitionVersion.V1, DefinitionVersion.V2));

    List<FactoryDefinitionInfo> factories =
        List.of(
            new FactoryDefinitionInfo(
                mockFactory1.getClass().getName(),
                "mockFactory",
                mockFactory1.getClass().getName(),
                mockFactory1.availableVersions()));

    try (MockedStatic<LandingZoneManager> staticMockLandingZoneManager =
        Mockito.mockStatic(LandingZoneManager.class)) {
      staticMockLandingZoneManager
          .when(LandingZoneManager::listDefinitionFactories)
          .thenReturn(factories);

      List<DeployedResource> deployedResources =
          List.of(
              new DeployedResource(VNET_1, VIRTUAL_NETWORK, TAGS, REGION),
              new DeployedResource(VNET_SUBNET_1, SUBNET, TAGS, REGION));
      Mockito.when(
              landingZoneManager.deployLandingZone(
                  ArgumentMatchers.any(),
                  ArgumentMatchers.any(),
                  ArgumentMatchers.any(),
                  ArgumentMatchers.any()))
          .thenReturn(deployedResources);
      DefinitionVersion requestedVersion = DefinitionVersion.V1;
      var azureLandingZoneDefinition =
          new AzureLandingZoneRequest(
              mockFactory1.getClass().getName(), requestedVersion.toString(), null);

      var azureLandingZone =
          azureLandingZoneService.createLandingZone(
              azureLandingZoneDefinition, landingZoneManager, "resourceGroup");

      assertNotNull(azureLandingZone);
      assertNotNull(azureLandingZone.getId());
      Assertions.assertEquals(2, azureLandingZone.getDeployedResources().size());
      validateDeployedResource(
          azureLandingZone.getDeployedResources(), 1, VNET_1, VIRTUAL_NETWORK, TAGS, REGION);
      validateDeployedResource(
          azureLandingZone.getDeployedResources(), 1, VNET_SUBNET_1, SUBNET, TAGS, REGION);
      Mockito.verify(landingZoneDao, Mockito.times(1)).createLandingZone(ArgumentMatchers.any());
    }
  }

  @Test
  public void createLandingZoneThrowErrorWhenDefinitionDoesntExist() {
    var mockFactory1 = Mockito.mock(LandingZoneDefinitionFactory.class);
    Mockito.when(mockFactory1.availableVersions())
        .thenReturn(List.of(DefinitionVersion.V1, DefinitionVersion.V2));

    List<FactoryDefinitionInfo> factories =
        List.of(
            new FactoryDefinitionInfo(
                mockFactory1.getClass().getName(),
                "mockFactory",
                mockFactory1.getClass().getName(),
                mockFactory1.availableVersions()));

    try (MockedStatic<LandingZoneManager> staticMockLandingZoneManager =
        Mockito.mockStatic(LandingZoneManager.class)) {
      staticMockLandingZoneManager
          .when(LandingZoneManager::listDefinitionFactories)
          .thenReturn(factories);

      DefinitionVersion notImplementedVersion = DefinitionVersion.V5;
      var azureLandingZoneDefinition =
          new AzureLandingZoneRequest(
              mockFactory1.getClass().getName(), notImplementedVersion.toString(), null);
      Assertions.assertThrows(
          AzureLandingZoneDefinitionNotFound.class,
          () ->
              azureLandingZoneService.createLandingZone(
                  azureLandingZoneDefinition,
                  landingZoneManager,
                  "Requested landing zone definition doesn't exist"));
      Mockito.verify(landingZoneDao, Mockito.never()).createLandingZone(ArgumentMatchers.any());
    }
  }

  @Test
  public void listResourcesByPurposeSuccess() {
    var purposeTags =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.SHARED_RESOURCE.toString());
    var deployedResources =
        List.of(
            new DeployedResource(VNET_1, VIRTUAL_NETWORK, purposeTags, REGION),
            new DeployedResource(VNET_SUBNET_1, SUBNET, purposeTags, REGION));

    ResourcesReader resourceReader = Mockito.mock(ResourcesReader.class);
    Mockito.when(resourceReader.listResourcesByPurpose(ArgumentMatchers.any()))
        .thenReturn(deployedResources);
    Mockito.when(landingZoneManager.reader()).thenReturn(resourceReader);

    List<AzureLandingZoneResource> resources =
        azureLandingZoneService.listResourcesByPurpose(
            landingZoneManager, ResourcePurpose.SHARED_RESOURCE);

    assertNotNull(resources);
    assertEquals(2, resources.size());
  }

  @Test
  public void listLandingZoneTemplatesSuccess() {
    var mockFactory1 = Mockito.mock(LandingZoneDefinitionFactory.class);

    List<FactoryDefinitionInfo> factories =
        List.of(
            new FactoryDefinitionInfo(
                "name1",
                "description1",
                mockFactory1.getClass().getName(),
                List.of(DefinitionVersion.V1, DefinitionVersion.V2)));
    try (MockedStatic<LandingZoneManager> staticMockLandingZoneManager =
        Mockito.mockStatic(LandingZoneManager.class)) {
      staticMockLandingZoneManager
          .when(LandingZoneManager::listDefinitionFactories)
          .thenReturn(factories);
      List<AzureLandingZoneDefinition> templates =
          azureLandingZoneService.listLandingZoneDefinitions();

      assertEquals(
          1,
          countAzureLandingZoneTemplateRecordsWithAttribute(
              templates, "name1", "description1", mockFactory1.getClass().getName(), "v1"));
      assertEquals(
          1,
          countAzureLandingZoneTemplateRecordsWithAttribute(
              templates, "name1", "description1", mockFactory1.getClass().getName(), "v2"));
    }
  }

  @Test
  public void deleteAzureLandingZoneThrowsException() {
    Assertions.assertThrows(
        NotImplementedException.class,
        () -> azureLandingZoneService.deleteLandingZone("lz-1"),
        "Delete operation is not supported");
    Mockito.verify(landingZoneDao, Mockito.never()).createLandingZone(ArgumentMatchers.any());
  }

  private long countAzureLandingZoneTemplateRecordsWithAttribute(
      List<AzureLandingZoneDefinition> list,
      String name,
      String description,
      String className,
      String version) {
    return list.stream()
        .filter(
            t ->
                t.getName().equals(name)
                    && t.getDescription().equals(description)
                    && t.getDefinition().equals(className)
                    && t.getVersion().equals(version))
        .count();
  }

  private void validateDeployedResource(
      List<AzureLandingZoneResource> list,
      long expectedCount,
      String expectedResourceId,
      String expectedResourceType,
      Map<String, String> expectedTags,
      String expectedRegion) {
    List<AzureLandingZoneResource> deployedResources =
        list.stream()
            .filter(
                r ->
                    r.getResourceId().equals(expectedResourceId)
                        && r.getResourceType().equals(expectedResourceType)
                        && r.getRegion().equals(expectedRegion))
            .toList();
    assertEquals(expectedCount, deployedResources.size());
    Assertions.assertEquals(expectedResourceId, deployedResources.get(0).getResourceId());
    Assertions.assertEquals(expectedResourceType, deployedResources.get(0).getResourceType());
    Assertions.assertEquals(expectedTags.size(), deployedResources.get(0).getTags().size());
    Assertions.assertEquals(expectedRegion, deployedResources.get(0).getRegion());
  }
}
