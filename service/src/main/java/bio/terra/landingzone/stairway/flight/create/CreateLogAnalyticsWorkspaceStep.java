package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CreateLogAnalyticsWorkspaceStep implements Step {
  public static final String LOG_ANALYTICS_WORKSPACE_ID = "LOG_ANALYTICS_WORKSPACE_ID";

  private final LandingZoneAzureConfiguration landingZoneAzureConfiguration;

  private final String landingZoneId;

  public CreateLogAnalyticsWorkspaceStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration) {
    this.landingZoneAzureConfiguration = landingZoneAzureConfiguration;
    landingZoneId = UUID.randomUUID().toString();
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    FlightMap workingMap = context.getWorkingMap();
    FlightUtils.validateRequiredEntries(workingMap, LandingZoneFlightMapKeys.BILLING_PROFILE);
    var billingProfile =
        workingMap.get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);

    var landingZoneTarget = LandingZoneTarget.fromBillingProfile(billingProfile);

    // TODO: check if we can arrange all these dependencies in a different way
    // Most like we need the same setup for different steps. At least we need armManagers.
    var azureProfile =
        new AzureProfile(
            landingZoneTarget.azureTenantId(),
            landingZoneTarget.azureSubscriptionId(),
            AzureEnvironment.AZURE);
    var tokenCredentials =
        new ClientSecretCredentialBuilder()
            .clientId(landingZoneAzureConfiguration.getManagedAppClientId())
            .clientSecret(landingZoneAzureConfiguration.getManagedAppClientSecret())
            .tenantId(landingZoneAzureConfiguration.getManagedAppTenantId())
            .build();
    var armManagers = LandingZoneManager.createArmManagers(tokenCredentials, azureProfile);

    ResourceGroup resourceGroup =
        armManagers
            .azureResourceManager()
            .resourceGroups()
            .getByName(landingZoneTarget.azureResourceGroupId());

    var logAnalyticsWorkspace =
        armManagers
            .logAnalyticsManager()
            .workspaces()
            .define(
                "uniqueLogAnalyticsName"
                //                definitionContext
                //                    .resourceNameGenerator()
                //
                // .nextName(ResourceNameGenerator.MAX_LOG_ANALYTICS_WORKSPACE_NAME_LENGTH)
                )
            .withRegion(resourceGroup.region())
            .withExistingResourceGroup(resourceGroup.id())
            .withRetentionInDays(
                context
                    .getInputParameters()
                    .get(
                        CromwellBaseResourcesFactory.ParametersNames.AUDIT_LOG_RETENTION_DAYS
                            .name(),
                        Integer.class))
            .withTags(
                Map.of(
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId,
                    LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                    ResourcePurpose.SHARED_RESOURCE.toString()))
            .create();

    //        var deployed =
    //                definitionContext
    //                        .deployment()
    //                        .definePrerequisites()
    //                        .withResourceWithPurpose(logAnalyticsWorkspace,
    // ResourcePurpose.SHARED_RESOURCE)
    //                        .deploy();

    // String logAnalyticsWorkspaceId = deployed.get(0).resourceId();
    String logAnalyticsWorkspaceId = logAnalyticsWorkspace.id();

    context.getWorkingMap().put(LOG_ANALYTICS_WORKSPACE_ID, logAnalyticsWorkspaceId);

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }

  private String getResourceId(List<DeployedResource> prerequisites, String resourceType) {
    return prerequisites.stream()
        .filter(deployedResource -> Objects.equals(deployedResource.resourceType(), resourceType))
        .findFirst()
        .get()
        .resourceId();
  }
}
