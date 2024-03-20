package bio.terra.landingzone.terraform.updaters;

import com.hashicorp.cdktf.TerraformResourceLifecycle;
import com.hashicorp.cdktf.TerraformStack;
import com.hashicorp.cdktf.providers.azurerm.kubernetes_cluster.KubernetesCluster;
import com.hashicorp.cdktf.providers.azurerm.kubernetes_cluster.KubernetesClusterDefaultNodePool;
import com.hashicorp.cdktf.providers.azurerm.kubernetes_cluster.KubernetesClusterIdentity;
import java.util.List;

public class AKSUpdater {

  private final TerraformStack landingZoneStack;
  private static final List<String> IGNORED_ATTRIBUTES =
      List.of(
          "dns_prefix",
          "default_node_pool[0]",
          "automatic_channel_upgrade",
          "image_cleaner_enabled",
          "image_cleaner_interval_hours",
          "oidc_issuer_enabled",
          "oms_agent",
          "azure_active_directory_role_based_access_control",
          "workload_identity_enabled",
          "api_server_authorized_ip_ranges",
          "tags");

  public AKSUpdater(TerraformStack landingZoneStack) {

    this.landingZoneStack = landingZoneStack;
  }

  public void updateResource(
      String aksName,
      com.azure.resourcemanager.containerservice.models.KubernetesCluster existingAksCluster) {
    // this object is actually ignored as part of our IGNORED_ATTRIBUTES list, but
    // a value _has_ to be passed in order for a plan to be generated
    var defaultNodePool =
        KubernetesClusterDefaultNodePool.builder()
            .enableAutoScaling(true)
            .maxCount(100)
            .minCount(1)
            .name("ignored")
            .nodeCount(1)
            .osDiskSizeGb(128)
            .osDiskType("Managed")
            .osSku("Ubuntu")
            .scaleDownMode("Delete")
            .vmSize("Standard_D4as_v4")
            .build();

    var identity = KubernetesClusterIdentity.builder().type("SystemAssigned").build();

    KubernetesCluster.Builder.create(landingZoneStack, "aks")
        .name(aksName)
        .location(existingAksCluster.regionName())
        .resourceGroupName(existingAksCluster.resourceGroupName())
        .defaultNodePool(defaultNodePool)
        .dnsPrefix("ignore")
        .identity(identity)
        .lifecycle(TerraformResourceLifecycle.builder().ignoreChanges(IGNORED_ATTRIBUTES).build())
        .build();
  }
}
