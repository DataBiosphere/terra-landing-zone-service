package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;
import com.azure.resourcemanager.storage.models.SkuName;
import java.util.Optional;
import java.util.Set;

public class StorageAccountSkuTypeValidator implements InputParameterValidator {
  private final StringBuilder sbErrors = new StringBuilder();

  @Override
  public void validate(ParametersResolver parametersResolver)
      throws InvalidInputParameterException {
    Optional<String> storageAccountSkuValueValidationMessage =
        validateStorageAccountSkuType(parametersResolver);
    storageAccountSkuValueValidationMessage.ifPresent(s -> sbErrors.append(s).append(" "));

    if (!sbErrors.isEmpty()) {
      throw new InvalidInputParameterException(sbErrors.toString());
    }
  }

  private Optional<String> validateStorageAccountSkuType(ParametersResolver parametersResolver) {
    String value =
        parametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.STORAGE_ACCOUNT_SKU_TYPE.name());

    Set<String> acceptedValues = getAcceptedValues();
    String errorMessage =
        buildErrorMessage(
            CromwellBaseResourcesFactory.ParametersNames.STORAGE_ACCOUNT_SKU_TYPE,
            String.format(" Accepted values: [%s].", String.join(",", acceptedValues)));

    return acceptedValues.contains(value) ? Optional.empty() : Optional.of(errorMessage);
  }

  private Set<String> getAcceptedValues() {
    // possible values are based on StorageAccountSkuType which is build based on SkuName.
    // since StorageAccountSkuType is a subset of SkuName returning here only those values which
    // can be mapped to a certain value in StorageAccountSkuType
    return Set.of(
        SkuName.STANDARD_LRS.toString(),
        SkuName.STANDARD_GRS.toString(),
        SkuName.STANDARD_RAGRS.toString(),
        SkuName.STANDARD_ZRS.toString(),
        SkuName.PREMIUM_LRS.toString());
  }
}
