package bio.terra.landingzone.service.landingzone.azure;

import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.service.landingzone.azure.exception.AzureLandingZoneDefinitionNotFound;
import bio.terra.landingzone.service.landingzone.azure.exception.AzureLandingZoneDeleteNotImplemented;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.AzureLandingZoneResource;
import com.azure.core.util.ExpandableStringEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class AzureLandingZoneService {
  private static final Logger logger = LoggerFactory.getLogger(AzureLandingZoneService.class);

  public AzureLandingZone createLandingZone(
      AzureLandingZoneRequest azureLandingZone, LandingZoneManager landingZoneManager)
      throws AzureLandingZoneDefinitionNotFound {

    Predicate<FactoryDefinitionInfo> requiredDefinition =
        (FactoryDefinitionInfo f) ->
            f.className().equals(azureLandingZone.getDefinition())
                && f.versions().stream()
                    .map(ExpandableStringEnum::toString)
                    .toList()
                    .contains(azureLandingZone.getVersion());
    var requestedFactory =
        LandingZoneManager.listDefinitionFactories().stream()
            .filter(requiredDefinition)
            .findFirst();

    if (requestedFactory.isEmpty()) {
      logger.warn(
          "Azure landing zone definition with name={} and version={} doesn't exist",
          azureLandingZone.getDefinition(),
          azureLandingZone.getVersion());
      throw new AzureLandingZoneDefinitionNotFound(
          "Requested landing zone definition doesn't exist");
    }

    UUID landingZoneId = UUID.randomUUID();
    List<DeployedResource> deployedResources =
        landingZoneManager.deployLandingZone(
            landingZoneId.toString(),
            requestedFactory.get().className(),
            DefinitionVersion.fromString(azureLandingZone.getVersion()),
            azureLandingZone.getParameters());

    logger.info(
        "Azure Landing Zone definition with the following "
            + "parameters: definition={}, version={} successfully created.",
        azureLandingZone.getDefinition(),
        azureLandingZone.getVersion());
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
  public List<AzureLandingZoneDefinition> listLandingZoneDefinitions() {
    List<AzureLandingZoneDefinition> landingZoneTemplates = new ArrayList<>();
    for (var factoryInfo : LandingZoneManager.listDefinitionFactories()) {
      factoryInfo
          .versions()
          .forEach(
              version ->
                  landingZoneTemplates.add(
                      AzureLandingZoneDefinition.builder()
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
    throw new AzureLandingZoneDeleteNotImplemented("Delete operation is not implemented");
  }
}
