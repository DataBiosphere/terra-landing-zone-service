package bio.terra.landingzone.db;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.landingzone.db.exception.DuplicateLandingZoneException;
import bio.terra.landingzone.db.exception.LandingZoneNotFoundException;
import bio.terra.landingzone.db.model.LandingZone;
import bio.terra.landingzone.testutils.LibraryTestBase;
import bio.terra.landingzone.testutils.TestFixtures;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DeployedLandingZoneDaoTest extends LibraryTestBase {
  private static final String RESOURCE_GROUP = "test-resource-group";
  private static final String SUBSCRIPTION = "test-subscription-Id";
  private static final String TENANT = "test-tenant-Id";
  private static final UUID BILLING_PROFILE = UUID.randomUUID();
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
              SUBSCRIPTION,
              TENANT,
              BILLING_PROFILE,
              Optional.of(DISPLAY_NAME),
              Optional.of(DESCRIPTION),
              properties);
      UUID actualLzId = landingZoneDao.createLandingZone(lz);
      assertEquals(expectedLzId, actualLzId);
    } finally {
      try {
        landingZoneDao.deleteLandingZone(expectedLzId);
      } catch (Exception ex) {
        fail("Failure while removing landing zone from database", ex);
      }
    }
  }

  @Test
  public void getLandingZone_validateFields_Success() {
    UUID expectedLzId = UUID.randomUUID();
    try {
      LandingZone lz =
          new LandingZone(
              expectedLzId,
              RESOURCE_GROUP,
              DEFINITION,
              VERSION,
              SUBSCRIPTION,
              TENANT,
              BILLING_PROFILE,
              Optional.of(DISPLAY_NAME),
              Optional.of(DESCRIPTION),
              properties);
      UUID actualLzId = landingZoneDao.createLandingZone(lz);
      assertEquals(expectedLzId, actualLzId);

      LandingZone lzRecord = landingZoneDao.getLandingZone(expectedLzId);
      assertEquals(expectedLzId, lz.landingZoneId());
      assertEquals(RESOURCE_GROUP, lz.resourceGroupId());
      assertEquals(DEFINITION, lz.definition());
      assertEquals(VERSION, lz.version());
      assertEquals(SUBSCRIPTION, lz.subscriptionId());
      assertEquals(TENANT, lz.tenantId());
      assertEquals(BILLING_PROFILE, lz.billingProfileId());
      assertEquals(DISPLAY_NAME, lz.displayName().get());
      assertEquals(DESCRIPTION, lz.description().get());
      assertEquals(properties, lz.properties());
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
    try {
      LandingZone lz =
          TestFixtures.createLandingZone(
              expectedLzId,
              RESOURCE_GROUP,
              DEFINITION,
              VERSION,
              DISPLAY_NAME,
              DESCRIPTION,
              properties,
              SUBSCRIPTION,
              TENANT,
              BILLING_PROFILE);
      landingZoneDao.createLandingZone(lz);

      assertThrows(DuplicateLandingZoneException.class, () -> landingZoneDao.createLandingZone(lz));
    } finally {
      try {
        landingZoneDao.deleteLandingZone(expectedLzId);
      } catch (Exception ex) {
        fail("Failure while removing landing zone from database", ex);
      }
    }
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
            properties,
            SUBSCRIPTION,
            TENANT,
            BILLING_PROFILE);
    landingZoneDao.createLandingZone(lz);

    assertTrue(landingZoneDao.deleteLandingZone(expectedLzId));
  }

  @Test
  public void getLandingZoneListThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> landingZoneDao.getLandingZoneList(SUBSCRIPTION, TENANT, ""));
    assertThrows(
        IllegalArgumentException.class,
        () -> landingZoneDao.getLandingZoneList(SUBSCRIPTION, TENANT, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> landingZoneDao.getLandingZoneList(SUBSCRIPTION, null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> landingZoneDao.getLandingZoneList(null, TENANT, RESOURCE_GROUP));
    assertThrows(
        IllegalArgumentException.class, () -> landingZoneDao.getLandingZoneList("", "", ""));
  }

  @Test
  public void getLandingZoneList_Success() {
    UUID expectedLzId = UUID.randomUUID();
    try {
      LandingZone lz =
          new LandingZone(
              expectedLzId,
              RESOURCE_GROUP,
              DEFINITION,
              VERSION,
              SUBSCRIPTION,
              TENANT,
              BILLING_PROFILE,
              Optional.of(DISPLAY_NAME),
              Optional.of(DESCRIPTION),
              properties);

      landingZoneDao.createLandingZone(lz);

      List<LandingZone> noRecords =
          landingZoneDao.getLandingZoneList(SUBSCRIPTION, TENANT, "res_group");
      assertEquals(Collections.EMPTY_LIST, noRecords);
      List<LandingZone> noRecords1 =
          landingZoneDao.getLandingZoneList("sub", TENANT, RESOURCE_GROUP);
      assertEquals(Collections.EMPTY_LIST, noRecords1);

      List<LandingZone> lzRecords =
          landingZoneDao.getLandingZoneList(SUBSCRIPTION, TENANT, RESOURCE_GROUP);

      // There can be more records created for the same Landing Zone target.
      // Checking that above record has been returned.
      assertTrue(lzRecords.size() >= 1);
      var records = lzRecords.stream().filter(r -> r.landingZoneId().equals(expectedLzId)).toList();

      assertNotNull(records);
      assertEquals(1, records.size());
      assertEquals(expectedLzId, records.get(0).landingZoneId());
      assertEquals(RESOURCE_GROUP, records.get(0).resourceGroupId());
      assertEquals(DEFINITION, records.get(0).definition());
      assertEquals(VERSION, records.get(0).version());
      assertEquals(SUBSCRIPTION, records.get(0).subscriptionId());
      assertEquals(TENANT, records.get(0).tenantId());
      assertEquals(BILLING_PROFILE, records.get(0).billingProfileId());
      assertEquals(DISPLAY_NAME, records.get(0).displayName().get());
      assertEquals(DESCRIPTION, records.get(0).description().get());
      assertEquals(properties, records.get(0).properties());
    } finally {
      try {
        landingZoneDao.deleteLandingZone(expectedLzId);
      } catch (Exception ex) {
        fail("Failure during removing landing zone from database", ex);
      }
    }
  }

  @Test
  public void deleteLandingZoneWhenItDoesntExist() {
    UUID notExistingLzId = UUID.fromString("00000000-0000-0000-C000-000000000046");
    assertFalse(landingZoneDao.deleteLandingZone(notExistingLzId));
  }
}
