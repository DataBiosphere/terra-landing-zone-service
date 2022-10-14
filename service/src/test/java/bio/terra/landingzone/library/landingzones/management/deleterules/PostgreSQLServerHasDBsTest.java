package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_POSTGRESQL_SERVER_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.resourcemanager.postgresql.models.Database;
import com.azure.resourcemanager.postgresql.models.Databases;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PostgreSQLServerHasDBsTest extends BaseDependencyRuleFixture {
  private PostgreSQLServerHasDBs rule;

  @BeforeEach
  void setUp() {
    rule = new PostgreSQLServerHasDBs(armManagers);
  }

  @Test
  void getExpectedType_returnsExpectedType() {
    assertThat(rule.getExpectedType(), equalTo(AZURE_POSTGRESQL_SERVER_TYPE));
  }

  @ParameterizedTest
  @MethodSource("hasDependentResourcesScenario")
  void hasDependentResources_numberOfDBs(List<Database> dbList, boolean expectedResult) {
    setUpResourceToDelete();
    var databases = mock(Databases.class);
    when(postgreSqlManager.databases()).thenReturn(databases);
    var values = toMockPageIterable(dbList);
    when(databases.listByServer(RESOURCE_GROUP, RESOURCE_NAME)).thenReturn(values);

    assertThat(rule.hasDependentResources(resourceToDelete), equalTo(expectedResult));
  }

  private static Stream<Arguments> hasDependentResourcesScenario() {
    Database db1 = mock(Database.class);
    Database db2 = mock(Database.class);
    when(db1.name()).thenReturn("db1");
    when(db2.name()).thenReturn("db2");
    Database sysdb1 = mock(Database.class);
    Database sysdb2 = mock(Database.class);
    Database sysdb3 = mock(Database.class);

    when(sysdb1.name()).thenReturn("postgres");
    when(sysdb2.name()).thenReturn("azure_sys");
    when(sysdb3.name()).thenReturn("azure_maintenance");

    return Stream.of(
        Arguments.of(new ArrayList<Database>(), false),
        Arguments.of(List.of(db1), true),
        Arguments.of(List.of(db2, db1), true),
        Arguments.of(List.of(sysdb1, sysdb2, sysdb3, db1), true),
        Arguments.of(List.of(sysdb1, sysdb2, sysdb3), false));
  }
}
