package bio.terra.landingzone.library.landingzones.definition.factories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class LandingZoneDefinitionProviderImplTest {
  private LandingZoneDefinitionProviderImpl provider;

  private ArmManagers armManagers;

  @BeforeEach
  void setUp() {
    armManagers = new ArmManagers(null, null, null, null);
    provider = new LandingZoneDefinitionProviderImpl(armManagers);
  }

  @Test
  void createDefinitionFactory_providerCreatesTestFactory() {
    var factory = provider.createDefinitionFactory(TestLandingZoneFactory.class);

    assertThat(factory, instanceOf(TestLandingZoneFactory.class));
  }
}
