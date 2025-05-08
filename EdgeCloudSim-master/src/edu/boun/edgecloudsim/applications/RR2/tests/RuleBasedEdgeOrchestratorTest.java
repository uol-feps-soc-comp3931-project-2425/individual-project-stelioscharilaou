package edu.boun.edgecloudsim.applications.RR2.tests;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ArrayList;

import edu.boun.edgecloudsim.applications.RR2.RuleBasedEdgeOrchestrator;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.junit.jupiter.api.*;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
//import edu.boun.edgecloudsim.edge_orchestrator.RuleBasedEdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(JMockitExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuleBasedEdgeOrchestratorTest {

    @Mocked SimSettings settings;
    @Mocked SimManager manager;
    @Mocked MobilityModel mobility;
    @Mocked Task highTask, lowTask;
    @Mocked CloudletScheduler sched0, sched1;
    @Mocked EdgeVM vm0, vm1;
    @Mocked EdgeHost host0, host1;
    @Mocked Datacenter dc0, dc1;
    @Mocked Location devLoc, loc0, loc1;

    private RuleBasedEdgeOrchestrator orch;

    // Simple stub manager with two datacenters and per-host VM lists
    static class FakeESM extends EdgeServerManager {
        final List<Datacenter> dcs;
        final List<List<EdgeVM>> vmLists;
        FakeESM(List<Datacenter> dcs, int hosts) {
            this.dcs = dcs;
            vmLists = new ArrayList<>();
            for (int i = 0; i < hosts; i++) vmLists.add(new ArrayList<>());
        }
        @Override public List<Datacenter> getDatacenterList() { return dcs; }
        @Override public List<EdgeVM> getVmList(int hostId) { return vmLists.get(hostId); }
        // no-op abstract methods
        @Override public void initialize() {}
        @Override public Object startDatacenters() throws Exception {
            return null;
        }
        @Override public void terminateDatacenters() {}
        @Override public void createVmList(int b) {}
        @Override public double getAvgUtilization() { return 0; }
        @Override public org.cloudbus.cloudsim.VmAllocationPolicy getVmAllocationPolicy(List<? extends org.cloudbus.cloudsim.Host> l, int i) { return null; }
    }

    @BeforeAll
    void beforeAll() {
        System.out.println("=== Starting RuleBasedEdgeOrchestratorTest ===");
    }

    @BeforeEach
    void setUp() {
        // initialize CloudSim
        CloudSim.init(1, new GregorianCalendar(), false);

        // fake manager with 2 hosts
        FakeESM fakeMgr = new FakeESM(Arrays.asList(dc0, dc1), 2);

        new Expectations() {{
            // SimSettings
            SimSettings.getInstance();        result = settings; minTimes = 0;
            settings.getNumOfEdgeHosts();     result = 2;        minTimes = 0;

            // SimManager
            SimManager.getInstance();         result = manager;  minTimes = 0;
            manager.getEdgeServerManager();   result = fakeMgr;  minTimes = 0;
            manager.getMobilityModel();       result = mobility; minTimes = 0;

            // Task priorities (permissive)
            highTask.getPriority();           result = 1;        minTimes = 0;
            highTask.getMobileDeviceId();     result = 0;        minTimes = 0;
            lowTask.getPriority();            result = 0;        minTimes = 0;
            lowTask.getMobileDeviceId();      result = 0;        minTimes = 0;

            // Mobility: device at (1,0)
            mobility.getLocation(0, CloudSim.clock()); result = devLoc; minTimes = 0;
            devLoc.getXPos();                 result = 1.0;      minTimes = 0;
            devLoc.getYPos();                 result = 0.0;      minTimes = 0;

            // Datacenter → Host → Location
            dc0.getHostList();                result = List.of(host0); minTimes = 0;
            dc1.getHostList();                result = List.of(host1); minTimes = 0;
            host0.getId();                    result = 0;               minTimes = 0;
            host1.getId();                    result = 1;               minTimes = 0;
            host0.getLocation();              result = loc0;            minTimes = 0;
            host1.getLocation();              result = loc1;            minTimes = 0;
            loc0.getXPos();                   result = 0.0;             minTimes = 0;
            loc0.getYPos();                   result = 0.0;             minTimes = 0;
            loc1.getXPos();                   result = 10.0;            minTimes = 0;
            loc1.getYPos();                   result = 0.0;             minTimes = 0;
        }};

        // Stub each VM’s scheduler and utilization
        new Expectations(vm0) {{
            vm0.getCloudletScheduler(); result = sched0; minTimes = 0;
        }};
        new Expectations(sched0) {{
            sched0.getTotalUtilizationOfCpu(anyDouble); result = 0.3; minTimes = 0;
        }};
        new Expectations(vm1) {{
            vm1.getCloudletScheduler(); result = sched1; minTimes = 0;
        }};
        new Expectations(sched1) {{
            sched1.getTotalUtilizationOfCpu(anyDouble); result = 0.7; minTimes = 0;
        }};

        // Add VMs to fake manager
        fakeMgr.getVmList(0).add(vm0);
        fakeMgr.getVmList(1).add(vm1);

        // instantiate and init orchestrator
        orch = new RuleBasedEdgeOrchestrator("RB", "ANY");
        orch.initialize();
    }

    @Test
    void highPriorityChoosesNearestLowUtilHost() {
        System.out.print("→ Running highPriorityChoosesNearestLowUtilHost… ");
        int chosen = orch.getDeviceToOffload(highTask);
        int expected = SimSettings.GENERIC_EDGE_DEVICE_ID + 0;
        assertEquals(expected, chosen);
        System.out.println("PASSED");
    }

    @Test
    void lowPriorityFallsBackToRoundRobin() {
        System.out.print("→ Running lowPriorityFallsBackToRoundRobin… ");
        int first  = orch.getDeviceToOffload(lowTask);
        int second = orch.getDeviceToOffload(lowTask);
        int base   = SimSettings.GENERIC_EDGE_DEVICE_ID;
        assertEquals(base + 0, first);
        assertEquals(base + 1, second);
        System.out.println("PASSED");
    }

    @AfterAll
    void afterAll() {
        System.out.println("=== Finished RuleBasedEdgeOrchestratorTest ===");
    }
}
