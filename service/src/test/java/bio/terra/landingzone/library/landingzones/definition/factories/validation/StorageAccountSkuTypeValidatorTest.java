package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;
import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class StorageAccountSkuTypeValidatorTest {
  private StorageAccountSkuTypeValidator validator;

  @Mock ParametersResolver mockParametersResolver;

  @BeforeEach
  void setup() {
    validator = new StorageAccountSkuTypeValidator();
  }

  @ParameterizedTest
  @MethodSource("validStorageAccountSkyTypeProvider")
  void testSuccessValidation(String storageAccountSkuType) {
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.STORAGE_ACCOUNT_SKU_TYPE.name()))
        .thenReturn(storageAccountSkuType);
    assertDoesNotThrow(
        () -> validator.validate(mockParametersResolver), "Validation should be successful.");
  }

  @ParameterizedTest
  @MethodSource("invalidStorageAccountSkyTypeProvider")
  void testFailureValidation(String storageAccountSkuType) {
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.STORAGE_ACCOUNT_SKU_TYPE.name()))
        .thenReturn(storageAccountSkuType);

    assertThrows(
        InvalidInputParameterException.class, () -> validator.validate(mockParametersResolver));
  }

  private static Stream<Arguments> validStorageAccountSkyTypeProvider() {
    return Stream.of(
        Arguments.of(StorageAccountSkuType.STANDARD_LRS.name().toString()),
        Arguments.of(StorageAccountSkuType.STANDARD_GRS.name().toString()),
        Arguments.of(StorageAccountSkuType.STANDARD_RAGRS.name().toString()),
        Arguments.of(StorageAccountSkuType.STANDARD_ZRS.name().toString()),
        Arguments.of(StorageAccountSkuType.PREMIUM_LRS.name().toString()));
  }

  private static Stream<Arguments> invalidStorageAccountSkyTypeProvider() {
    return Stream.of(
        Arguments.of("PREMIUM_ZRS"),
        Arguments.of("STANDARD_GZRS"),
        Arguments.of("STANDARD_RAGZRS"),
        Arguments.of("notSupportedValue"),
        Arguments.of("fakeValue"));
  }
}
