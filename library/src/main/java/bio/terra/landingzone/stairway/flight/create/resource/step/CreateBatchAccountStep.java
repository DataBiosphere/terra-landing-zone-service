package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateBatchAccountStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateBatchAccountStep.class);
  public static final String BATCH_ACCOUNT_ID = "BATCH_ACCOUNT_ID";
  public static final String BATCH_ACCOUNT_RESOURCE_KEY = "BATCH_ACCOUNT";

  public CreateBatchAccountStep(
      ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    String batchAccountName = resourceNameProvider.getName(getResourceType());
    var batch =
        armManagers
            .batchManager()
            .batchAccounts()
            .define(batchAccountName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
            .withTags(
                Map.of(
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId.toString(),
                    LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                    ResourcePurpose.SHARED_RESOURCE.toString()))
            .create();
    context.getWorkingMap().put(BATCH_ACCOUNT_ID, batch.id());
    context
        .getWorkingMap()
        .put(
            BATCH_ACCOUNT_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(batch.id())
                .resourceType(batch.type())
                .tags(batch.tags())
                .region(batch.regionName())
                .resourceName(batch.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), batch.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.batchManager().batchAccounts().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "BatchAccount";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(BATCH_ACCOUNT_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_BATCH_ACCOUNT_NAME_LENGTH));
  }
}
