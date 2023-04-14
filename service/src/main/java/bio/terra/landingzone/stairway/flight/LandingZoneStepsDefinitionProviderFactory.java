package bio.terra.landingzone.stairway.flight;

public class LandingZoneStepsDefinitionProviderFactory {
    //TODO: make enum
    public final static String CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE = "cromwell";
    public final static String PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME = "protectedData";

    public static StepsDefinitionProvider create(String type) {
        return switch (type) {
            case CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE -> new CromwellStepsDefinitionProvider();
            case PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME -> new ProtectedDataStepsDefinitionProvider();
            default -> throw new RuntimeException("Unknown step definition provider type");
        };
    }
}
