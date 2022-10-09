package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_STORAGE_ACCOUNT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.storage.fluent.models.ListContainerItemInner;
import com.azure.resourcemanager.storage.models.BlobContainers;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StorageAccountHasContainersTestFixture extends BaseDependencyRuleFixture {

  private StorageAccountHasContainers rule;

  @BeforeEach
  void setUp() {
    rule = new StorageAccountHasContainers(armManagers);
  }

  @Test
  void getExpectedType_returnsExpectedResult() {
    assertThat(rule.getExpectedType(), equalTo(AZURE_STORAGE_ACCOUNT_TYPE));
  }

  @ParameterizedTest
  @MethodSource("hasDependentResourcesScenario")
  void hasDependentResources_containerScenarios(
      List<ListContainerItemInner> containers, boolean expectedResult) {
    setUpResourceToDelete();
    BlobContainers blobContainers = mock(BlobContainers.class);
    when(azureResourceManager.storageBlobContainers()).thenReturn(blobContainers);
    PagedIterable<ListContainerItemInner> blobContainerList = toMockPageIterable(containers);
    when(blobContainers.list(RESOURCE_GROUP, RESOURCE_NAME)).thenReturn(blobContainerList);

    assertThat(rule.hasDependentResources(resourceToDelete), equalTo(expectedResult));
  }

  private static Stream<Arguments> hasDependentResourcesScenario() {
    return Stream.of(
        Arguments.of(new ArrayList<ListContainerItemInner>(), false),
        Arguments.of(List.of(mock(ListContainerItemInner.class)), true),
        Arguments.of(
            List.of(mock(ListContainerItemInner.class), mock(ListContainerItemInner.class)), true));
  }
}
