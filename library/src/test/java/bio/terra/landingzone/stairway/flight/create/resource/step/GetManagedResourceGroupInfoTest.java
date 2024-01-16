package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class GetManagedResourceGroupInfoTest extends BaseStepTest {

  @Mock private ResourceGroups mockResourceGroups;
  @Mock private ResourceGroup mockResourceGroup;

  private GetManagedResourceGroupInfo getManagedResourceGroupInfo;

  @BeforeEach
  void setup() {
    getManagedResourceGroupInfo = new GetManagedResourceGroupInfo();
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String resourceGroupName = "resourceGroupName";
    final String resourceGroupRegionName = "resourceGroupRegionName";
    var billingProfile = ResourceStepFixture.createDefaultProfileModel();

    setupFlightContext(
        mockFlightContext,
        Map.of(LandingZoneFlightMapKeys.BILLING_PROFILE, billingProfile),
        new HashMap<>());
    setupArmManagersForDoStep(resourceGroupName, resourceGroupRegionName);
    FlightMap spyWorkingMap = spy(mockFlightContext.getWorkingMap());

    StepResult stepResult = getManagedResourceGroupInfo.doStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockResourceGroups, times(1)).getByName(billingProfile.getManagedResourceGroupId());
    // verify that mrg info has been saved for future use
    var requiredWorkingMapElement =
        spyWorkingMap.get(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY, TargetManagedResourceGroup.class);
    assertNotNull(requiredWorkingMapElement);
    assertThat(requiredWorkingMapElement.name(), equalTo(resourceGroupName));
    assertThat(requiredWorkingMapElement.region(), equalTo(resourceGroupRegionName));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    // undo doesn't do anything and always return success. let's make sure we catch any future
    // changes.
    assertThat(
        getManagedResourceGroupInfo.undoStep(mockFlightContext),
        equalTo(StepResult.getStepResultSuccess()));
  }

  private void setupArmManagersForDoStep(String resourceGroupName, String resourceGroupRegionName) {
    when(mockResourceGroup.name()).thenReturn(resourceGroupName);
    when(mockResourceGroup.regionName()).thenReturn(resourceGroupRegionName);
    when(mockResourceGroups.getByName(anyString())).thenReturn(mockResourceGroup);
    when(mockAzureResourceManager.resourceGroups()).thenReturn(mockResourceGroups);
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
  }
}
