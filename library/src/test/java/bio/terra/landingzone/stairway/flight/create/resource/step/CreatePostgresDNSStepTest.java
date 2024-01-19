package bio.terra.landingzone.stairway.flight.create.resource.step;

import static bio.terra.landingzone.stairway.flight.create.resource.step.CreatePostgresqlDNSStep.POSTGRES_DNS_SUFFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.privatedns.models.PrivateDnsZone;
import com.azure.resourcemanager.privatedns.models.PrivateDnsZones;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class CreatePostgresDNSStepTest extends BaseStepTest {
  private CreatePostgresqlDNSStep testStep;
  @Mock private PrivateDnsZones mockPrivateDnsZones;
  @Mock private PrivateDnsZone.DefinitionStages.Blank mockDefine;
  @Mock private PrivateDnsZone.DefinitionStages.WithCreate mockWithCreate;
  @Mock private PrivateDnsZone mockPrivateDnsZone;

  @BeforeEach
  void setup() {
    testStep = new CreatePostgresqlDNSStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String resourceName = UUID.randomUUID().toString();

    when(mockResourceNameProvider.getName(testStep.getResourceType())).thenReturn(resourceName);

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg));
    setupArmManagersForDoStep(resourceName, mrg);

    var dnsId = UUID.randomUUID().toString();
    when(mockPrivateDnsZone.id()).thenReturn(dnsId);

    var stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        mockFlightContext
            .getWorkingMap()
            .get(CreatePostgresqlDNSStep.POSTGRESQL_DNS_ID, String.class),
        equalTo(dnsId));

    var tags = tagsCaptor.getValue();
    assertNotNull(tags);
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()),
        equalTo(LANDING_ZONE_ID.toString()));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    setupFlightContext(mockFlightContext, inputParameters, Map.of());

    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var resourceId = "resourceId";
    var workingMap =
        Map.of(
            CreatePostgresqlDNSStep.POSTGRESQL_DNS_ID,
            resourceId,
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            ResourceStepFixture.createDefaultMrg());
    setupFlightContext(mockFlightContext, Map.of(), workingMap);

    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.privateDnsZones()).thenReturn(mockPrivateDnsZones);

    var stepResult = testStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockPrivateDnsZones).deleteById(resourceId);
  }

  private void setupArmManagersForDoStep(String dnsZoneName, TargetManagedResourceGroup mrg) {
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.privateDnsZones()).thenReturn(mockPrivateDnsZones);
    when(mockPrivateDnsZones.define(dnsZoneName + POSTGRES_DNS_SUFFIX)).thenReturn(mockDefine);
    when(mockDefine.withExistingResourceGroup(mrg.name())).thenReturn(mockWithCreate);
    when(mockWithCreate.withTags(tagsCaptor.capture())).thenReturn(mockWithCreate);
    when(mockWithCreate.create()).thenReturn(mockPrivateDnsZone);
  }
}
