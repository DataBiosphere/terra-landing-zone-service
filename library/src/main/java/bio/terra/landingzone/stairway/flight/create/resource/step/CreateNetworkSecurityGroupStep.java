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
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.network.models.NetworkSecurityGroup;
import com.azure.resourcemanager.network.models.SecurityRuleProtocol;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class CreateNetworkSecurityGroupStep extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateNetworkSecurityGroupStep.class);
  public static final String NSG_ID = "NSG_ID";
  public static final String NSG_RESOURCE_KEY = "NSG";
  public static final String BATCH_NSG_ID = "BATCH_NSG_ID";
  public static final String BATCH_NSG_RESOURCE_KEY = "BATCH_NSG";

  public CreateNetworkSecurityGroupStep(
      ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var nsgName = resourceNameProvider.getName(getResourceType());
    var batchNsgName = resourceNameProvider.getName(getResourceType());

    NetworkSecurityGroup defaultNsg = createNSG(nsgName, context, landingZoneId, false);
    context.getWorkingMap().put(NSG_ID, defaultNsg.id());
    context
        .getWorkingMap()
        .put(
            NSG_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(defaultNsg.id())
                .resourceType(defaultNsg.type())
                .tags(defaultNsg.tags())
                .region(defaultNsg.regionName())
                .resourceName(defaultNsg.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), defaultNsg.id(), getMRGName(context));

    NetworkSecurityGroup batchNsg = createNSG(batchNsgName, context, landingZoneId, true);
    context.getWorkingMap().put(BATCH_NSG_ID, batchNsg.id());
    context
        .getWorkingMap()
        .put(
            BATCH_NSG_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(batchNsg.id())
                .resourceType(batchNsg.type())
                .tags(batchNsg.tags())
                .region(batchNsg.regionName())
                .resourceName(batchNsg.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), batchNsg.id(), getMRGName(context));
  }

  private NetworkSecurityGroup createNSG(
      String nsgName, FlightContext context, UUID landingZoneId, boolean isBatchNsg) {
    NetworkSecurityGroup nsg = null;
    try {
      var withCreate =
          armManagers
              .azureResourceManager()
              .networkSecurityGroups()
              .define(nsgName)
              .withRegion(getMRGRegionName(context))
              .withExistingResourceGroup(getMRGName(context));

      if (isBatchNsg) {
        withCreate = attachBatchNSGRules(withCreate, context);
      }

      nsg =
          withCreate
              .withTags(
                  Map.of(
                      LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                      landingZoneId.toString(),
                      LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                      ResourcePurpose.SHARED_RESOURCE.toString()))
              .create();
    } catch (ManagementException e) {
      // resource may already exist if this step is being retried
      if (e.getResponse() != null
          && HttpStatus.CONFLICT.value() == e.getResponse().getStatusCode()) {
        nsg =
            armManagers
                .azureResourceManager()
                .networkSecurityGroups()
                .getByResourceGroup(getMRGName(context), nsgName);
      } else {
        throw e;
      }
    }
    return nsg;
  }

  private NetworkSecurityGroup.DefinitionStages.WithCreate attachBatchNSGRules(
      NetworkSecurityGroup.DefinitionStages.WithCreate withCreate, FlightContext context) {
    return withCreate
        .defineRule("ALLOW_IN_BATCH_SERVICE")
        .allowInbound()
        .fromAddress(
            String.format("BatchNodeManagement.%s", getMRGRegionName(context)))
        .fromAnyPort()
        .toAnyAddress()
        .toPortRanges("29876-29877")
        .withProtocol(SecurityRuleProtocol.TCP)
        .attach()
        .defineRule("ALLOW_OUT_BATCH_SERVICE")
        .allowOutbound()
        .fromAnyAddress()
        .fromAnyPort()
        .toAddress(
            String.format("BatchNodeManagement.%s", getMRGRegionName(context)))
        .toPort(443)
        .withAnyProtocol()
        .attach()
        .defineRule("ALLOW_OUT_STORAGE")
        .allowOutbound()
        .fromAnyAddress()
        .fromAnyPort()
        .toAddress(
            String.format("Storage.%s", getMRGRegionName(context)))
        .toPort(443)
        .withProtocol(SecurityRuleProtocol.TCP)
        .attach();
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.azureResourceManager().networkSecurityGroups().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "NetworkSecurityGroup";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(NSG_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(getResourceType(), ResourceNameGenerator.MAX_NSG_NAME_LENGTH));
  }
}
