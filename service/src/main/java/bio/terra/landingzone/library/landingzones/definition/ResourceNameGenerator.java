package bio.terra.landingzone.library.landingzones.definition;

import com.azure.core.util.logging.ClientLogger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class that facilitates creation of unique Azure resource names with a predictable
 * outcome. Names are generated from hash created from the landingZoneId and an internal sequence
 * number. Each call to the method nextName() the internal sequence is increased.
 */
public class ResourceNameGenerator {
  public static final int MAX_NAME_LENGTH = 66;
  public static final int MAX_STORAGE_ACCOUNT_NAME_LENGTH = 24;
  public static final int MAX_BATCH_ACCOUNT_NAME_LENGTH = 24;
  public static final int MAX_RELAY_NS_NAME_LENGTH = 50;
  public static final int MAX_POSTGRESQL_SERVER_NAME_LENGTH = 63;
  public static final int MAX_PRIVATE_DNS_ZONE_NAME_LENGTH = 24;
  public static final int UAMI_NAME_LENGTH = 20;
  public static final int MAX_PRIVATE_VNET_LINK_NAME_LENGTH = 80;
  public static final int MAX_LOG_ANALYTICS_WORKSPACE_NAME_LENGTH = 63;
  public static final int MAX_PRIVATE_ENDPOINT_NAME_LENGTH = 23; // TODO verify this
  public static final int MAX_PRIVATE_LINK_CONNECTION_NAME_LENGTH = 10; // TODO verify this
  // This is not the maximum value of 63. This lower value is to reduce the risk of deployment
  // errors as the cluster name is added to the node pool resource group name.
  // The node pool name resource group name can't be longer than 80 characters,
  // and follows this pattern: MC_resourceGroupName_resourceName_AzureRegion.
  public static final int MAX_AKS_CLUSTER_NAME_LENGTH = 25;
  public static final int MAX_VNET_NAME_LENGTH = 64;
  public static final int MAX_AKS_AGENT_POOL_NAME_LENGTH = 11;
  public static final int MAX_AKS_DNS_PREFIX_NAME_LENGTH = 54;
  public static final int MAX_DIAGNOSTIC_SETTING_NAME_LENGTH = 260;
  public static final int MAX_APP_INSIGHTS_COMPONENT_NAME_LENGTH = 255;
  public static final int MAX_DATA_COLLECTION_RULE_NAME_LENGTH = 64;

  private final ClientLogger logger = new ClientLogger(ResourceNameGenerator.class);
  private final String landingZoneId;
  private int sequence;

  public ResourceNameGenerator(String landingZoneId) {
    this.landingZoneId = landingZoneId;
    sequence = 0;
  }

  /**
   * Returns a prefixed sub-string of the hash of the landing zone id and the internal sequence
   * number. The return value is guaranteed to be the same for a landing zone id and a given
   * sequence. The sequence number is increased after each call. The returned value is not
   * guaranteed to be globally unique for all lengths; however, the likelihood of a naming collision
   * is low.
   *
   * @param length size of the returned string.
   * @return next name in the sequence.
   */
  public synchronized String nextName(int length) {
    String name = prepareStringAsAzureResourceName(generateHashFromSeed(), length);
    sequence = sequence + 1;
    return name;
  }

  /** Resets the sequence number back to zero. */
  public synchronized void resetSequence() {
    sequence = 0;
  }

  private String generateHashFromSeed() {
    MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw logger.logExceptionAsError(new RuntimeException(e));
    }

    final byte[] bytes = messageDigest.digest(parseStringToHash().getBytes(StandardCharsets.UTF_8));
    return bytesToHex(bytes);
  }

  private String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  private String prepareStringAsAzureResourceName(String hash, int length) {
    int end = Math.max(length, 5);
    end = Math.min(end, hash.length() + 2);
    return "lz" + hash.substring(0, end - 2);
  }

  private String parseStringToHash() {
    return String.format("%s%s", landingZoneId, sequence);
  }

  public String nextName() {
    return nextName(MAX_NAME_LENGTH);
  }
}
