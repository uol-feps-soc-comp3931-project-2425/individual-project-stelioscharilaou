package edu.boun.edgecloudsim.applications.RR2.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;

import edu.boun.edgecloudsim.edge_client.Task;
import org.cloudbus.cloudsim.core.CloudSim;
import org.junit.jupiter.api.*;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;

import edu.boun.edgecloudsim.applications.RR2.CustomNetworkModel;
import edu.boun.edgecloudsim.core.SimSettings;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(JMockitExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomNetworkModelTest {

    @Mocked SimSettings settings;
    @Mocked
    Task task;

    @BeforeAll
    void beforeAll() {
        System.out.println("=== Starting CustomNetworkModelTest ===");
    }

    @BeforeEach
    void initCloudSimAndSettings() {
        // minimal CloudSim setup
        CloudSim.init(1, new java.util.GregorianCalendar(), false);

        // stub simulation time
        new Expectations() {{
            SimSettings.getInstance();      result = settings; minTimes = 0;
            settings.getSimulationTime();   result = 100.0;   minTimes = 0;
        }};
    }

    @Test
    void smartManufacturingInitializesPureLanNoWirelessOrBursts() throws Exception {
        System.out.print("→ Testing SMART_MANUFACTURING initialization… ");
        CustomNetworkModel m = new CustomNetworkModel(1, "SMART_MANUFACTURING");
        m.initialize();

        Field lanDelayF   = CustomNetworkModel.class.getDeclaredField("lanDelay");
        Field wanDelayF   = CustomNetworkModel.class.getDeclaredField("wanDelay");
        Field wlanDelayF  = CustomNetworkModel.class.getDeclaredField("wlanDelay");
        Field lanBwF      = CustomNetworkModel.class.getDeclaredField("lanBandwidth");
        Field wlanBwF     = CustomNetworkModel.class.getDeclaredField("wlanBandwidth");
        Field wanBwF      = CustomNetworkModel.class.getDeclaredField("wanBandwidth");
        Field spikesF     = CustomNetworkModel.class.getDeclaredField("spikeWindows");

        lanDelayF.setAccessible(true); wanDelayF.setAccessible(true);
        wlanDelayF.setAccessible(true); lanBwF.setAccessible(true);
        wlanBwF.setAccessible(true); wanBwF.setAccessible(true);
        spikesF.setAccessible(true);

        double lanDelay      = (double) lanDelayF.get(m);
        double wanDelay      = (double) wanDelayF.get(m);
        double wlanDelay     = (double) wlanDelayF.get(m);
        double lanBandwidth  = (double) lanBwF.get(m);
        double wlanBandwidth = (double) wlanBwF.get(m);
        double wanBandwidth  = (double) wanBwF.get(m);
        @SuppressWarnings("unchecked")
        List<double[]> spikes = (List<double[]>) spikesF.get(m);

        assertTrue(lanDelay >= 0.001 && lanDelay <= 0.005, "LAN delay in [1,5] ms");
        assertEquals(0.0, wanDelay,      "WAN delay zero");
        assertEquals(0.0, wlanDelay,     "WLAN delay zero");
        assertEquals(100.0e6 / 8.0, lanBandwidth, 1e-6, "LAN BW 100 Mbps");
        assertEquals(0.0, wlanBandwidth, "WLAN BW zero");
        assertEquals(0.0, wanBandwidth,  "WAN BW zero");
        assertTrue(spikes.isEmpty(),     "No bursts");

        System.out.println("PASSED");
    }

    @Test
    void autonomousDrivingSchedulesThreeWirelessSpikes() throws Exception {
        System.out.print("→ Testing AUTONOMOUS_DRIVING initialization… ");
        CustomNetworkModel m = new CustomNetworkModel(1, "AUTONOMOUS_DRIVING");
        m.initialize();

        Field wlanDelayF = CustomNetworkModel.class.getDeclaredField("wlanDelay");
        Field spikesF    = CustomNetworkModel.class.getDeclaredField("spikeWindows");

        wlanDelayF.setAccessible(true);
        spikesF.setAccessible(true);

        double wlanDelay = (double) wlanDelayF.get(m);
        @SuppressWarnings("unchecked")
        List<double[]> spikes = (List<double[]>) spikesF.get(m);

        assertTrue(wlanDelay >= 0.010 && wlanDelay <= 0.020, "WLAN delay in [10,20] ms");
        assertEquals(3, spikes.size(), "Three spikes");
        for (double[] w : spikes) {
            double duration = w[1] - w[0];
            assertTrue(duration >= 5.0 && duration <= 10.0, "Spike 5–10 s");
        }

        System.out.println("PASSED");
    }

    @Test
    void emergencyResponseSchedulesFiveWanBursts() throws Exception {
        System.out.print("→ Testing EMERGENCY_RESPONSE initialization… ");
        CustomNetworkModel m = new CustomNetworkModel(1, "EMERGENCY_RESPONSE");
        m.initialize();

        Field wanDelayF = CustomNetworkModel.class.getDeclaredField("wanDelay");
        Field spikesF   = CustomNetworkModel.class.getDeclaredField("spikeWindows");

        wanDelayF.setAccessible(true);
        spikesF.setAccessible(true);

        double wanDelay = (double) wanDelayF.get(m);
        @SuppressWarnings("unchecked")
        List<double[]> spikes = (List<double[]>) spikesF.get(m);

        assertTrue(wanDelay >= 0.015 && wanDelay <= 0.025, "WAN delay in [15,25] ms");
        assertEquals(5, spikes.size(), "Five bursts");
        for (double[] w : spikes) {
            double duration = w[1] - w[0];
            assertTrue(duration >= 3.0 && duration <= 7.0, "Burst 3–7 s");
        }

        System.out.println("PASSED");
    }

    @Test
    void autonomousDrivingBandwidthPropagationAndFifo() throws Exception {
        System.out.print("→ Testing bandwidth + propagation + FIFO in AUTONOMOUS_DRIVING… ");
        CustomNetworkModel model = new CustomNetworkModel(1, "AUTONOMOUS_DRIVING");
        model.initialize();

        // override the fields actually used by getUploadDelay
        Field propF   = CustomNetworkModel.class.getDeclaredField("wanDelay");
        Field bwF     = CustomNetworkModel.class.getDeclaredField("wanBandwidth");
        Field spikesF = CustomNetworkModel.class.getDeclaredField("spikeWindows");
        propF.setAccessible(true);
        bwF.setAccessible(true);
        spikesF.setAccessible(true);

        // force 15 ms propagation and 1 MiB/s
        propF.setDouble(model, 0.015);
        bwF.setDouble(model, 1_048_576.0);

        @SuppressWarnings("unchecked")
        List<double[]> spikes = (List<double[]>) spikesF.get(model);
        spikes.clear();

        new Expectations(task) {{
            task.getCloudletFileSize(); result = 1_048_576L; minTimes = 0;
        }};

        double bytes = 1_048_576.0;
        double tx    = (bytes * 8.0) / 1_048_576.0; // = 8.0 s
        double firstExp  = 0.015 + tx;             // 8.015 s
        double secondExp = 0.015 + tx + tx;        // 16.015 s

        double first  = model.getUploadDelay(0, SimSettings.GENERIC_EDGE_DEVICE_ID, task);
        double second = model.getUploadDelay(0, SimSettings.GENERIC_EDGE_DEVICE_ID, task);

        assertEquals(firstExp,  first,  1e-9, "First upload = prop + tx");
        assertEquals(secondExp, second, 1e-9, "Second upload = prop + 2·tx");
        System.out.println("PASSED");
    }

    @Test
    void smartManufacturingBandwidthPropagationAndFifo() throws Exception {
        System.out.print("→ Testing bandwidth + propagation + FIFO in SMART_MANUFACTURING… ");
        CustomNetworkModel model = new CustomNetworkModel(1, "SMART_MANUFACTURING");
        model.initialize();

        Field propF   = CustomNetworkModel.class.getDeclaredField("lanDelay");
        Field bwF     = CustomNetworkModel.class.getDeclaredField("lanBandwidth");
        Field spikesF = CustomNetworkModel.class.getDeclaredField("spikeWindows");
        propF.setAccessible(true);
        bwF.setAccessible(true);
        spikesF.setAccessible(true);

        propF.setDouble(model, 0.005);
        bwF.setDouble(model, 1_048_576.0);

        @SuppressWarnings("unchecked")
        List<double[]> spikes = (List<double[]>) spikesF.get(model);
        spikes.clear();

        new Expectations(task) {{
            task.getCloudletFileSize(); result = 1_048_576L; minTimes = 0;
        }};

        double bytes = 1_048_576.0;
        double tx    = (bytes * 8.0) / 1_048_576.0; // = 8.0 s
        double firstExp  = 0.005 + tx;             // 8.005 s
        double secondExp = 0.005 + tx + tx;        // 16.005 s

        double first  = model.getUploadDelay(0, SimSettings.GENERIC_EDGE_DEVICE_ID, task);
        double second = model.getUploadDelay(0, SimSettings.GENERIC_EDGE_DEVICE_ID, task);

        assertEquals(firstExp,  first,  1e-9, "First upload = prop + tx");
        assertEquals(secondExp, second, 1e-9, "Second upload = prop + 2·tx");
        System.out.println("PASSED");
    }

    @Test
    void emergencyResponseBandwidthPropagationAndFifo() throws Exception {
        System.out.print("→ Testing bandwidth + propagation + FIFO in EMERGENCY_RESPONSE… ");
        CustomNetworkModel model = new CustomNetworkModel(1, "EMERGENCY_RESPONSE");
        model.initialize();

        Field propF   = CustomNetworkModel.class.getDeclaredField("wanDelay");
        Field bwF     = CustomNetworkModel.class.getDeclaredField("wanBandwidth");
        Field spikesF = CustomNetworkModel.class.getDeclaredField("spikeWindows");
        propF.setAccessible(true);
        bwF.setAccessible(true);
        spikesF.setAccessible(true);

        propF.setDouble(model, 0.020);      // 20 ms
        bwF.setDouble(model, 500_000.0);    // e.g. 0.5 MB/s

        @SuppressWarnings("unchecked")
        List<double[]> spikes = (List<double[]>) spikesF.get(model);
        spikes.clear();

        new Expectations(task) {{
            task.getCloudletFileSize(); result = 1_000_000L; minTimes = 0;
        }};

        double bytes = 1_000_000.0;
        double tx    = (bytes * 8.0) / 500_000.0; // = 16.0 s
        double firstExp  = 0.020 + tx;            // 16.020 s
        double secondExp = 0.020 + tx + tx;       // 32.020 s

        double first  = model.getUploadDelay(0, SimSettings.GENERIC_EDGE_DEVICE_ID, task);
        double second = model.getUploadDelay(0, SimSettings.GENERIC_EDGE_DEVICE_ID, task);

        assertEquals(firstExp,  first,  1e-9, "First upload = prop + tx");
        assertEquals(secondExp, second, 1e-9, "Second upload = prop + 2·tx");
        System.out.println("PASSED");
    }


    @Test
    void unknownScenarioThrows() {
        System.out.print("→ Testing unknown scenario exception… ");
        CustomNetworkModel m = new CustomNetworkModel(1, "UNKNOWN");
        assertThrows(IllegalArgumentException.class, m::initialize, "Should throw");
        System.out.println("PASSED");
    }

    @AfterAll
    void afterAll() {
        System.out.println("=== Finished CustomNetworkModelTest ===");
    }
}
