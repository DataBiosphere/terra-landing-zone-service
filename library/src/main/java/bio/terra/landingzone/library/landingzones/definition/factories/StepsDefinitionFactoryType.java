package bio.terra.landingzone.library.landingzones.definition.factories;

public enum StepsDefinitionFactoryType {
  CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE("CromwellBaseResourcesFactory"),
  PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME("ProtectedDataResourcesFactory"),
  REFERENCED_DEFINITION_STEPS_PROVIDER_TYPE("ReferencedResourcesFactory");

  private final String value;

  public String getValue() {
    return value;
  }

  StepsDefinitionFactoryType(String value) {
    this.value = value;
  }

  public static StepsDefinitionFactoryType fromString(String text) {
    for (StepsDefinitionFactoryType type : StepsDefinitionFactoryType.values()) {
      if (type.getValue().equalsIgnoreCase(text)) {
        return type;
      }
    }
    return null;
  }
}
