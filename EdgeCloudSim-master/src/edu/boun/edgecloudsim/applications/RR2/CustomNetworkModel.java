package edu.boun.edgecloudsim.applications.RR2;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.Location;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Scenario‐specific network model:
 * 1) Smart Manufacturing – pure LAN, 1–5 ms, ~100 Mbps
 * 2) Autonomous Driving – V2X WAN, 10–20 ms (50 ms spikes), 50 Mbps
 * 3) Emergency Response – mobile WAN, 15–25 ms (60 ms spikes), 8 Mbps
 */
public class CustomNetworkModel extends NetworkModel {
    private double lanDelay, wanDelay, wlanDelay;
    private double lanBandwidth, wlanBandwidth, wanBandwidth;
    private final ThreadLocalRandom rng = ThreadLocalRandom.current();

    // for congestion spikes
    private final List<double[]> spikeWindows = new ArrayList<>();

    // simple per‐link FIFO
    private final Map<Integer, Double> nextUpFree  = new ConcurrentHashMap<>();
    private final Map<Integer, Double> nextDownFree= new ConcurrentHashMap<>();

    public CustomNetworkModel(int devices, String scenario) {
        super(devices, scenario);
    }

    @Override
    public void initialize() {
        double simEnd = SimSettings.getInstance().getSimulationTime();

        switch (simScenario) {
            // 1) Smart Manufacturing
            case "SMART_MANUFACTURING", "MANUFACTURING_MONITOR", "PREDICTIVE_MAINTENANCE" -> {
                // 1–5 ms LAN, 100 Mbps, no WLAN nor WAN queuing
                lanDelay      = rng.nextDouble(1, 5) / 1_000.0;
                lanBandwidth  = 100.0 * 1_000_000.0 / 8.0; // bytes/sec
                wlanBandwidth = 50.0  * 1_000_000.0 / 8.0;                   // 50 Mbps (reasonable local WLAN)
                wanBandwidth  = 10.0  * 1_000_000.0 / 8.0;
            }

            // 2) Autonomous Driving
            case "AUTONOMOUS_DRIVING", "AUTONOMOUS_PERCEPTION", "ROUTE_PLANNING" -> {
                // V2X wireless: 10–20 ms base, 50 ms spikes, ~50 Mbps
                wlanDelay     = rng.nextDouble(10, 20)   / 1_000.0;
                wlanBandwidth = 50.0  * 1_000_000.0 / 8.0;
                lanBandwidth  = 10.0  * 1_000_000.0 / 8.0;                   // fallback LAN
                wanBandwidth  = 5.0   * 1_000_000.0 / 8.0;

                // schedule 3 random congestion spikes
                for (int i = 0; i < 3; i++) {
                    double start = rng.nextDouble(0, simEnd - 30);
                    spikeWindows.add(new double[]{start, start + rng.nextDouble(5, 10)});
                }
            }

            // 3) Emergency Response
            case "EMERGENCY_RESPONSE", "EMERGENCY_ALERT", "VIDEO_SURVEILLANCE" -> {
                // Mobile WAN: 15–25 ms base, 60 ms spikes, ~8 Mbps
                wanDelay      = rng.nextDouble(15, 25) / 1_000.0;            // 15–25 ms WAN delay (realistic for 4G/5G)
                wanBandwidth  = 50.0  * 1_000_000.0 / 8.0;                   // upgraded to 50 Mbps WAN
                wlanBandwidth = 30.0  * 1_000_000.0 / 8.0;                   // upgraded to 30 Mbps WLAN
                lanBandwidth  = 100.0 * 1_000_000.0 / 8.0;                   // LAN fallback at 100 Mbps

                for (int i = 0; i < 5; i++) {
                    double start = rng.nextDouble(0, simEnd - 20);
                    spikeWindows.add(new double[]{start, start + rng.nextDouble(3, 7)});
                }
            }

            default -> throw new IllegalArgumentException("Unknown scenario "+simScenario);
        }
    }

    // helper to check if we are inside a congestion spike
    private boolean inSpike(double t) {
        return spikeWindows.stream().anyMatch(w -> t >= w[0] && t <= w[1]);
    }

    @Override
    public double getUploadDelay(int src, int dst, Task task) {
        double now = CloudSim.clock();
        double bytes = task.getCloudletFileSize();
        double prop  = (lanBandwidth>0 ? lanDelay : wanDelay);
        double bw    = (lanBandwidth>0 ? lanBandwidth : wanBandwidth);
        double tx    = (bytes * 8.0) / bw;

        // if in a spike, add extra latency
        if (inSpike(now)) prop +=  (simScenario.startsWith("AUTONOMOUS") ? 0.030 : 0.035);

        double freeAt = nextUpFree.getOrDefault(dst, now);
        double start  = Math.max(now, freeAt);
        double finish = start + tx;
        nextUpFree.put(dst, finish);

        return (start-now) + tx + prop;
    }

    @Override
    public double getDownloadDelay(int src, int dst, Task task) {
        double now = CloudSim.clock();
        double bytes = task.getCloudletOutputSize();
        double prop  = (lanBandwidth>0 ? lanDelay : wanDelay);
        double bw    = (lanBandwidth>0 ? lanBandwidth : wanBandwidth);
        double tx    = (bytes * 8.0) / bw;

        if (inSpike(now)) prop += (simScenario.startsWith("AUTONOMOUS") ? 0.030 : 0.035);

        double freeAt = nextDownFree.getOrDefault(dst, now);
        double start  = Math.max(now, freeAt);
        double finish = start + tx;
        nextDownFree.put(dst, finish);

        return (start-now) + tx + prop;
    }

    @Override
    public void uploadStarted(Location accessPointLocation, int destDeviceId) {

    }

    @Override
    public void uploadFinished(Location accessPointLocation, int destDeviceId) {

    }

    @Override
    public void downloadStarted(Location accessPointLocation, int sourceDeviceId) {

    }

    @Override
    public void downloadFinished(Location accessPointLocation, int sourceDeviceId) {

    }
}
