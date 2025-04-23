package edu.boun.edgecloudsim.applications.RR2;

import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.network.MM1Queue;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.utils.Location;
import org.cloudbus.cloudsim.core.CloudSim;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CustomNetworkModel applies scenario-based base delays, dynamic burst
 * congestion windows, and M/M/1 queueing for WLAN/WAN links.
 */
public class CustomNetworkModel extends MM1Queue {
    private double lanDelay;
    private double wanDelay;
    private int wlanBandwidth;
    private final List<double[]> burstWindows = new ArrayList<>();
    private final ThreadLocalRandom rng = ThreadLocalRandom.current();

    /**
     * Constructor.
     *
     * @param devices  number of mobile devices in simulation
     * @param scenario scenario name (e.g., SMART_MANUFACTURING)
     */
    public CustomNetworkModel(int devices, String scenario) {
        super(devices, scenario);
    }

    /**
     * Initializes base delays and bandwidth per scenario, and sets up
     * congestion burst windows for emergency response.
     */
    @Override
    public void initialize() {
        super.initialize();
        double simEnd = SimSettings.getInstance().getSimulationTime();

        switch (simScenario) {
            case "EMERGENCY_RESPONSE" -> {
                // base propagation delays (15–25 ms)
                lanDelay = rng.nextDouble(15, 25) / 1000.0;
                wanDelay = rng.nextDouble(15, 25) / 1000.0;
                wlanBandwidth = 1000;
                // schedule three random 5–10 s congestion bursts
                for (int i = 0; i < 3; i++) {
                    double start = rng.nextDouble(0, simEnd - 5);
                    double end = start + rng.nextDouble(5, 10);
                    burstWindows.add(new double[]{start, end});
                }
            }
            case "SMART_MANUFACTURING" -> {
                // base propagation delays (1–5 ms)
                lanDelay = rng.nextDouble(1, 5) / 1000.0;
                wanDelay = rng.nextDouble(1, 5) / 1000.0;
                wlanBandwidth = 5000;
            }
            case "AUTONOMOUS_DRIVING" -> {
                // base propagation delays (10–20 ms)
                lanDelay = rng.nextDouble(10, 20) / 1000.0;
                wanDelay = rng.nextDouble(10, 20) / 1000.0;
                wlanBandwidth = 2000;
            }
            default -> {
                // fallback to config defaults
                SimSettings ss = SimSettings.getInstance();
                lanDelay = ss.getInternalLanDelay();
                wanDelay = ss.getWanPropagationDelay();
                wlanBandwidth = ss.getWlanBandwidth();
            }
        }
    }

    /**
     * Checks if the given time is within any defined burst window.
     *
     * @param now current simulation time in seconds
     * @return true if in a burst window, false otherwise
     */
    private boolean inBurst(double now) {
        for (double[] window : burstWindows) {
            if (now >= window[0] && now <= window[1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates upload delay including M/M/1 queueing and optional
     * emergency burst congestion.
     *
     * @param src  source device ID
     * @param dst  destination device ID
     * @param task the Task being uploaded
     * @return delay in seconds
     */
    @Override
    public double getUploadDelay(int src, int dst, Task task) {
        // direct edge-to-edge LAN loopback
        if (src == dst && src == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            return lanDelay;
        }
        // compute WLAN queue + transmission
        Location loc = SimManager.getInstance()
                .getMobilityModel()
                .getLocation(src, CloudSim.clock());
        double wlan = super.getWlanUploadDelay(loc, CloudSim.clock());
        double wan = dst == SimSettings.CLOUD_DATACENTER_ID
                ? super.getWanUploadDelay(loc, CloudSim.clock() + wlan)
                : 0;
        double base = wlan + wan;
        // extra congestion delay during bursts
        if ("EMERGENCY_RESPONSE".equals(simScenario) && inBurst(CloudSim.clock())) {
            base += rng.nextDouble(50, 100) / 1000.0;
        }
        return base;
    }

    /**
     * Calculates download delay including M/M/1 queueing and optional
     * emergency burst congestion.
     *
     * @param src  source device ID
     * @param dst  destination device ID
     * @param task the Task being downloaded
     * @return delay in seconds
     */
    @Override
    public double getDownloadDelay(int src, int dst, Task task) {
        // direct edge-to-edge LAN loopback
        if (src == dst && src == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            return lanDelay;
        }
        // compute WLAN queue + transmission
        Location loc = SimManager.getInstance()
                .getMobilityModel()
                .getLocation(dst, CloudSim.clock());
        double wlan = super.getWlanDownloadDelay(loc, CloudSim.clock());
        double wan = src == SimSettings.CLOUD_DATACENTER_ID
                ? super.getWanDownloadDelay(loc, CloudSim.clock() + wlan)
                : 0;
        double base = wlan + wan;
        // extra congestion delay during bursts
        if ("EMERGENCY_RESPONSE".equals(simScenario) && inBurst(CloudSim.clock())) {
            base += rng.nextDouble(50, 100) / 1000.0;
        }
        return base;
    }
}
