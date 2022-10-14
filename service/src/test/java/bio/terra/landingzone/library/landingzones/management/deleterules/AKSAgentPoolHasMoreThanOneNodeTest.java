package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_AKS_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool;
import com.azure.resourcemanager.containerservice.models.KubernetesClusters;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

class AKSAgentPoolHasMoreThanOneNodeTest extends BaseDependencyRuleFixture {

  private AKSAgentPoolHasMoreThanOneNode rule;

  @Mock private KubernetesClusters clusters;

  @Mock private KubernetesCluster cluster;

  @Mock private KubernetesClusterAgentPool pool;

  @BeforeEach
  void setUp() {
    rule = new AKSAgentPoolHasMoreThanOneNode(armManagers);
  }

  @Test
  void getExpectedType_returnsExpectedType() {
    assertThat(rule.getExpectedType(), equalTo(AZURE_AKS_TYPE));
  }

  @ParameterizedTest
  @MethodSource("hasDependentResourcesScenario")
  void hasDependentResources_agentPoolSize(int poolSize, boolean expectedResult) {
    setUpResourceToDelete();
    when(azureResourceManager.kubernetesClusters()).thenReturn(clusters);
    when(clusters.getByResourceGroup(RESOURCE_GROUP, RESOURCE_NAME)).thenReturn(cluster);
    Map<String, KubernetesClusterAgentPool> pools = new HashMap<>();
    pools.put("Pool", pool);
    when(pool.count()).thenReturn(poolSize);
    when(cluster.agentPools()).thenReturn(pools);

    assertThat(rule.hasDependentResources(resourceToDelete), equalTo(expectedResult));
  }

  private static Stream<Arguments> hasDependentResourcesScenario() {
    return Stream.of(
        Arguments.of(0, false),
        Arguments.of(1, false),
        Arguments.of(2, true),
        Arguments.of(100, true));
  }
}
