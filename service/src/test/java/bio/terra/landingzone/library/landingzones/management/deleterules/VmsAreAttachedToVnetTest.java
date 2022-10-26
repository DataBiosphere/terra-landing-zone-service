package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_VM_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachines;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.NicIpConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VmsAreAttachedToVnetTest extends BaseDependencyRuleFixture {

  protected VmsAreAttachedToVnet rule;

  @BeforeEach
  void setUp() {
    rule = new VmsAreAttachedToVnet(armManagers);
  }

  @Test
  void getExpectedType_returnsExpectedType() {
    assertThat(rule.getExpectedType(), equalTo(AZURE_VM_TYPE));
  }

  @Test
  void hasDependentResources_vmAttachedToNetwork_returnsTrue() {
    when(resource.resourceGroupName()).thenReturn(RESOURCE_GROUP);
    when(resourceToDelete.resource()).thenReturn(resource);

    var vms = mock(VirtualMachines.class);
    when(azureResourceManager.virtualMachines()).thenReturn(vms);
    var vm = mock(VirtualMachine.class);
    PagedIterable<VirtualMachine> vmList = toMockPageIterable(List.of(vm));
    when(vms.listByResourceGroup(RESOURCE_GROUP)).thenReturn(vmList);
    var nic = mock(NetworkInterface.class);
    when(vm.getPrimaryNetworkInterface()).thenReturn(nic);
    var nicIpConfiguration = mock(NicIpConfiguration.class);
    when(nic.primaryIPConfiguration()).thenReturn(nicIpConfiguration);
    var network = mock(Network.class);
    when(nicIpConfiguration.getNetwork()).thenReturn(network);
    var networkId = "LZNetworkId";
    when(network.id()).thenReturn(networkId);
    // the generic resources API returns the type in lower case, so the rule must match ignoring
    // the case.
    var networkIdLowerCase = "lznetworkid";
    when(resource.id()).thenReturn(networkIdLowerCase);

    assertThat(rule.hasDependentResources(resourceToDelete), equalTo(true));
  }

  @Test
  void hasDependentResources_vmAttachedToAnotherNetwork_returnsFalse() {
    when(resource.resourceGroupName()).thenReturn(RESOURCE_GROUP);
    when(resourceToDelete.resource()).thenReturn(resource);

    var vms = mock(VirtualMachines.class);
    when(azureResourceManager.virtualMachines()).thenReturn(vms);
    var vm = mock(VirtualMachine.class);
    PagedIterable<VirtualMachine> vmList = toMockPageIterable(List.of(vm));
    when(vms.listByResourceGroup(RESOURCE_GROUP)).thenReturn(vmList);
    var nic = mock(NetworkInterface.class);
    when(vm.getPrimaryNetworkInterface()).thenReturn(nic);
    var nicIpConfiguration = mock(NicIpConfiguration.class);
    when(nic.primaryIPConfiguration()).thenReturn(nicIpConfiguration);
    var network = mock(Network.class);
    when(nicIpConfiguration.getNetwork()).thenReturn(network);
    var networkId = "Not a LZ NetworkId";
    when(network.id()).thenReturn(networkId);
    // the generic resources API returns the type in lower case, so the rule must match ignoring
    // the case.
    var networkIdLowerCase = "lznetworkid";
    when(resource.id()).thenReturn(networkIdLowerCase);

    assertThat(rule.hasDependentResources(resourceToDelete), equalTo(false));
  }
}
