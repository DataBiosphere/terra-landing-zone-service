package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_RELAY_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.relay.models.HybridConnection;
import com.azure.resourcemanager.relay.models.HybridConnections;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

class AzureRelayHasHybridConnectionsTest extends BaseDependencyRuleUnitTest {

  @Mock private AzureRelayHasHybridConnections rule;

  @Mock private HybridConnections connections;

  @BeforeEach
  void setUp() {
    rule = new AzureRelayHasHybridConnections(armManagers);
  }

  @Test
  void getExpectedType_returnsExpectedType() {
    assertThat(rule.getExpectedType(), equalTo(AZURE_RELAY_TYPE));
  }

  @ParameterizedTest
  @MethodSource("hasDependentResourcesScenario")
  void hasDependentResources_hybridConnectionsScenarios(
      List<HybridConnection> hcConnections, boolean expectedResult) {
    setUpResourceToDelete();
    PagedIterable<HybridConnection> values = toMockPageIterable(hcConnections);
    when(relayManager.hybridConnections()).thenReturn(connections);
    when(connections.listByNamespace(RESOURCE_GROUP, RESOURCE_NAME)).thenReturn(values);

    assertThat(rule.hasDependentResources(resourceToDelete), equalTo(expectedResult));
  }

  private static Stream<Arguments> hasDependentResourcesScenario() {
    return Stream.of(
        Arguments.of(new ArrayList<HybridConnection>(), false),
        Arguments.of(List.of(mock(HybridConnection.class)), true),
        Arguments.of(List.of(mock(HybridConnection.class), mock(HybridConnection.class)), true));
  }
}
