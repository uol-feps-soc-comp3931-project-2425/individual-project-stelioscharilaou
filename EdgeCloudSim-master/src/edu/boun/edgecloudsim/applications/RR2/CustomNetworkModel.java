package edu.boun.edgecloudsim.applications.RR2;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.network.MM1Queue;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.Location;
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

    private boolean isMobileDevice(int deviceId) {
        return deviceId >= 0 && deviceId < SimSettings.getInstance().getMaxNumOfMobileDev();
    }


    @Override
    public double getUploadDelay(int sourceDeviceId, int destDeviceId, Task task) {
        double delay = 0;

        if (isMobileDevice(sourceDeviceId)) {
            Location accessPointLocation = SimManager.getInstance()
                    .getMobilityModel()
                    .getLocation(sourceDeviceId, CloudSim.clock());

            if (destDeviceId == SimSettings.CLOUD_DATACENTER_ID) {
                double wlanDelay = super.getUploadDelay(sourceDeviceId, destDeviceId, task);
                double wanDelay = getWanUploadDelay(accessPointLocation, CloudSim.clock() + wlanDelay);
                if (wlanDelay > 0 && wanDelay > 0)
                    delay = wlanDelay + wanDelay;
            }
            else if (destDeviceId == SimSettings.EDGE_ORCHESTRATOR_ID) {
                delay = super.getUploadDelay(sourceDeviceId, destDeviceId, task) +
                        SimSettings.getInstance().getInternalLanDelay();
            }
            else if (destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
                delay = super.getUploadDelay(sourceDeviceId, destDeviceId, task);
            }
        }

        return delay;
    }


    @Override
    public double getDownloadDelay(int sourceDeviceId, int destDeviceId, Task task) {
        if (sourceDeviceId == SimSettings.EDGE_ORCHESTRATOR_ID &&
                destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            return SimSettings.getInstance().getInternalLanDelay();
        }

        double delay = 0;

        if (isMobileDevice(destDeviceId)) {
            Location accessPointLocation = SimManager.getInstance()
                    .getMobilityModel()
                    .getLocation(destDeviceId, CloudSim.clock());

            if (sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID) {
                double wlanDelay = super.getDownloadDelay(sourceDeviceId, destDeviceId, task);
                double wanDelay = getWanDownloadDelay(accessPointLocation, CloudSim.clock() + wlanDelay);
                if (wlanDelay > 0 && wanDelay > 0)
                    delay = wlanDelay + wanDelay;
            }
            else {
                delay = super.getDownloadDelay(sourceDeviceId, destDeviceId, task);

                EdgeHost host = (EdgeHost)(SimManager.getInstance()
                        .getEdgeServerManager()
                        .getDatacenterList()
                        .get(sourceDeviceId)
                        .getHostList()
                        .get(0));

                if (host.getLocation().getServingWlanId() != accessPointLocation.getServingWlanId()) {
                    delay += SimSettings.getInstance().getInternalLanDelay() * 2;
                }
            }
        }

        return delay;
    }


    private boolean isEmergencyBurst() {
        return (simScenario.startsWith("EMERGENCY" ) || simScenario.equals("VIDEO_SURVEILLANCE"))
                && inBurst(CloudSim.clock());
    }

}
