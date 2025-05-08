package edu.boun.edgecloudsim.applications.RR2.tests;

import java.util.Calendar;
import java.util.GregorianCalendar;

import edu.boun.edgecloudsim.applications.RR2.RoundRobinEdgeOrchestrator;
import org.cloudbus.cloudsim.core.CloudSim;
import org.junit.jupiter.api.*;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(JMockitExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoundRobinEdgeOrchestratorTest {

    @Mocked SimSettings settings;    // intercepts SimSettings.getInstance()
    @Mocked Task dummyTask;          // no-arg mock of Task

    @BeforeAll
    void beforeAll() {
        System.out.println("=== Starting RoundRobinEdgeOrchestratorTest ===");
    }

    @BeforeEach
    void setup() {
        // initialize CloudSim so CloudSim.entities is non-null
        CloudSim.init(
                1,                          // numUsers
                new GregorianCalendar(),   // calendar
                false                       // no trace
        );

        // allow any number of calls to SimSettings.getInstance() and getNumOfEdgeHosts()
        new Expectations() {{
            SimSettings.getInstance();    result = settings; minTimes = 0;
            settings.getNumOfEdgeHosts(); result = 3;        minTimes = 0;
        }};
    }

    @Test
    void roundRobinCyclesThroughThreeHosts() {
        System.out.print("→ Running roundRobinCyclesThroughThreeHosts… ");
        // construct and initialize
        var orch = new RoundRobinEdgeOrchestrator("RoundRobin", "ANY");
        orch.initialize();

        int base = SimSettings.GENERIC_EDGE_DEVICE_ID;
        // five invocations should yield hosts 0,1,2,0,1
        assertEquals(base + 0, orch.getDeviceToOffload(dummyTask));
        assertEquals(base + 1, orch.getDeviceToOffload(dummyTask));
        assertEquals(base + 2, orch.getDeviceToOffload(dummyTask));
        assertEquals(base + 0, orch.getDeviceToOffload(dummyTask));
        assertEquals(base + 1, orch.getDeviceToOffload(dummyTask));
        System.out.println("PASSED");
    }

    @AfterAll
    void afterAll() {
        System.out.println("=== Finished RoundRobinEdgeOrchestratorTest ===");
    }
}
