package bio.terra.landingzone.library.landingzones.definition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ResourceNameGeneratorTest {

  @Test
  void nextName_areTheSameForTwoInstancesWithSameLZId() {
    String landingZoneId = UUID.randomUUID().toString();
    ResourceNameGenerator generator1 = new ResourceNameGenerator(landingZoneId);
    ResourceNameGenerator generator2 = new ResourceNameGenerator(landingZoneId);

    assertThat(generator1.nextName(23), equalTo(generator2.nextName(23)));
    assertThat(generator1.nextName(23), equalTo(generator2.nextName(23)));
  }

  @Test
  void nextName_calledTwiceNamesAreDifferent() {
    String landingZoneId = UUID.randomUUID().toString();
    ResourceNameGenerator generator1 = new ResourceNameGenerator(landingZoneId);

    assertThat(generator1.nextName(23), not(equalTo(generator1.nextName(23))));
  }

  @Test
  void nextName_maxLengthIsReturned() {
    String landingZoneId = UUID.randomUUID().toString();
    ResourceNameGenerator generator1 = new ResourceNameGenerator(landingZoneId);

    assertThat(generator1.nextName().length(), equalTo(66));
  }

  @Test
  void nextName_nameIsExpectedLength() {
    String landingZoneId = UUID.randomUUID().toString();
    ResourceNameGenerator generator1 = new ResourceNameGenerator(landingZoneId);

    assertThat(generator1.nextName(23).length(), equalTo(23));
  }

  @Test
  void resetSequence_firstNameIsRegeneratedAfterReset() {
    String landingZoneId = UUID.randomUUID().toString();
    ResourceNameGenerator generator1 = new ResourceNameGenerator(landingZoneId);
    String firstName = generator1.nextName(23);
    generator1.resetSequence();
    String afterReset = generator1.nextName(23);

    assertThat(firstName, equalTo(afterReset));
  }
}
