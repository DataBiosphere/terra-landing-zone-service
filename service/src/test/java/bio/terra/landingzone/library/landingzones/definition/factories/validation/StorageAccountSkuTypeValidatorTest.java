package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

@Tag("unit")
class StorageAccountSkuTypeValidatorTest {
  private StorageAccountSkuTypeValidator validator;

  @BeforeEach
  void setup() {
    validator = new StorageAccountSkuTypeValidator();
  }
}
