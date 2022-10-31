package bio.terra.landingzone.db;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.landingzone.db.exception.DuplicateLandingZoneException;
import bio.terra.landingzone.db.exception.LandingZoneNotFoundException;
import bio.terra.landingzone.db.model.LandingZoneRecord;
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
      LandingZoneRecord lz =
          new LandingZoneRecord(
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
      LandingZoneRecord lz =
          new LandingZoneRecord(
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

      LandingZoneRecord lzRecord = landingZoneDao.getLandingZoneRecord(expectedLzId);
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
      LandingZoneRecord lz =
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
        LandingZoneNotFoundException.class,
        () -> landingZoneDao.getLandingZoneRecord(notExistingLzId));
  }

  @Test
  public void deleteLandingZoneSuccess() {
    UUID expectedLzId = UUID.randomUUID();
    LandingZoneRecord lz =
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
  public void getLandingZoneList_ByLandingZoneTarget_Success() {
    UUID expectedLzId = UUID.randomUUID();
    try {
      LandingZoneRecord lz =
          new LandingZoneRecord(
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

      List<LandingZoneRecord> noRecords =
          landingZoneDao.getLandingZoneList(SUBSCRIPTION, TENANT, "res_group");
      assertEquals(Collections.EMPTY_LIST, noRecords);
      List<LandingZoneRecord> noRecords1 =
          landingZoneDao.getLandingZoneList("sub", TENANT, RESOURCE_GROUP);
      assertEquals(Collections.EMPTY_LIST, noRecords1);

      List<LandingZoneRecord> lzRecords =
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
  public void getLandingZoneList_OneId_Success() {
    UUID expectedLzId = UUID.randomUUID();
    try {
      LandingZoneRecord lz =
          new LandingZoneRecord(
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
      List<LandingZoneRecord> lzRecords =
          landingZoneDao.getLandingZoneMatchingIdList(List.of(expectedLzId));

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
  public void getLandingZoneList_Success() {
    UUID expectedLz1Id = UUID.randomUUID();
    UUID expectedLz2Id = UUID.randomUUID();
    try {
      LandingZoneRecord lz1 =
          new LandingZoneRecord(
              expectedLz1Id,
              RESOURCE_GROUP,
              DEFINITION,
              VERSION,
              SUBSCRIPTION,
              TENANT,
              BILLING_PROFILE,
              Optional.of(DISPLAY_NAME),
              Optional.of(DESCRIPTION),
              properties);
      LandingZoneRecord lz2 =
          new LandingZoneRecord(
              expectedLz2Id,
              RESOURCE_GROUP,
              DEFINITION,
              VERSION,
              SUBSCRIPTION,
              TENANT,
              UUID.randomUUID(),
              Optional.of(DISPLAY_NAME),
              Optional.of(DESCRIPTION),
              properties);
      landingZoneDao.createLandingZone(lz1);
      landingZoneDao.createLandingZone(lz2);
      List<LandingZoneRecord> lzRecords =
          landingZoneDao.getLandingZoneMatchingIdList(List.of(expectedLz1Id, expectedLz2Id));

      // Checking that above records has been returned.
      assertNotNull(lzRecords);
      assertTrue(lzRecords.size() == 2);
      var recordLz1 =
          lzRecords.stream().filter(r -> r.landingZoneId().equals(expectedLz1Id)).toList();
      var recordLz2 =
          lzRecords.stream().filter(r -> r.landingZoneId().equals(expectedLz2Id)).toList();

      assertEquals(expectedLz1Id, recordLz1.get(0).landingZoneId());
      assertEquals(expectedLz2Id, recordLz2.get(0).landingZoneId());
      assertEquals(BILLING_PROFILE, recordLz1.get(0).billingProfileId());
      assertEquals(lz2.billingProfileId(), recordLz2.get(0).billingProfileId());
    } finally {
      try {
        landingZoneDao.deleteLandingZone(expectedLz1Id);
        landingZoneDao.deleteLandingZone(expectedLz2Id);
      } catch (Exception ex) {
        fail("Failure during removing landing zone from database", ex);
      }
    }
  }

  @Test
  public void getLandingZoneList_NoMatchingIdsAreIgnored_Success() {
    UUID expectedLzId = UUID.randomUUID();
    try {
      LandingZoneRecord lz =
          new LandingZoneRecord(
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
      List<LandingZoneRecord> lzRecords =
          landingZoneDao.getLandingZoneMatchingIdList(
              List.of(UUID.randomUUID(), UUID.randomUUID(), expectedLzId));

      // There can be more records created for the same Landing Zone target.
      // Checking that above record has been returned.
      assertTrue(lzRecords.size() == 1);
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
