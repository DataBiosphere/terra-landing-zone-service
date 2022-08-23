package bio.terra.landingzone.db;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.landingzone.db.exception.DuplicateLandingZoneException;
import bio.terra.landingzone.db.exception.LandingZoneNotFoundException;
import bio.terra.landingzone.db.model.LandingZone;
import bio.terra.landingzone.testutils.LibraryTestBase;
import bio.terra.landingzone.testutils.TestFixtures;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DeployedLandingZoneDaoTest extends LibraryTestBase {
  private static final String RESOURCE_GROUP = "test-resource-group";
  private static final String DEFINITION = "lzDefinition";
  private static final String VERSION = "v1";
  private static final String DISPLAY_NAME = "lzDisplayName";
  private static final String DESCRIPTION = "lzDescription";
  private static final Map<String, String> properties = Map.of("key1", "value1");

  @Autowired private LandingZoneDao landingZoneDao;

  @Test
  public void createLandingZoneSuccess() {
    UUID expectedLzId = UUID.randomUUID();
    try {
      LandingZone lz =
          new LandingZone(
              expectedLzId,
              RESOURCE_GROUP,
              DEFINITION,
              VERSION,
              DISPLAY_NAME,
              DESCRIPTION,
              properties);
      UUID actualLzId = landingZoneDao.createLandingZone(lz);
      assertEquals(expectedLzId, actualLzId);
    } finally {
      try {
        landingZoneDao.deleteLandingZone(expectedLzId);
      } catch (Exception ex) {
        fail("Failure during removing landing zone from database", ex);
      }
    }
  }

  @Test
  public void createDuplicateLandingZoneThrowsException() {
    UUID expectedLzId = UUID.randomUUID();
    LandingZone lz =
        TestFixtures.createLandingZone(
            expectedLzId,
            RESOURCE_GROUP,
            DEFINITION,
            VERSION,
            DISPLAY_NAME,
            DESCRIPTION,
            properties);
    landingZoneDao.createLandingZone(lz);

    assertThrows(DuplicateLandingZoneException.class, () -> landingZoneDao.createLandingZone(lz));
  }

  @Test
  public void findNotExistingRecordThrowsException() {
    UUID notExistingLzId = UUID.fromString("00000000-0000-0000-C000-000000000046");
    assertThrows(
        LandingZoneNotFoundException.class, () -> landingZoneDao.getLandingZone(notExistingLzId));
  }

  @Test
  public void deleteLandingZoneSuccess() {
    UUID expectedLzId = UUID.randomUUID();
    LandingZone lz =
        TestFixtures.createLandingZone(
            expectedLzId,
            RESOURCE_GROUP,
            DEFINITION,
            VERSION,
            DISPLAY_NAME,
            DESCRIPTION,
            properties);
    landingZoneDao.createLandingZone(lz);

    assertTrue(landingZoneDao.deleteLandingZone(expectedLzId));
  }

  @Test
  public void deleteLandingZoneWhenItDoesntExist() {
    UUID notExistingLzId = UUID.fromString("00000000-0000-0000-C000-000000000046");
    assertFalse(landingZoneDao.deleteLandingZone(notExistingLzId));
  }
}
