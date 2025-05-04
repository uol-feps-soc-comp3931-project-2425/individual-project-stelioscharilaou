package edu.boun.edgecloudsim.applications.RR2;

import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.network.MM1Queue;
import edu.boun.edgecloudsim.core.SimSettings;
import org.cloudbus.cloudsim.core.CloudSim;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CustomNetworkModel applies scenario-based base delays, bandwidth-driven
 * transmission times, dynamic burst congestion windows, and M/M/1 queueing
 * for WLAN/WAN links.
 */
public class CustomNetworkModel extends MM1Queue {
    private double lanDelay;
    private double wanDelay;
    private double wlanBandwidth;
    private final List<double[]> burstWindows = new ArrayList<>();
    private final ThreadLocalRandom rng = ThreadLocalRandom.current();

    /**
     * Constructor.
     *
     * @param devices  number of mobile devices in simulation
     * @param scenario scenario name (e.g. AUTONOMOUS_PERCEPTION)
     */
    public CustomNetworkModel(int devices, String scenario) {
        super(devices, scenario);
    }

    /**
     * Initializes base delays, bandwidth per scenario, and sets up
     * congestion burst windows for emergency scenarios.
     */
    @Override
    public void initialize() {
        super.initialize();
        double simEnd = SimSettings.getInstance().getSimulationTime();

        switch (simScenario) {
            case "EMERGENCY_RESPONSE", "EMERGENCY_ALERT", "VIDEO_SURVEILLANCE" -> {
                lanDelay = rng.nextDouble(15, 25) / 1000.0;
                wanDelay = rng.nextDouble(15, 25) / 1000.0;
                wlanBandwidth = 1000 * 1024;  // ~1 MB/s
                for (int i = 0; i < 3; i++) {
                    double start = rng.nextDouble(0, simEnd - 5);
                    double end = start + rng.nextDouble(5, 10);
                    burstWindows.add(new double[]{start, end});
                }
            }
            case "SMART_MANUFACTURING", "MANUFACTURING_MONITOR", "PREDICTIVE_MAINTENANCE" -> {
                lanDelay = rng.nextDouble(1, 5) / 1000.0;
                wanDelay = rng.nextDouble(1, 5) / 1000.0;
                wlanBandwidth = 5000 * 1024;  // ~5 MB/s
            }
            case "AUTONOMOUS_DRIVING", "AUTONOMOUS_PERCEPTION", "ROUTE_PLANNING" -> {
                lanDelay = rng.nextDouble(10, 20) / 1000.0;
                wanDelay = rng.nextDouble(10, 20) / 1000.0;
                wlanBandwidth = 2000 * 1024;  // ~2 MB/s
            }
            default -> {
                SimSettings ss = SimSettings.getInstance();
                lanDelay = ss.getInternalLanDelay();
                wanDelay = ss.getWanPropagationDelay();
                wlanBandwidth = ss.getWlanBandwidth() * 1024;
            }
        }
    }

    private boolean inBurst(double now) {
        return burstWindows.stream().anyMatch(w -> now >= w[0] && now <= w[1]);
    }

    @Override
    public double getUploadDelay(int src, int dst, Task task) {
        // constant LAN if local
        if (src == dst && src == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            return lanDelay;
        }

        double size = task.getCloudletFileSize() + task.getCloudletOutputSize();
        return calculateDelay(dst, size);
    }


    @Override
    public double getDownloadDelay(int src, int dst, Task task) {
        if (src == dst && src == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            return lanDelay;
        }

        double size = task.getCloudletOutputSize();
        return calculateDelay(src, size);
    }

    private double calculateDelay(int dst, double size) {
        double transmission = size / wlanBandwidth;
        double totalDelay = lanDelay + transmission;

        if (dst == SimSettings.CLOUD_DATACENTER_ID) {
            totalDelay += wanDelay;
        }

        if (isEmergencyBurst()) {
            totalDelay += rng.nextDouble(50, 100) / 1000.0;
        }
        return totalDelay;
    }

    private boolean isEmergencyBurst() {
        return (simScenario.startsWith("EMERGENCY" ) || simScenario.equals("VIDEO_SURVEILLANCE"))
                && inBurst(CloudSim.clock());
    }

}
