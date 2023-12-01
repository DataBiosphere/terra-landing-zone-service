package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.landingzone.stairway.flight.exception.ResourceCreationException;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.containerservice.models.AgentPoolMode;
import com.azure.resourcemanager.containerservice.models.ContainerServiceVMSizeTypes;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.containerservice.models.ManagedClusterOidcIssuerProfile;
import com.azure.resourcemanager.containerservice.models.ManagedClusterSecurityProfile;
import com.azure.resourcemanager.containerservice.models.ManagedClusterSecurityProfileWorkloadIdentity;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class CreateAksStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateAksStep.class);
  public static final String AKS_ID = "AKS_ID";
  public static final String AKS_RESOURCE_KEY = "AKS";
  private static final String DNS_SUFFIX_KEY = "_DNS";
  private static final String POOL_SUFFIX_KEY = "_POOL";
  public static final String AKS_OIDC_ISSUER_URL = "AKS_OIDC_ISSUER_URL";
  public static final int NODE_RESOURCE_GROUP_NAME_MAX_LENGTH = 80;
  public static final String NODE_RESOURCE_GROUP_NAME_SUFFIX = "_aks";
  public static final String SPOT_NODE_POOL_NAME = "spotnodepool";

  // it's always true, false is only for testing; see denySleepWhilePoolingForAksStatus() method
  private boolean sleepWhilePollingAksStatus = true;

  public CreateAksStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameProvider resourceNameProvider) {
    super(armManagers, parametersResolver, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var vNetId = getParameterOrThrow(context.getWorkingMap(), CreateVnetStep.VNET_ID, String.class);
    boolean costSavingsSpotNodesEnabled =
        Boolean.parseBoolean(
            parametersResolver.getValue(
                CromwellBaseResourcesFactory.ParametersNames.AKS_COST_SAVING_SPOT_NODES_ENABLED
                    .name()));
    boolean costSavingsVpaEnabled =
        Boolean.parseBoolean(
            parametersResolver.getValue(
                CromwellBaseResourcesFactory.ParametersNames.AKS_COST_SAVING_VPA_ENABLED
                    .name()));
    boolean autoScalingEnabled =
        Boolean.parseBoolean(
            parametersResolver.getValue(
                CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_ENABLED.name()));

    var aks = createAks(context, vNetId, costSavingsSpotNodesEnabled, autoScalingEnabled);
    enableWorkloadIdentity(aks);
    enableCostSavings(aks, vNetId, costSavingsSpotNodesEnabled);

    context.getWorkingMap().put(AKS_ID, aks.id());
    context
        .getWorkingMap()
        .put(AKS_OIDC_ISSUER_URL, aks.innerModel().oidcIssuerProfile().issuerUrl());
    context
        .getWorkingMap()
        .put(
            AKS_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(aks.id())
                .resourceType(aks.type())
                .tags(aks.tags())
                .region(aks.regionName())
                .resourceName(aks.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), aks.id(), getMRGName(context));
  }

  private KubernetesCluster createAks(
      FlightContext context,
      String vNetId,
      boolean costSavingsSpotNodesEnabled,
      boolean autoScalingEnabled) {
    UUID landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    var aksName = resourceNameProvider.getName(getResourceType());

    KubernetesCluster aks;
    try {
      var aksPartial =
          armManagers
              .azureResourceManager()
              .kubernetesClusters()
              .define(aksName)
              .withRegion(getMRGRegionName(context))
              .withExistingResourceGroup(getMRGName(context))
              .withDefaultVersion()
              .withSystemAssignedManagedServiceIdentity()
              .withAgentPoolResourceGroup(getNodeResourceGroup(getMRGName(context)))
              .withAzureActiveDirectoryGroup(
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.ParametersNames.AKS_AAD_PROFILE_USER_GROUP_ID
                          .name()))
              .defineAgentPool(resourceNameProvider.getName(getResourceType() + POOL_SUFFIX_KEY))
              .withVirtualMachineSize(
                  ContainerServiceVMSizeTypes.fromString(
                      parametersResolver.getValue(
                          CromwellBaseResourcesFactory.ParametersNames.AKS_MACHINE_TYPE.name())))
              .withAgentPoolVirtualMachineCount(
                  Integer.parseInt(
                      parametersResolver.getValue(
                          CromwellBaseResourcesFactory.ParametersNames.AKS_NODE_COUNT.name())))
              .withAgentPoolMode(AgentPoolMode.SYSTEM)
              .withVirtualNetwork(vNetId, CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name());

      // Add autoscaling to system nodepool
      if (autoScalingEnabled) {
        int min =
            Integer.parseInt(
                parametersResolver.getValue(
                    CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_MIN.name()));
        int max =
            Integer.parseInt(
                parametersResolver.getValue(
                    CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_MAX.name()));
        aksPartial = aksPartial.withAutoScaling(min, max);
      }

      aks =
          aksPartial
              .attach()
              .withDnsPrefix(resourceNameProvider.getName(getResourceType() + DNS_SUFFIX_KEY))
              .withTags(buildTagMap(landingZoneId, costSavingsSpotNodesEnabled))
              .create();
    } catch (ManagementException e) {
      if (e.getResponse() != null
          && HttpStatus.CONFLICT.value() == e.getResponse().getStatusCode()) {
        aks = handleConflictAndMaybeGetAks(context, aksName, e);
      } else {
        throw e;
      }
    }
    return aks;
  }

  private KubernetesCluster handleConflictAndMaybeGetAks(
      FlightContext context, String aksName, ManagementException e) {
    return switch (e.getValue().getCode().toLowerCase()) {
        /*duplicate request (Stairway has resumed flight after interruption)
        but resource is not ready for use and is still being provisioned*/
      case "operationnotallowed" -> waitAndMaybeGetAksProvisioned(getMRGName(context), aksName);
        /*duplicate request (Stairway resume flight after interruption), but resource is ready for use*/
      case "conflict" -> armManagers
          .azureResourceManager()
          .kubernetesClusters()
          .getByResourceGroup(getMRGName(context), aksName);
      default -> throw e;
    };
  }

  private KubernetesCluster waitAndMaybeGetAksProvisioned(String mrgName, String aksName) {
    final int pollCycleNumberMax = 30;
    final int sleepDurationSeconds = 30;
    int pollCycleNumber = 0;
    KubernetesCluster existingAks;
    try {
      boolean aksNotProvisioned;
      do {
        if (sleepWhilePollingAksStatus) {
          /*always sleep except during unit testing*/
          TimeUnit.SECONDS.sleep(sleepDurationSeconds);
        }
        existingAks =
            armManagers
                .azureResourceManager()
                .kubernetesClusters()
                .getByResourceGroup(mrgName, aksName);
        aksNotProvisioned =
            (existingAks == null)
                || (!StringUtils.equalsIgnoreCase(existingAks.provisioningState(), "Succeeded"));
        if (++pollCycleNumber == pollCycleNumberMax && aksNotProvisioned) {
          throw new ResourceCreationException(
              "Aks resource still not ready after %s sec."
                  .formatted(pollCycleNumber * sleepDurationSeconds));
        }
      } while (aksNotProvisioned);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResourceCreationException(e.getMessage(), e);
    }
    return existingAks;
  }

  private Map<String, String> buildTagMap(UUID landingZoneId, boolean costSavingsSpotNodesEnabled) {
    return Map.of(
        LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
        landingZoneId.toString(),
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        ResourcePurpose.SHARED_RESOURCE.toString(),
        LandingZoneTagKeys.AKS_COST_SAVING_SPOT_NODES_ENABLED.toString(),
        String.valueOf(costSavingsSpotNodesEnabled));
  }

  @VisibleForTesting
  void denySleepWhilePoolingForAksStatus() {
    sleepWhilePollingAksStatus = false;
  }

  /** see https://github.com/Azure/azure-sdk-for-java/issues/31271 */
  private static void enableWorkloadIdentity(KubernetesCluster aks) {
    KubernetesCluster.Update update = aks.update();

    var securityProfile = aks.innerModel().securityProfile();
    if (securityProfile == null) {
      securityProfile = new ManagedClusterSecurityProfile();
      aks.innerModel().withSecurityProfile(securityProfile);
    }
    securityProfile.withWorkloadIdentity(
        new ManagedClusterSecurityProfileWorkloadIdentity().withEnabled(true));
    aks.innerModel().withOidcIssuerProfile(new ManagedClusterOidcIssuerProfile().withEnabled(true));

    update.apply();
    aks.refresh();
  }

  private void enableCostSavings(
      KubernetesCluster aks, String vNetId, boolean costSavingsSpotNodesEnabled) {
    // Enable a spot nodepool if cost savings are enabled.
    if (costSavingsSpotNodesEnabled) {
      // set to default if no specified parameters were provided at LZ creation
      var machineSize =
          !StringUtils.isEmpty(
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.ParametersNames.AKS_SPOT_MACHINE_TYPE.name()))
              ? ContainerServiceVMSizeTypes.fromString(
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.ParametersNames.AKS_SPOT_MACHINE_TYPE.name()))
              : ContainerServiceVMSizeTypes.fromString(
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.ParametersNames.AKS_MACHINE_TYPE.name()));

      // spot nodes should always use auto scaler per MS documentation:
      // https://learn.microsoft.com/en-us/azure/aks/spot-node-pool
      // "If you don't use a cluster autoscaler, upon eviction,
      // the Spot pool will eventually decrease to 0 and require manual operation to receive any
      // additional Spot nodes."
      int max =
          Integer.parseInt(
              parametersResolver.getValue(
                  CromwellBaseResourcesFactory.ParametersNames.AKS_SPOT_AUTOSCALING_MAX.name()));

      var aksPartialUpdate =
          aks.update()
              .defineAgentPool(SPOT_NODE_POOL_NAME)
              .withVirtualMachineSize(machineSize)
              .withAgentPoolVirtualMachineCount(
                  Integer.parseInt(
                      parametersResolver.getValue(
                          CromwellBaseResourcesFactory.ParametersNames.AKS_NODE_COUNT.name())))
              .withSpotPriorityVirtualMachine()
              .withAgentPoolMode(AgentPoolMode.USER)
              .withAutoScaling(1, max)
              .withVirtualNetwork(vNetId, CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name());

      var attach = aksPartialUpdate.attach();
      attach.apply();
      aks.refresh();
    }
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.azureResourceManager().kubernetesClusters().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "AKS";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(AKS_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType() /*main*/, ResourceNameGenerator.MAX_AKS_CLUSTER_NAME_LENGTH),
        new ResourceNameRequirements(
            getResourceType() + POOL_SUFFIX_KEY,
            ResourceNameGenerator.MAX_AKS_AGENT_POOL_NAME_LENGTH),
        new ResourceNameRequirements(
            getResourceType() + DNS_SUFFIX_KEY,
            ResourceNameGenerator.MAX_AKS_DNS_PREFIX_NAME_LENGTH));
  }

  /**
   * Define name of the node resource group for AKS.
   *
   * <p>The name of the node resource group should follow this pattern [RESOURCE_GROUP_NAME]_aks.
   * Maximum allowed length of the name is 80 characters. RESOURCE_GROUP_NAME might be truncated in
   * case of the full name length exceeds 80 characters.
   *
   * @param managedResourceGroupName Name of the managed resource group
   * @return Custom name for AKS node resource group
   */
  @VisibleForTesting
  String getNodeResourceGroup(String managedResourceGroupName) {
    var nodeResourceGroup =
        "%s%s".formatted(managedResourceGroupName, NODE_RESOURCE_GROUP_NAME_SUFFIX);
    if (nodeResourceGroup.length() > NODE_RESOURCE_GROUP_NAME_MAX_LENGTH) {
      nodeResourceGroup =
          nodeResourceGroup.replace(
              managedResourceGroupName,
              managedResourceGroupName.substring(
                  0,
                  NODE_RESOURCE_GROUP_NAME_MAX_LENGTH - NODE_RESOURCE_GROUP_NAME_SUFFIX.length()));
    }
    return nodeResourceGroup;
  }
}
