package bio.terra.landingzone.library.landingzones;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreatePostgresqlDNSStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateVirtualNetworkLinkStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateVnetStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetManagedResourceGroupInfo;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepStatus;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Simple test that allows rapid iteration on resource steps in a real Azure resource group
 *
 * <p>This is useful to exercise a particular resource step or steps, and if you do not want to run
 * the full LZ definition integration test suite.
 */
@Tag("integration")
public class AdHocIntegrationTest extends LandingZoneTestFixture {
  @Test
  void testStep() throws InterruptedException {
    var lzid = UUID.randomUUID();
    FlightMap inputParameters = new FlightMap();
    ProfileModel profile = new ProfileModel();

    inputParameters.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, lzid);
    inputParameters.put(LandingZoneFlightMapKeys.BILLING_PROFILE, profile);
    var flightContext = mock(FlightContext.class);
    when(flightContext.getInputParameters()).thenReturn(inputParameters);

    FlightMap workingMap = new FlightMap();
    workingMap.put(
        GetManagedResourceGroupInfo.TARGET_MRG_KEY,
        new TargetManagedResourceGroup(resourceGroup.name(), resourceGroup.regionName()));
    when(flightContext.getWorkingMap()).thenReturn(workingMap);

    ParametersResolver parametersResolver =
        new ParametersResolver(Map.of(), LandingZoneDefaultParameters.get());

    LandingZoneManager.createArmManagers(tokenCredential, azureProfile);
    var rnp = new ResourceNameProvider(lzid);

    testSteps(flightContext, parametersResolver, rnp);
  }

  private void testSteps(
      FlightContext flightContext, ParametersResolver parametersResolver, ResourceNameProvider rnp)
      throws InterruptedException {
    // put your step testing code here, i.e., something like:
    CreateVnetStep cvs = new CreateVnetStep(armManagers, parametersResolver, rnp);
    var result = cvs.doStep(flightContext);
    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));

    CreatePostgresqlDNSStep dns = new CreatePostgresqlDNSStep(armManagers, parametersResolver, rnp);
    result = dns.doStep(flightContext);
    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));

    CreateVirtualNetworkLinkStep vnl =
        new CreateVirtualNetworkLinkStep(armManagers, parametersResolver, rnp);
    result = vnl.doStep(flightContext);
    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
  }
}
