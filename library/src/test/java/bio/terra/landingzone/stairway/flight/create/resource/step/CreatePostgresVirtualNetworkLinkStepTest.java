package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.privatedns.PrivateDnsZoneManager;
import com.azure.resourcemanager.privatedns.fluent.PrivateDnsManagementClient;
import com.azure.resourcemanager.privatedns.fluent.VirtualNetworkLinksClient;
import com.azure.resourcemanager.privatedns.fluent.models.VirtualNetworkLinkInner;
import com.azure.resourcemanager.privatedns.models.PrivateDnsZones;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class CreatePostgresVirtualNetworkLinkStepTest extends BaseStepTest {
  private CreatePostgresVirtualNetworkLinkStep testStep;
  @Mock private PrivateDnsZones mockPrivateDnsZones;
  @Mock private PrivateDnsZoneManager mockDnsManager;
  @Mock private PrivateDnsManagementClient mockServiceClient;
  @Mock private VirtualNetworkLinksClient mockVirtualNetworkLinks;
  @Captor private ArgumentCaptor<VirtualNetworkLinkInner> vnetLinkInnerCaptor;
  @Mock private VirtualNetworkLinkInner mockVirtualNetworkLink;

  @BeforeEach
  void setup() {
    testStep = new CreatePostgresVirtualNetworkLinkStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String resourceName = UUID.randomUUID().toString();
    final String dnsZoneName = UUID.randomUUID().toString();
    final String vnetId = UUID.randomUUID().toString();

    when(mockResourceNameProvider.getName(testStep.getResourceType())).thenReturn(resourceName);

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg,
            CreateVnetStep.VNET_ID,
            vnetId,
            CreatePostgresqlDNSZoneStep.POSTGRESQL_DNS_RESOURCE_KEY,
            LandingZoneResource.builder().resourceName(dnsZoneName).build()));
    setupArmManagersForDoStep(dnsZoneName, mrg, resourceName);

    when(mockVirtualNetworkLink.id()).thenReturn(resourceName);

    var stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        mockFlightContext
            .getWorkingMap()
            .get(CreatePostgresVirtualNetworkLinkStep.VNET_LINK_ID, String.class),
        equalTo(resourceName));

    var tags = vnetLinkInnerCaptor.getValue().tags();
    assertNotNull(tags);
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()),
        equalTo(LANDING_ZONE_ID.toString()));
    assertThat(vnetLinkInnerCaptor.getValue().virtualNetwork().id(), equalTo(vnetId));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var workingMap = new FlightMap();
    var mrg = ResourceStepFixture.createDefaultMrg();
    var dnsZoneName = UUID.randomUUID().toString();
    var linkName = UUID.randomUUID().toString();
    var resourceId =
        String.format(
            "/subscriptions/ffd1069e-e34f-4d87-a8b8-44abfcba39af/resourceGroups/%s/providers/Microsoft.Network/privateDnsZones/%s/virtualNetworkLinks/%s",
            mrg.name(), dnsZoneName, linkName);
    workingMap.put(CreatePostgresVirtualNetworkLinkStep.VNET_LINK_ID, resourceId);
    workingMap.put(GetManagedResourceGroupInfo.TARGET_MRG_KEY, mrg);
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.privateDnsZones()).thenReturn(mockPrivateDnsZones);
    when(mockPrivateDnsZones.manager()).thenReturn(mockDnsManager);
    when(mockDnsManager.serviceClient()).thenReturn(mockServiceClient);
    when(mockServiceClient.getVirtualNetworkLinks()).thenReturn(mockVirtualNetworkLinks);

    var stepResult = testStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockVirtualNetworkLinks).delete(mrg.name(), dnsZoneName, linkName);
  }

  private void setupArmManagersForDoStep(
      String dnsZoneName, TargetManagedResourceGroup mrg, String linkName) {
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.privateDnsZones()).thenReturn(mockPrivateDnsZones);
    when(mockPrivateDnsZones.manager()).thenReturn(mockDnsManager);
    when(mockDnsManager.serviceClient()).thenReturn(mockServiceClient);
    when(mockServiceClient.getVirtualNetworkLinks()).thenReturn(mockVirtualNetworkLinks);
    when(mockVirtualNetworkLinks.createOrUpdate(
            eq(mrg.name()), eq(dnsZoneName), eq(linkName), vnetLinkInnerCaptor.capture()))
        .thenReturn(mockVirtualNetworkLink);
  }
}
