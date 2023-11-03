package bio.terra.landingzone.library.landingzones.management;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.management.deleterules.DeleteRule;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DeleteRulesVerifierTest {

  @Mock private DeleteRule deleteRule1;
  @Mock private DeleteRule deleteRule2;

  @Mock private ResourceToDelete resourceToDelete1;

  @Mock private ResourceToDelete resourceToDelete2;

  @Mock private DeleteRuleResult deleteRuleResult1;

  @Mock private DeleteRuleResult deleteRuleResult2;

  private DeleteRulesVerifier deleteRulesVerifier;

  @BeforeEach
  void setUp() {
    List<DeleteRule> deleteRules = List.of(deleteRule1, deleteRule2);
    deleteRulesVerifier = new DeleteRulesVerifier(deleteRules);
  }

  @Test
  void checkIfRulesAllowDelete_emptyRuleList_noException() throws LandingZoneRuleDeleteException {
    List<ResourceToDelete> resourcesToDelete = List.of(resourceToDelete1, resourceToDelete2);

    var ruleVerifier = new DeleteRulesVerifier(new ArrayList<>());

    ruleVerifier.checkIfRulesAllowDelete(resourcesToDelete);
  }

  @Test
  void checkIfRulesAllowDelete_rulesPass_noException() throws LandingZoneRuleDeleteException {
    List<ResourceToDelete> resourcesToDelete = List.of(resourceToDelete1, resourceToDelete2);

    when(deleteRuleResult1.isDeletable()).thenReturn(true);
    when(deleteRuleResult2.isDeletable()).thenReturn(true);
    when(deleteRule1.applyRule(any())).thenReturn(deleteRuleResult1);
    when(deleteRule2.applyRule(any())).thenReturn(deleteRuleResult2);

    deleteRulesVerifier.checkIfRulesAllowDelete(resourcesToDelete);
  }

  @Test
  void checkIfRulesAllowDelete_1ruleFails_throwsExceptionAndReason() {
    List<ResourceToDelete> resourcesToDelete = List.of(resourceToDelete1, resourceToDelete2);

    when(deleteRuleResult1.isDeletable()).thenReturn(true);
    when(deleteRuleResult2.isDeletable()).thenReturn(false);
    when(deleteRuleResult2.reason()).thenReturn("failed 2");
    when(deleteRule1.applyRule(any())).thenReturn(deleteRuleResult1);
    when(deleteRule2.applyRule(any())).thenReturn(deleteRuleResult2);

    var ex =
        assertThrows(
            LandingZoneRuleDeleteException.class,
            () -> deleteRulesVerifier.checkIfRulesAllowDelete(resourcesToDelete));

    assertThat(ex.getMessage(), containsString("failed 2"));
  }

  @Test
  void checkIfRulesAllowDelete_2ruleFail_throwsExceptionAndReasons() {
    List<ResourceToDelete> resourcesToDelete = List.of(resourceToDelete1, resourceToDelete2);

    when(deleteRuleResult1.isDeletable()).thenReturn(false);
    when(deleteRuleResult2.isDeletable()).thenReturn(false);
    when(deleteRuleResult1.reason()).thenReturn("failed 1");
    when(deleteRuleResult2.reason()).thenReturn("failed 2");
    when(deleteRule1.applyRule(any())).thenReturn(deleteRuleResult1);
    when(deleteRule2.applyRule(any())).thenReturn(deleteRuleResult2);

    var ex =
        assertThrows(
            LandingZoneRuleDeleteException.class,
            () -> deleteRulesVerifier.checkIfRulesAllowDelete(resourcesToDelete));

    assertThat(ex.getMessage(), containsString("failed 1"));
    assertThat(ex.getMessage(), containsString("failed 2"));
  }
}
