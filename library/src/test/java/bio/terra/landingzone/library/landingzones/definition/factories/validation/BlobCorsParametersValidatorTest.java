package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import static bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.StorageAccountBlobCorsParametersNames;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class BlobCorsParametersValidatorTest {
  private BlobCorsParametersValidator validator;

  @BeforeEach
  void setup() {
    validator = new BlobCorsParametersValidator();
  }

  @Test
  void testDefaultCorsParametersSuccess() {
    var parametersResolver =
        buildParametersResolver(
            STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS_DEFAULT,
            STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS_DEFAULT,
            STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS_DEFAULT,
            STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS_DEFAULT,
            STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE_DEFAULT);
    assertDoesNotThrow(
        () -> validator.validate(parametersResolver), "Validation should be successful.");
  }

  @ParameterizedTest
  @MethodSource("maxAgeValueValidSupplier")
  void testMaxAgeParameterSuccess(String maxAgeValue) {
    var parametersResolver = buildParametersResolver(null, null, null, null, maxAgeValue);
    assertDoesNotThrow(
        () -> validator.validate(parametersResolver), "Validation should be successful.");
  }

  @ParameterizedTest
  @MethodSource("maxAgeValueInvalidSupplier")
  void testMaxAgeParameterFailure(String maxAgeValue) {
    var parametersResolver = buildParametersResolver(null, null, null, null, maxAgeValue);
    Exception e =
        assertThrows(
            InvalidInputParameterException.class, () -> validator.validate(parametersResolver));
    assertTrue(
        e.getMessage()
            .contains(
                StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE.name()));
  }

  @ParameterizedTest
  @MethodSource("allowedMethodsValidSupplier")
  void testAllowedMethodsSuccess(String allowedMethodsValue) {
    var parametersResolver = buildParametersResolver(null, allowedMethodsValue, null, null, null);
    assertDoesNotThrow(
        () -> validator.validate(parametersResolver), "Validation should be successful.");
  }

  @ParameterizedTest
  @MethodSource("allowedMethodsInvalidSupplier")
  void testAllowedMethodsFailure(String allowedMethodsValue) {
    var parametersResolver = buildParametersResolver(null, allowedMethodsValue, null, null, null);
    Exception e =
        assertThrows(
            InvalidInputParameterException.class, () -> validator.validate(parametersResolver));
    assertTrue(
        e.getMessage()
            .contains(
                StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS
                    .name()));
  }

  @ParameterizedTest
  @MethodSource("headersValidSupplier")
  void testExposedHeadersSuccess(String exposedHeadersValue) {
    var parametersResolver = buildParametersResolver(null, null, null, exposedHeadersValue, null);
    assertDoesNotThrow(
        () -> validator.validate(parametersResolver), "Validation should be successful.");
  }

  @ParameterizedTest
  @MethodSource("headersInvalidSupplier")
  void testExposedHeadersFailure(String exposedHeadersValue) {
    var parametersResolver = buildParametersResolver(null, null, null, exposedHeadersValue, null);
    Exception e =
        assertThrows(
            InvalidInputParameterException.class, () -> validator.validate(parametersResolver));
    assertTrue(
        e.getMessage()
            .contains(
                StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS
                    .name()));
  }

  @ParameterizedTest
  @MethodSource("headersValidSupplier")
  void testAllowedHeadersSuccess(String allowedHeadersValue) {
    var parametersResolver = buildParametersResolver(null, null, allowedHeadersValue, null, null);
    assertDoesNotThrow(
        () -> validator.validate(parametersResolver), "Validation should be successful.");
  }

  @ParameterizedTest
  @MethodSource("headersInvalidSupplier")
  void testAllowedHeadersFailure(String allowedHeadersValue) {
    var parametersResolver = buildParametersResolver(null, null, allowedHeadersValue, null, null);
    Exception e =
        assertThrows(
            InvalidInputParameterException.class, () -> validator.validate(parametersResolver));
    assertTrue(
        e.getMessage()
            .contains(
                StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS
                    .name()));
  }

  @ParameterizedTest
  @MethodSource("maxAgeAndAllowedHeadersInvalidSupplier")
  void testMultipleFailure(String maxAgeValue, String allowedHeadersValue) {
    var parametersResolver =
        buildParametersResolver(null, null, allowedHeadersValue, null, maxAgeValue);
    Exception e =
        assertThrows(
            InvalidInputParameterException.class, () -> validator.validate(parametersResolver));
    assertTrue(
        e.getMessage()
            .contains(
                StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE.name()));
    assertTrue(
        e.getMessage()
            .contains(
                StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS
                    .name()));
  }

  ParametersResolver buildParametersResolver(
      String allowedOrigins,
      String allowedMethods,
      String allowedHeaders,
      String exposedHeaders,
      String maxAge) {
    Map<String, String> parameters = new HashMap<>(Map.of(
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS.name(),
        STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS_DEFAULT,
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS.name(),
        STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS_DEFAULT,
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS.name(),
        STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS_DEFAULT,
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS.name(),
        STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS_DEFAULT,
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE.name(),
        STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE_DEFAULT));
    if (allowedOrigins != null) {
      parameters.put(
          StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS.name(),
          allowedOrigins);
    }
    if (allowedMethods != null) {
      parameters.put(
          StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS.name(),
          allowedMethods);
    }
    if (allowedHeaders != null) {
      parameters.put(
          StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS.name(),
          allowedHeaders);
    }
    if (exposedHeaders != null) {
      parameters.put(
          StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS.name(),
          exposedHeaders);
    }
    if (maxAge != null) {
      parameters.put(
          StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE.name(), maxAge);
    }

    return new ParametersResolver(parameters);
  }

  static Stream<Arguments> maxAgeValueValidSupplier() {
    return Stream.of(
        Arguments.of("0"),
        Arguments.of("10"),
        Arguments.of("1234"),
        Arguments.of("123400"),
        Arguments.of("864000"),
        Arguments.of(String.valueOf(Integer.MAX_VALUE)));
  }

  static Stream<Arguments> maxAgeValueInvalidSupplier() {
    return Stream.of(
        Arguments.of("test"),
        Arguments.of("invalid value"),
        Arguments.of("1.0"),
        Arguments.of("-1234"),
        Arguments.of("1234,00"),
        Arguments.of(Integer.MAX_VALUE + "0"),
        Arguments.of(" "),
        Arguments.of(""));
  }

  static Stream<Arguments> allowedMethodsValidSupplier() {
    return Stream.of(
        Arguments.of("POST"),
        Arguments.of("GET"),
        Arguments.of("DELETE"),
        Arguments.of("PATCH"),
        Arguments.of("GET, POST"),
        Arguments.of("GET, MERGE, DELETE"),
        Arguments.of("GET, MERGE, PATCH, PUT, DELETE"));
  }

  static Stream<Arguments> allowedMethodsInvalidSupplier() {
    return Stream.of(
        Arguments.of("POST1"),
        Arguments.of("GET!"),
        Arguments.of("DELETE IT"),
        Arguments.of("PATCH PLEASE"),
        Arguments.of("UNKNOWN"),
        Arguments.of("UNKNOWN, PATCH_PLEASE"),
        Arguments.of("GOT, MERGEIT, WRONG"));
  }

  static Stream<Arguments> headersValidSupplier() {
    return Stream.of(
        Arguments.of("header"),
        Arguments.of("header-with-dash"),
        Arguments.of("header_with_underscore"),
        Arguments.of("header1, header2"),
        Arguments.of("header1, header2", "header3"));
  }

  static Stream<Arguments> headersInvalidSupplier() {
    return Stream.of(
        Arguments.of("test@abc"),
        Arguments.of("test@abc,test2"),
        Arguments.of("he@der1,he@der2"),
        Arguments.of("he>der1,he<der2"),
        Arguments.of("header<"),
        Arguments.of("header>"),
        Arguments.of("head<>er"),
        Arguments.of("head()er"),
        Arguments.of("head[]er"),
        Arguments.of("head{}er"),
        Arguments.of("head;er"),
        Arguments.of("head:er"),
        Arguments.of("head/er"),
        Arguments.of("head\\er"));
  }

  static Stream<Arguments> maxAgeAndAllowedHeadersInvalidSupplier() {
    return Stream.of(
        Arguments.of("-100", "test@abc"),
        Arguments.of("1234523452345234524", "test@abc,test2"),
        Arguments.of("sadasdf", "he@der1,he@der2"),
        Arguments.of("-1", "he>der1,he<der2"),
        Arguments.of("a12", "header<"),
        Arguments.of("1 2", "header>"),
        Arguments.of("1 1 1", "head<>er"),
        Arguments.of("1.0", "head()er"),
        Arguments.of("one", "head[]er"),
        Arguments.of("1 ", "head{}er"),
        Arguments.of("2,000.00", "head;er"),
        Arguments.of("-", "head:er"),
        Arguments.of("22.", "head/er"),
        Arguments.of(".2", "head\\er"));
  }
}
