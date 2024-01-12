package bio.terra.landingzone.stairway.flight.create.resource.step;

import static java.util.Map.entry;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.monitor.models.DiagnosticSetting;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAksLogSettingsStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateAksLogSettingsStep.class);

  // set the retention days to zero to handle azure API deprecation
  // see
  // https://learn.microsoft.com/en-us/azure/azure-monitor/essentials/migrate-to-azure-storage-lifecycle-policy
  private static final int RETENTION_DAYS = 0;
  private static final Map<String, Integer> AKS_LOGS_TO_CAPTURE =
      Map.ofEntries(
          entry("kube-apiserver", RETENTION_DAYS),
          entry("kube-audit", RETENTION_DAYS),
          entry("kube-audit-admin", RETENTION_DAYS),
          entry("kube-controller-manager", RETENTION_DAYS),
          entry("kube-scheduler", RETENTION_DAYS),
          entry("cluster-autoscaler", RETENTION_DAYS),
          entry("cloud-controller-manager", RETENTION_DAYS),
          entry("guard", RETENTION_DAYS),
          entry("csi-azuredisk-controller", RETENTION_DAYS),
          entry("csi-azurefile-controller", RETENTION_DAYS),
          entry("csi-snapshot-controller", RETENTION_DAYS));
  private static final Map<String, Integer> AKS_METRICS_TO_CAPTURE =
      Map.ofEntries(entry("AllMetrics", RETENTION_DAYS));
  private static final int METRICS_GRANULARITY_SECONDS = 30;

  private final LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration;

  public CreateAksLogSettingsStep(
      ArmManagers armManagers,
      ResourceNameProvider resourceNameProvider,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    super(armManagers, resourceNameProvider);
    this.landingZoneProtectedDataConfiguration = landingZoneProtectedDataConfiguration;
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var aksId = getParameterOrThrow(context.getWorkingMap(), CreateAksStep.AKS_ID, String.class);

    String lzRegion = getMRGRegionName(context);
    if (!landingZoneProtectedDataConfiguration
        .getLongTermStorageAccountIds()
        .containsKey(lzRegion)) {
      throw new MissingRequiredFieldsException(
          "No matching long term storage account for region " + lzRegion);
    }
    // get long-term storage account based on region
    var storageAccountId =
        landingZoneProtectedDataConfiguration.getLongTermStorageAccountIds().get(lzRegion);
    var aksLogSettingsName = resourceNameProvider.getName(getResourceType());
    var aksDiagnosticSettingsConfiguration =
        armManagers
            .monitorManager()
            .diagnosticSettings()
            .define(aksLogSettingsName)
            .withResource(aksId)
            .withStorageAccount(storageAccountId);
    aksDiagnosticSettingsConfiguration = setupLogConfiguration(aksDiagnosticSettingsConfiguration);
    aksDiagnosticSettingsConfiguration =
        setupMetricsConfiguration(aksDiagnosticSettingsConfiguration);

    var aksDiagnosticSettings = aksDiagnosticSettingsConfiguration.create();
    logger.info(
        RESOURCE_CREATED, getResourceType(), aksDiagnosticSettings.id(), getMRGName(context));
  }

  private DiagnosticSetting.DefinitionStages.WithCreate setupLogConfiguration(
      DiagnosticSetting.DefinitionStages.WithCreate aksPartiallySetup) {
    for (Map.Entry<String, Integer> pair : AKS_LOGS_TO_CAPTURE.entrySet()) {
      aksPartiallySetup = aksPartiallySetup.withLog(pair.getKey(), pair.getValue());
    }
    return aksPartiallySetup;
  }

  private DiagnosticSetting.DefinitionStages.WithCreate setupMetricsConfiguration(
      DiagnosticSetting.DefinitionStages.WithCreate aksPartiallySetup) {
    for (Map.Entry<String, Integer> pair : AKS_METRICS_TO_CAPTURE.entrySet()) {
      aksPartiallySetup =
          aksPartiallySetup.withMetric(
              pair.getKey(), Duration.ofSeconds(METRICS_GRANULARITY_SECONDS), pair.getValue());
    }
    return aksPartiallySetup;
  }

  @Override
  protected void deleteResource(String resourceId) {
    // do nothing
  }

  @Override
  protected String getResourceType() {
    return "AKSLogSettings";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.empty();
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_DIAGNOSTIC_SETTING_NAME_LENGTH));
  }
}
