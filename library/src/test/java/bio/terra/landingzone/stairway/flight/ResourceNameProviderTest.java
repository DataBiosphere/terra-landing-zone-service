package bio.terra.landingzone.stairway.flight;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.stairway.flight.create.resource.step.BaseResourceCreateStep;
import bio.terra.landingzone.stairway.flight.exception.ResourceNameGenerationException;
import bio.terra.stairway.FlightContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ResourceNameProviderTest {
  private final UUID LANDING_ZONE_ID = UUID.randomUUID();
  private ResourceNameProvider resourceNameProvider;
  private ArmManagers armManagers;

  @BeforeEach
  void setup() {
    armManagers = mock(ArmManagers.class);
    resourceNameProvider = new ResourceNameProvider(LANDING_ZONE_ID);
  }

  @Test
  void testGetNameSuccess() {
    int resourceNameMaxLength = 26;
    String resourceType = "SOME_RESOURCE";

    createDummyStep(
        armManagers,
        resourceNameProvider,
        new ResourceNameRequirements(resourceType, resourceNameMaxLength));

    var name = resourceNameProvider.getName(resourceType);

    assertNotNull(name);
    assertThat(name.length(), equalTo(resourceNameMaxLength));
  }

  @Test
  void testStepIsNotRegisteredThrowsException() {
    assertThrows(
        ResourceNameGenerationException.class,
        () -> resourceNameProvider.getName("NOT_REGISTERED_RESOURCE"));
  }

  @Test
  void testStepWithSameResourceAlreadyRegisteredThrowsException() {
    int resourceNameMaxLength = 26;
    // step is registered for name generation during construction
    createDummyStep(
        armManagers,
        resourceNameProvider,
        new ResourceNameRequirements("SOME_RESOURCE", resourceNameMaxLength));

    assertThrows(
        ResourceNameGenerationException.class,
        () ->
            createDummyStep(
                armManagers,
                resourceNameProvider,
                new ResourceNameRequirements("SOME_RESOURCE", resourceNameMaxLength)));
  }

  private static BaseResourceCreateStep createDummyStep(
      ArmManagers armManagers,
      ResourceNameProvider resourceNameProvider,
      ResourceNameRequirements resourceNameRequirements) {
    return new BaseResourceCreateStep(armManagers, resourceNameProvider) {
      @Override
      public List<ResourceNameRequirements> getResourceNameRequirements() {
        return List.of(
            new ResourceNameRequirements(
                getResourceType(), resourceNameRequirements.maxNameLength()));
      }

      @Override
      protected void createResource(FlightContext context) {
        // we don't need implementation here
      }

      @Override
      protected void deleteResource(String resourceId) {
        // we don't need implementation here
      }

      @Override
      protected String getResourceType() {
        return resourceNameRequirements.resourceType();
      }

      @Override
      protected Optional<String> getResourceId(FlightContext context) {
        return Optional.empty();
      }
    };
  }
}
