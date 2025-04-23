package edu.boun.edgecloudsim.applications.RR2;

import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.List;

/**
 * Rule-based orchestrator:
 * - High-priority tasks: assigned to the least-utilized VM among the first two reserved VMs
 *   on the two closest hosts.
 * - Low-priority tasks: assigned in a simple round-robin across the remaining VMs.
 */
public class RuleBasedEdgeOrchestrator extends EdgeOrchestrator {
    private int rrIndex;
    private int numHosts;

    public RuleBasedEdgeOrchestrator(String policy, String simScenario) {
        super(policy, simScenario);
    }

    @Override
    public void initialize() {
        numHosts = SimSettings.getInstance().getNumOfEdgeHosts();
        rrIndex = 0;
    }

    /**
     * High-priority: pick two closest hosts, then pick lowest-util VM among their first two VMs.
     * Low-priority: pick next VM in round-robin from the remaining pool (index>=2).
     */
    @Override
    public int getDeviceToOffload(Task task) {
        boolean highPriority = (task.getTaskType() == 2);
        if (highPriority) {
            var mob = SimManager.getInstance().getMobilityModel();
            var devLoc = mob.getLocation(task.getMobileDeviceId(), CloudSim.clock());

            int firstHost=-1, secondHost=-1;
            double firstDist=Double.MAX_VALUE, secondDist=Double.MAX_VALUE;
            List<Datacenter> dcs = SimManager.getInstance().getEdgeServerManager().getDatacenterList();
            for (Datacenter dc : dcs) {
                EdgeHost host = (EdgeHost)dc.getHostList().get(0);
                var loc=host.getLocation();
                double dx=devLoc.getXPos()-loc.getXPos(), dy=devLoc.getYPos()-loc.getYPos();
                double dist=Math.hypot(dx,dy);
                int hid=SimSettings.GENERIC_EDGE_DEVICE_ID+host.getId();
                if (dist<firstDist) { secondDist=firstDist; secondHost=firstHost; firstDist=dist; firstHost=hid; }
                else if (dist<secondDist) { secondDist=dist; secondHost=hid; }
            }
            // choose among first two reserved VMs on firstHost
            EdgeVM reserved1 = getLowestVm(firstHost);
            EdgeVM reserved2 = getLowestVm(secondHost);
            // compare utilizations
            double u1 = utilization(reserved1), u2 = utilization(reserved2);
            return (u1<=u2 ? firstHost : secondHost);
        }
        // low-priority: round-robin among hosts
        int host = SimSettings.GENERIC_EDGE_DEVICE_ID + (rrIndex % numHosts);
        rrIndex++;
        return host;
    }

    /**
     * Reserved VMs for high-priority: pick lowest-utilization among the first two VMs on host.
     */
    @Override
    public Vm getVmToOffload(Task task, int deviceId) {
        int hostIdx = deviceId - SimSettings.GENERIC_EDGE_DEVICE_ID;
        List<EdgeVM> vms = SimManager.getInstance().getEdgeServerManager().getVmList(hostIdx);
        if (task.getTaskType()==2) {
            // reserve first two VMs
            EdgeVM vm1 = vms.size()>0 ? vms.get(0) : null;
            EdgeVM vm2 = vms.size()>1 ? vms.get(1) : null;
            double u1 = vm1!=null ? utilization(vm1) : Double.MAX_VALUE;
            double u2 = vm2!=null ? utilization(vm2) : Double.MAX_VALUE;
            return u1<=u2 ? vm1 : vm2;
        }
        // other tasks: assign next available VM index>=2
        int idx = 2 + ((rrIndex-1) / numHosts) % Math.max(1, vms.size()-2);
        return vms.size()>idx ? vms.get(idx) : vms.get(0);
    }

    @Override public void processEvent(org.cloudbus.cloudsim.core.SimEvent ev){}
    @Override public void startEntity(){}
    @Override public void shutdownEntity(){}

    private double utilization(EdgeVM vm) {
        return vm.getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
    }

    /**
     * Pick lowest-util VM among the first two on a host.
     */
    private EdgeVM getLowestVm(int hostId) {
        int idx=hostId-SimSettings.GENERIC_EDGE_DEVICE_ID;
        List<EdgeVM> vms=SimManager.getInstance().getEdgeServerManager().getVmList(idx);
        EdgeVM best=null; double minU=Double.MAX_VALUE;
        for (int i=0; i<2 && i<vms.size(); i++) {
            double u=utilization(vms.get(i));
            if (u<minU) { minU=u; best=vms.get(i); }
        }
        return best;
    }
}
