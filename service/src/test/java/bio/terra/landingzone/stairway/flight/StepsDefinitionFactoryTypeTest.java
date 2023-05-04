package bio.terra.landingzone.stairway.flight;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("unit")
class StepsDefinitionFactoryTypeTest {

  @ParameterizedTest
  @MethodSource("getCorrectValues")
  void testCorrectValuesInDifferentCases(String value) {
    assertNotNull(StepsDefinitionFactoryType.fromString(value));
  }

  @ParameterizedTest
  @MethodSource("getInCorrectValues")
  void testIncorrectValuesReturnNull(String value) {
    assertNull(StepsDefinitionFactoryType.fromString(value));
  }

  private static Stream<Arguments> getCorrectValues() {
    return Stream.of(
        Arguments.of("CromwellBaseResourcesFactory"),
        Arguments.of("ProtectedDataResourcesFactory"),
        Arguments.of("cromwellbaseresourcesfactory"),
        Arguments.of("protecteddataresourcesfactory"),
        Arguments.of("CROMWELLBASERESOURCESFACTORY"),
        Arguments.of("PROTECTEDDATARESOURCESFACTORY"));
  }

  private static Stream<Arguments> getInCorrectValues() {
    return Stream.of(
        Arguments.of("Cromwell_Base_Resources_Factory"),
        Arguments.of("Protected_Data_Resources_Factory"),
        Arguments.of("newfactory"),
        Arguments.of("testfactory"),
        Arguments.of("CROMWELL_BASE_RESOURCES_FACTORY"),
        Arguments.of("PROTECTED_DATA_RESOURCES_FACTORY"));
  }
}
