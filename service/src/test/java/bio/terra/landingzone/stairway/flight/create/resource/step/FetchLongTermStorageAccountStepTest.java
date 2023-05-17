package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.utils.ProtectedDataAzureStorageHelper;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.storage.models.StorageAccount;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class FetchLongTermStorageAccountStepTest extends BaseStepTest {

  @Mock ProtectedDataAzureStorageHelper mockStorageHelper;
  @Mock StorageAccount mockStorageAccount;

  @Test
  void doStepSuccess() throws InterruptedException {
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            new TargetManagedResourceGroup("fake_mrg", "fake_mrg_region")));
    when(mockStorageHelper.getMatchingAdminStorageAccounts(any()))
        .thenReturn(List.of(mockStorageAccount));
    var step = new FetchLongTermStorageAccountStep(mockStorageHelper);

    var result = step.doStep(mockFlightContext);

    assertThat(result.isSuccess(), equalTo(true));
  }

  @Test
  void doStepFailure_moreThanOneStorageAccount() throws InterruptedException {
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            new TargetManagedResourceGroup("fake_mrg", "fake_mrg_region")));
    when(mockStorageHelper.getMatchingAdminStorageAccounts(any()))
        .thenReturn(List.of(mockStorageAccount, mockStorageAccount));
    var step = new FetchLongTermStorageAccountStep(mockStorageHelper);

    var result = step.doStep(mockFlightContext);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_FAILURE_FATAL));
  }

  @Test
  void doStepFailure_zeroStorageAccounts() throws InterruptedException {
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            new TargetManagedResourceGroup("fake_mrg", "fake_mrg_region")));
    when(mockStorageHelper.getMatchingAdminStorageAccounts(any())).thenReturn(List.of());
    var step = new FetchLongTermStorageAccountStep(mockStorageHelper);

    var result = step.doStep(mockFlightContext);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_FAILURE_FATAL));
  }

  @Test
  void doStepUndo_success() throws InterruptedException {
    var step = new FetchLongTermStorageAccountStep(null);

    var result = step.undoStep(mockFlightContext);
    assertThat(result.isSuccess(), equalTo(true));
  }
}
