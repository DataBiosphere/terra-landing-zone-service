package bio.terra.landingzone.library.landingzones.management;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.landingzone.library.landingzones.definition.factories.TestLandingZoneFactory;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class LandingZoneManagerIntegrationTest {
  @Test
  void listDefinitionFactories_testFactoryIsListed() {
    var factories = LandingZoneManager.listDefinitionFactories();
    FactoryDefinitionInfo testFactory =
        new FactoryDefinitionInfo(
            TestLandingZoneFactory.LZ_NAME,
            TestLandingZoneFactory.LZ_DESC,
            TestLandingZoneFactory.DEFINITION_NAME,
            List.of(DefinitionVersion.V1));

    assertThat(factories, hasItem(testFactory));
  }
}
