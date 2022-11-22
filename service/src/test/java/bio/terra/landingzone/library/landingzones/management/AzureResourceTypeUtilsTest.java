package bio.terra.landingzone.library.landingzones.management;

import static bio.terra.landingzone.library.landingzones.TestUtils.STUB_BATCH_ACCOUNT_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("unit")
class AzureResourceTypeUtilsTest {

  @ParameterizedTest
  @MethodSource("validScenarios")
  void resourceTypeFromResourceId_validResourceIdsProvided(
      String resourceId, String expectedResult) {

    String result = AzureResourceTypeUtils.resourceTypeFromResourceId(resourceId);

    assertThat(result, equalTo(expectedResult));
  }

  private static Stream<Arguments> validScenarios() {
    return Stream.of(
        Arguments.of(STUB_BATCH_ACCOUNT_ID, AzureResourceTypeUtils.AZURE_BATCH_TYPE),
        Arguments.of("/" + STUB_BATCH_ACCOUNT_ID, AzureResourceTypeUtils.AZURE_BATCH_TYPE),
        Arguments.of(STUB_BATCH_ACCOUNT_ID + "/", AzureResourceTypeUtils.AZURE_BATCH_TYPE),
        Arguments.of("/" + STUB_BATCH_ACCOUNT_ID + "/", AzureResourceTypeUtils.AZURE_BATCH_TYPE),
        Arguments.of(
            "  /" + STUB_BATCH_ACCOUNT_ID + "/  ", AzureResourceTypeUtils.AZURE_BATCH_TYPE));
  }

  @ParameterizedTest
  @MethodSource("invalidScenarios")
  void resourceTypeFromResourceId_invalidResourceIdsProvided(
      String resourceId, Class<Throwable> exception) {

    assertThrows(exception, () -> AzureResourceTypeUtils.resourceTypeFromResourceId(resourceId));
  }

  private static Stream<Arguments> invalidScenarios() {
    return Stream.of(
        Arguments.of("", IllegalArgumentException.class),
        Arguments.of(null, IllegalArgumentException.class),
        Arguments.of("s1-s2-s3", IllegalArgumentException.class),
        Arguments.of("s1/s2", IllegalArgumentException.class),
        Arguments.of("s1/s2/s3", IllegalArgumentException.class),
        Arguments.of("s1/s2/s3/s4", IllegalArgumentException.class),
        Arguments.of("s1/s2/s3/s4/s5", IllegalArgumentException.class),
        Arguments.of("s1/s2/s3/s4/s5/s6", IllegalArgumentException.class),
        Arguments.of("s1/s2/s3/s4/s5/s6/s7", IllegalArgumentException.class));
  }
}
