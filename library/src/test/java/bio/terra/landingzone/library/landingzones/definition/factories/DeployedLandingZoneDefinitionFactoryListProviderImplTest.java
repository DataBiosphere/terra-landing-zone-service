package bio.terra.landingzone.library.landingzones.definition.factories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class DeployedLandingZoneDefinitionFactoryListProviderImplTest {

  @Test
  void listFactories_listContainsTestLandingZoneDefinitionFactory() {
    var provider = new LandingZoneDefinitionFactoryListProviderImpl();

    List<FactoryDefinitionInfo> factoryDefinitionInfos = provider.listFactories();

    assertThat(
        factoryDefinitionInfos,
        hasItem(
            new FactoryDefinitionInfo(
                TestLandingZoneFactory.LZ_NAME,
                TestLandingZoneFactory.LZ_DESC,
                TestLandingZoneFactory.DEFINITION_NAME,
                List.of(DefinitionVersion.V1))));
  }

  @Test
  void listFactoriesClasses_listContainsTestLandingZoneDefinitionFactory() {
    var provider = new LandingZoneDefinitionFactoryListProviderImpl();

    assertThat(provider.listFactoriesClasses(), hasItem(TestLandingZoneFactory.class));
  }
}
