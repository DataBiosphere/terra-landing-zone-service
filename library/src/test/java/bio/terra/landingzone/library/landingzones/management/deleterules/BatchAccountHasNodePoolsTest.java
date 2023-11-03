package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_BATCH_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.batch.models.Pool;
import com.azure.resourcemanager.batch.models.Pools;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BatchAccountHasNodePoolsTest extends BaseDependencyRuleFixture {

  private BatchAccountHasNodePools rule;

  @BeforeEach
  void setUp() {
    rule = new BatchAccountHasNodePools(armManagers);
  }

  @Test
  void getExpectedType_returnsExpectedType() {
    assertThat(rule.getExpectedType(), equalTo(AZURE_BATCH_TYPE));
  }

  @ParameterizedTest
  @MethodSource("hasDependentResourcesScenario")
  void hasDependentResources(List<Pool> poolList, boolean expectedResult) {
    setUpResourceToDelete();
    Pools pools = mock(Pools.class);
    when(batchManager.pools()).thenReturn(pools);
    PagedIterable<Pool> values = toMockPageIterable(poolList);
    when(pools.listByBatchAccount(RESOURCE_GROUP, RESOURCE_NAME)).thenReturn(values);

    assertThat(rule.hasDependentResources(resourceToDelete), equalTo(expectedResult));
  }

  private static Stream<Arguments> hasDependentResourcesScenario() {
    return Stream.of(
        Arguments.of(new ArrayList<>(), false),
        Arguments.of(List.of(mock(Pool.class), mock(Pool.class)), true),
        Arguments.of(List.of(mock(Pool.class)), true));
  }
}
