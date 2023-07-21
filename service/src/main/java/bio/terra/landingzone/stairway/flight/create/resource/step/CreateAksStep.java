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
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.containerservice.models.AgentPoolMode;
import com.azure.resourcemanager.containerservice.models.ContainerServiceVMSizeTypes;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.containerservice.models.ManagedClusterAddonProfile;
import com.azure.resourcemanager.containerservice.models.ManagedClusterOidcIssuerProfile;
import com.azure.resourcemanager.containerservice.models.ManagedClusterSecurityProfile;
import com.azure.resourcemanager.containerservice.models.ManagedClusterSecurityProfileWorkloadIdentity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAksStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateAksStep.class);
  public static final String AKS_ID = "AKS_ID";
  public static final String AKS_RESOURCE_KEY = "AKS";
  private static final String DNS_SUFFIX_KEY = "_DNS";
  private static final String POOL_SUFFIX_KEY = "_POOL";
  public static final String AKS_OIDC_ISSUER_URL = "AKS_OIDC_ISSUER_URL";

  public CreateAksStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameProvider resourceNameProvider) {
    super(armManagers, parametersResolver, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    UUID landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);
    var vNetId = getParameterOrThrow(context.getWorkingMap(), CreateVnetStep.VNET_ID, String.class);

    final Map<String, ManagedClusterAddonProfile> addonProfileMap = new HashMap<>();
    addonProfileMap.put(
        "omsagent",
        new ManagedClusterAddonProfile()
            .withEnabled(true)
            .withConfig(Map.of("logAnalyticsWorkspaceResourceID", logAnalyticsWorkspaceId)));

    var aksName = resourceNameProvider.getName(getResourceType());
    var aksPartial =
        armManagers
            .azureResourceManager()
            .kubernetesClusters()
            .define(aksName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
            .withDefaultVersion()
            .withSystemAssignedManagedServiceIdentity()
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

    if (Boolean.parseBoolean(
        parametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_ENABLED.name()))) {
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

    var aks =
        aksPartial
            .attach()
            .withDnsPrefix(resourceNameProvider.getName(getResourceType() + DNS_SUFFIX_KEY))
            .withAddOnProfiles(addonProfileMap)
            .withTags(
                Map.of(
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId.toString(),
                    LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                    ResourcePurpose.SHARED_RESOURCE.toString()))
            .create();

    enableWorkloadIdentity(aks);

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
}
