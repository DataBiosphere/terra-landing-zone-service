package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_VNET_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.management.ResourceToDelete;
import com.azure.resourcemanager.resources.models.GenericResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceDependencyDeleteRuleTest {

  @Mock private ResourceDependencyDeleteRule dependencyDeleteRule;

  @Mock private GenericResource genericResource;

  @Mock private ResourceToDelete resourceToDelete;

  @BeforeEach
  void setUp() {
    when(dependencyDeleteRule.applyRule(any())).thenCallRealMethod();
    when(resourceToDelete.resource()).thenReturn(genericResource);
    when(genericResource.type()).thenReturn(AZURE_VNET_TYPE);
    when(dependencyDeleteRule.getExpectedType()).thenReturn(AZURE_VNET_TYPE);
  }

  @Test
  void applyRule_resourceIsExpectedTypeWithDependency_isNotDeletable() {
    when(dependencyDeleteRule.hasDependentResources(resourceToDelete)).thenReturn(true);

    var rule = dependencyDeleteRule.applyRule(resourceToDelete);

    assertThat(rule.isDeletable(), equalTo(false));
  }

  @Test
  void applyRule_resourceIsExpectedTypeWithNoDependency_isDeletable() {
    when(dependencyDeleteRule.hasDependentResources(resourceToDelete)).thenReturn(false);

    var rule = dependencyDeleteRule.applyRule(resourceToDelete);

    assertThat(rule.isDeletable(), equalTo(true));
  }

  @Test
  void applyRule_resourceIsNotExpectedType_isDeletable() {
    when(dependencyDeleteRule.getExpectedType()).thenReturn("WrongType");

    var rule = dependencyDeleteRule.applyRule(resourceToDelete);

    assertThat(rule.isDeletable(), equalTo(true));
  }
}
