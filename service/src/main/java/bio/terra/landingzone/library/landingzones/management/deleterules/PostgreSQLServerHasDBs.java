package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_POSTGRESQL_SERVER_TYPE;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.ResourceToDelete;
import java.util.List;

public class PostgreSQLServerHasDBs extends ResourceDependencyDeleteRule {
  public PostgreSQLServerHasDBs(ArmManagers armManagers) {
    super(armManagers);
  }

  private static final List<String> SYS_DB_NAMES =
      List.of("postgres", "azure_sys", "azure_maintenance");

  @Override
  public String getExpectedType() {
    return AZURE_POSTGRESQL_SERVER_TYPE;
  }

  @Override
  public boolean hasDependentResources(ResourceToDelete resourceToDelete) {
    return armManagers
        .postgreSqlManager()
        .databases()
        .listByServer(
            resourceToDelete.resource().resourceGroupName(), resourceToDelete.resource().name())
        .stream()
        .anyMatch(d -> SYS_DB_NAMES.stream().noneMatch(n -> n.equalsIgnoreCase(d.name())));
  }
}
