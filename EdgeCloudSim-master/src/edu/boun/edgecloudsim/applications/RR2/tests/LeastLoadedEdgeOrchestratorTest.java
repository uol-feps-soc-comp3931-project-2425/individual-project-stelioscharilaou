package edu.boun.edgecloudsim.applications.RR2.tests;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import edu.boun.edgecloudsim.applications.RR2.LeastLoadedEdgeOrchestrator;
import org.cloudbus.cloudsim.core.CloudSim;
import org.junit.jupiter.api.*;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that LeastLoadedEdgeOrchestrator picks the host
 * with the lowest average VM CPU utilization.
 */
@ExtendWith(JMockitExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeastLoadedEdgeOrchestratorTest {

    @Mocked SimSettings settings;
    @Mocked SimManager manager;
    @Mocked EdgeVM vm0, vm1, vm2;
    @Mocked Task dummyTask;

    static abstract class FakeEdgeServerManager extends EdgeServerManager {
        final List<List<EdgeVM>> vmLists = new ArrayList<>();
        FakeEdgeServerManager(int numHosts) {
            for (int i = 0; i < numHosts; i++) {
                vmLists.add(new ArrayList<>());
            }
        }
        @Override
        public List<EdgeVM> getVmList(int hostId) {
            return vmLists.get(hostId);
        }
    }

    private FakeEdgeServerManager fakeMgr;

    @BeforeAll
    void beforeAll() {
        System.out.println("=== Starting LeastLoadedEdgeOrchestratorTest ===");
    }

    @BeforeEach
    void setup() {
        // initialize CloudSim
        CloudSim.init(1, new GregorianCalendar(), false);

        // prepare fake manager
        fakeMgr = new FakeEdgeServerManager(3) {
            @Override public void initialize() {}
            @Override public Object startDatacenters() throws Exception { return null; }
            @Override public void terminateDatacenters() {}
            @Override public void createVmList(int brokerId) {}
            @Override public double getAvgUtilization() { return 0; }
            @Override public org.cloudbus.cloudsim.VmAllocationPolicy getVmAllocationPolicy(List<? extends org.cloudbus.cloudsim.Host> l, int i) { return null; }
        };

        // stub singletons
        new Expectations() {{
            SimSettings.getInstance();           result = settings;    minTimes = 0;
            settings.getNumOfEdgeHosts();        result = 3;           minTimes = 0;
            SimManager.getInstance();            result = manager;     minTimes = 0;
            manager.getEdgeServerManager();      result = fakeMgr;     minTimes = 0;
        }};

        // stub and add VMs
        stubVmUtil(vm0, 0.2); fakeMgr.getVmList(0).add(vm0);
        stubVmUtil(vm1, 0.5); fakeMgr.getVmList(1).add(vm1);
        stubVmUtil(vm2, 0.1); fakeMgr.getVmList(2).add(vm2);
    }

    private void stubVmUtil(EdgeVM vm, double util) {
        new Expectations(vm) {{
            vm.getCloudletScheduler().getTotalUtilizationOfCpu(anyDouble);
            result = util; minTimes = 0;
        }};
    }

    @Test
    void picksHostWithLowestAverageUtilization() {
        System.out.print("→ Running picksHostWithLowestAverageUtilization… ");
        var orch = new LeastLoadedEdgeOrchestrator("LL", "ANY");
        orch.initialize();
        int chosen = orch.getDeviceToOffload(dummyTask);
        assertEquals(2, chosen);
        System.out.println("PASSED");
    }

    @AfterAll
    void afterAll() {
        System.out.println("=== Finished LeastLoadedEdgeOrchestratorTest ===");
    }
}
