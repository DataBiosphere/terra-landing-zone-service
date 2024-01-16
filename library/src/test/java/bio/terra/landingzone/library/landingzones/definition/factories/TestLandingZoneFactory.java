package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.DefinitionHeader;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.stairway.flight.ParametersResolverProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class TestLandingZoneFactory implements StepsDefinitionProvider {
  public static final String DEFINITION_NAME = "TestResourcesFactory";
  public static final String LZ_NAME = "LZ_NAME";
  public static final String LZ_DESC = "LZ_DESC";

  @Override
  public String landingZoneDefinition() {
    return DEFINITION_NAME;
  }

  @Override
  public DefinitionHeader header() {
    return new DefinitionHeader(LZ_NAME, LZ_DESC);
  }

  @Override
  public List<DefinitionVersion> availableVersions() {
    return List.of(DefinitionVersion.V1);
  }

  @Override
  public List<Pair<Step, RetryRule>> get(
      ParametersResolverProvider parametersResolverProvider,
      ResourceNameProvider resourceNameProvider,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    return Collections.emptyList();
  }
}
