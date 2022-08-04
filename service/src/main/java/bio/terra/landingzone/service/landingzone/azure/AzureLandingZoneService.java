package bio.terra.landingzone.service.landingzone.azure;

import bio.terra.cloudres.azure.landingzones.definition.DefinitionVersion;
import bio.terra.cloudres.azure.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.cloudres.azure.landingzones.deployment.DeployedResource;
import bio.terra.cloudres.azure.landingzones.deployment.ResourcePurpose;
import bio.terra.cloudres.azure.landingzones.management.LandingZoneManager;
import bio.terra.common.exception.ValidationException;
import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.model.LandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneResource;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneTemplate;
import com.azure.core.util.ExpandableStringEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class AzureLandingZoneService {
  private static final Logger logger = LoggerFactory.getLogger(AzureLandingZoneService.class);

  private final LandingZoneDao landingZoneDao;

  @Autowired
  public AzureLandingZoneService(LandingZoneDao landingZoneDao) {
    this.landingZoneDao = landingZoneDao;
  }

  public AzureLandingZone createLandingZone(
      AzureLandingZoneDefinition azureLandingZone,
      LandingZoneManager landingZoneManager,
      String resourceGroupName)
      throws ValidationException {

    Predicate<FactoryDefinitionInfo> requiredTemplate =
        (FactoryDefinitionInfo f) ->
            f.className().equals(azureLandingZone.getName())
                && f.versions().stream()
                    .map(ExpandableStringEnum::toString)
                    .toList()
                    .contains(azureLandingZone.getVersion());
    var requestedFactory =
        LandingZoneManager.listDefinitionFactories().stream().filter(requiredTemplate).findFirst();

    if (requestedFactory.isEmpty()) {
      logger.warn(
          "Azure landing zone definition with name={} and version={} doesn't exist",
          azureLandingZone.getName(),
          azureLandingZone.getVersion());
      throw new ValidationException("Requested landing zone definition doesn't exist");
    }

    UUID landingZoneId = UUID.randomUUID();
    List<DeployedResource> deployedResources =
        landingZoneManager.deployLandingZone(
            landingZoneId.toString(),
            requestedFactory.get().className(),
            DefinitionVersion.fromString(azureLandingZone.getVersion()),
            azureLandingZone.getParameters());

    landingZoneDao.createLandingZone(
        LandingZone.builder()
            .landingZoneId(landingZoneId)
            .resourceGroupId(resourceGroupName)
            .definition(azureLandingZone.getName())
            .displayName(requestedFactory.get().description())
            .description(requestedFactory.get().description())
            .properties(azureLandingZone.getParameters())
            .build());
    return AzureLandingZone.builder()
        .id(landingZoneId.toString())
        .deployedResources(
            deployedResources.stream()
                .map(
                    dr ->
                        AzureLandingZoneResource.builder()
                            .resourceId(dr.resourceId())
                            .resourceType(dr.resourceType())
                            .tags(dr.tags())
                            .region(dr.region())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  @Cacheable("landingZoneDefinitions")
  public List<AzureLandingZoneTemplate> listLandingZoneDefinitions() {
    List<AzureLandingZoneTemplate> landingZoneTemplates = new ArrayList<>();
    for (var factoryInfo : LandingZoneManager.listDefinitionFactories()) {
      factoryInfo
          .versions()
          .forEach(
              version ->
                  landingZoneTemplates.add(
                      AzureLandingZoneTemplate.builder()
                          // TODO: remove FQN
                          .definition(factoryInfo.className())
                          .name(factoryInfo.name())
                          .description(factoryInfo.description())
                          .version(version.toString())
                          .build()));
    }
    return landingZoneTemplates;
  }

  public List<AzureLandingZoneResource> listResourcesByPurpose(
      LandingZoneManager landingZoneManager, ResourcePurpose purpose) {

    var deployedResources = landingZoneManager.reader().listResourcesByPurpose(purpose);

    return deployedResources.stream()
        .map(
            dp ->
                AzureLandingZoneResource.builder()
                    .resourceId(dp.resourceId())
                    .resourceType(dp.resourceType())
                    .tags(dp.tags())
                    .region(dp.region())
                    .build())
        .collect(Collectors.toList());
  }

  public void deleteLandingZone(String landingZoneId) {
    throw new NotImplementedException("Delete operation is not implemented");
  }
}
