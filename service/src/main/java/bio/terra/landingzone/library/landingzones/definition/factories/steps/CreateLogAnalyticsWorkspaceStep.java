package bio.terra.landingzone.library.landingzones.definition.factories.steps;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.DefinitionContext;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;
import java.util.List;
import java.util.Objects;

public class CreateLogAnalyticsWorkspaceStep implements Step {
  public static String LOG_ANALYTICS_WORKSPACE_ID = "LOG_ANALYTICS_WORKSPACE_ID";

  private final ArmManagers armManagers;
  private final DefinitionContext definitionContext;

  public CreateLogAnalyticsWorkspaceStep(
      ArmManagers armManagers, DefinitionContext definitionContext) {
    this.armManagers = armManagers;
    this.definitionContext = definitionContext;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var logAnalyticsWorkspace =
        armManagers
            .logAnalyticsManager()
            .workspaces()
            .define(
                definitionContext
                    .resourceNameGenerator()
                    .nextName(ResourceNameGenerator.MAX_LOG_ANALYTICS_WORKSPACE_NAME_LENGTH))
            .withRegion(definitionContext.resourceGroup().region())
            .withExistingResourceGroup(definitionContext.resourceGroup().name())
            .withRetentionInDays(
                context
                    .getInputParameters()
                    .get(
                        CromwellBaseResourcesFactory.ParametersNames.AUDIT_LOG_RETENTION_DAYS
                            .name(),
                        Integer.class));

    var deployed =
        definitionContext
            .deployment()
            .definePrerequisites()
            .withResourceWithPurpose(logAnalyticsWorkspace, ResourcePurpose.SHARED_RESOURCE)
            .deploy();

    String logAnalyticsWorkspaceId = deployed.get(0).resourceId();

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
