package bio.terra.landingzone.service.landingzone.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.landingzone.job.AzureLandingZoneJobService;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionFactory;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.library.landingzones.management.ResourcesReader;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDefinitionNotFound;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDeleteNotImplemented;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import java.util.List;
import java.util.Map;
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
public class LandingZoneServiceTest {
  private static final String VNET_1 = "vnet_1";
  private static final String VNET_SUBNET_1 = "vnet_subnet_1";
  private static final String VIRTUAL_NETWORK = "VirtualNetwork";
  private static final String SUBNET = "Subnet";
  private static final String REGION = "westus";
  private static final String TAG_NAME = "TAG";
  private static final String TAG_VALUE = "VALUE";
  public static final Map<String, String> TAGS = Map.of(TAG_NAME, TAG_VALUE);

  private LandingZoneService landingZoneService;

  @Mock private LandingZoneManager landingZoneManager;

  @Mock private AzureLandingZoneJobService landingZoneJobService;
  @Mock private LandingZoneAzureConfiguration landingZoneAzureConfiguration;

  @BeforeEach
  public void setup() {
    landingZoneService =
        new LandingZoneService(landingZoneJobService, landingZoneAzureConfiguration);
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
          new LandingZoneRequest(
              mockFactory1.getClass().getName(), requestedVersion.toString(), null, azureCloudContext);

      var azureLandingZone =
          landingZoneService.createLandingZone(azureLandingZoneDefinition, landingZoneManager);

      assertNotNull(azureLandingZone);
      assertNotNull(azureLandingZone.getId());
      Assertions.assertEquals(2, azureLandingZone.getDeployedResources().size());
      validateDeployedResource(
          azureLandingZone.getDeployedResources(), 1, VNET_1, VIRTUAL_NETWORK, TAGS, REGION);
      validateDeployedResource(
          azureLandingZone.getDeployedResources(), 1, VNET_SUBNET_1, SUBNET, TAGS, REGION);
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
          new LandingZoneRequest(
              mockFactory1.getClass().getName(), notImplementedVersion.toString(), null, azureCloudContext);
      Assertions.assertThrows(
          LandingZoneDefinitionNotFound.class,
          () ->
              landingZoneService.createLandingZone(azureLandingZoneDefinition, landingZoneManager));
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

    List<LandingZoneResource> resources =
        landingZoneService.listResourcesByPurpose(
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
      List<LandingZoneDefinition> templates = landingZoneService.listLandingZoneDefinitions();

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
        LandingZoneDeleteNotImplemented.class,
        () -> landingZoneService.deleteLandingZone("lz-1"),
        "Delete operation is not supported");
  }

  private long countAzureLandingZoneTemplateRecordsWithAttribute(
      List<LandingZoneDefinition> list,
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
      List<LandingZoneResource> list,
      long expectedCount,
      String expectedResourceId,
      String expectedResourceType,
      Map<String, String> expectedTags,
      String expectedRegion) {
    List<LandingZoneResource> deployedResources =
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
