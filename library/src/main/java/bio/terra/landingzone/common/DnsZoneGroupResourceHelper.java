package bio.terra.landingzone.common;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;

public class DnsZoneGroupResourceHelper {
  private DnsZoneGroupResourceHelper() {}

  public static void delete(ArmManagers armManagers, String resourceId) {
    // Example zone group resource ID:
    //
    // /subscriptions/289871e8-b6d7-4b86-91b1-8d365496bf5c/resourceGroups/rtPrivateTerra3/providers/Microsoft.Network/privateEndpoints/lz88db01a9e464d8d0f7b1228e689cdd18f71fd117e044ae83e670f55bae6a87/privateDnsZoneGroups/lz5f1e5aafefd4b9823828927d22883fc14f06a8b34b648db1ee8e86d2dce377
    var idParts = resourceId.split("/");
    if (idParts.length != 11) {
      throw new IllegalArgumentException(
          String.format("Invalid dns zone group resourceId: %s", resourceId));
    }
    var mrgName = idParts[4];
    var privateEndpointName = idParts[8];
    var zoneGroupName = idParts[10];

    armManagers
        .azureResourceManager()
        .privateEndpoints()
        .manager()
        .serviceClient()
        .getPrivateDnsZoneGroups()
        .delete(mrgName, privateEndpointName, zoneGroupName);
  }
}
