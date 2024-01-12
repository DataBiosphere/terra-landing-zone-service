package bio.terra.landingzone.library.landingzones.definition.factories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.landingzone.library.configuration.LandingZoneAzureRegionConfiguration;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.ParametersResolverProvider;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ParametersResolverProviderTest {
  private final String REGION = "eastus";
  private final LandingZoneAzureRegionConfiguration regionConfiguration =
      new LandingZoneAzureRegionConfiguration();
  private final ParametersResolverProvider provider =
      new ParametersResolverProvider(regionConfiguration);
  private Map<String, String> inputParameters;
  private Map<String, String> regionalDefaults;
  private String testingKey;

  @BeforeEach
  void setUp() {
    inputParameters = new HashMap<>();
    regionalDefaults = new HashMap<>();
    regionConfiguration.setDefaultParameters(Map.of(REGION, regionalDefaults));
    assertThat(
        "nonempty base default parameters assumed throughout test suite",
        LandingZoneDefaultParameters.get().size() > 0);
    // grab first base default parameter key, doesn't matter which
    testingKey = LandingZoneDefaultParameters.get().keySet().iterator().next();
  }

  @Test
  void create_throwsAnExceptionIfRegionIsNull() {
    assertThrows(IllegalArgumentException.class, () -> provider.create(new HashMap<>(), null));
  }

  @Test
  void getValue_inputParametersOverrideRegionAndBaseDefaults() {
    regionalDefaults.put(testingKey, "FOO");
    inputParameters.put(testingKey, "BAR");
    var resolver = provider.create(inputParameters, REGION);

    assertThat(resolver.getValue(testingKey), equalTo("BAR"));
  }

  @Test
  void getValue_regionParametersOverrideBaseDefaults() {
    regionalDefaults.put(testingKey, "FOO");
    var resolver = provider.create(inputParameters, REGION);

    assertThat(resolver.getValue(testingKey), equalTo("FOO"));
  }

  @Test
  void getValue_fallsBackToBaseDefault() {
    var resolver = provider.create(inputParameters, REGION);

    assertThat(
        resolver.getValue(testingKey), equalTo(LandingZoneDefaultParameters.get().get(testingKey)));
  }

  @Test
  void getValue_usesGlobalDefaultsIfRegionNotFound() {
    var resolver = provider.create(inputParameters, "nonexistent region");

    assertThat(
        resolver.getValue(testingKey), equalTo(LandingZoneDefaultParameters.get().get(testingKey)));
  }

  @Test
  void getValue_fallsBackToBaseDefaultIfNoParametersOrRegionProvided() {
    var resolver = provider.create(null, REGION);

    assertThat(
        resolver.getValue(testingKey), equalTo(LandingZoneDefaultParameters.get().get(testingKey)));
  }

  @Test
  void getValue_fallsBackToBaseDefaultIfRegionalConfigNotFound() {
    regionConfiguration.setDefaultParameters(null);
    var resolver = provider.create(null, REGION);

    assertThat(
        resolver.getValue(testingKey), equalTo(LandingZoneDefaultParameters.get().get(testingKey)));
  }

  @Test
  void getValue_returnsEmptyStringIfKeyNotFound() {
    var resolver = provider.create(inputParameters, REGION);

    assertThat(resolver.getValue("nonexistent key"), equalTo(""));
  }
}
