package bio.terra.landingzone.common;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;

public class VirtualNetworkLinkResourceHelper {
  private VirtualNetworkLinkResourceHelper() {}

  public static void delete(ArmManagers armManagers, String resourceId) {
    var idParts = resourceId.split("/");
    if (idParts.length != 11) {
      throw new IllegalArgumentException(
          String.format("Invalid vnet link resourceId: %s", resourceId));
    }
    var mrgName = idParts[4];
    var privateDnsZoneName = idParts[8];
    var vnetLinkName = idParts[10];

    armManagers
        .azureResourceManager()
        .privateDnsZones()
        .manager()
        .serviceClient()
        .getVirtualNetworkLinks()
        .delete(mrgName, privateDnsZoneName, vnetLinkName);
  }
}
