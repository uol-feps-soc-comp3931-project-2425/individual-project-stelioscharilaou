package edu.boun.edgecloudsim.mobility;

import edu.boun.edgecloudsim.utils.Location;

/**
 * Places every device at a static WLAN cell (ID=0).
 */
public class StaticMeshMobility extends NomadicMobility {
    private final Location[] locs;

    public StaticMeshMobility(int numDevices, double simulationTime) {
        super(numDevices, simulationTime);
        locs = new Location[numDevices];
        for (int i = 0; i < numDevices; i++) {
            // all devices serve on WLAN ID=0; coordinates ignored
            locs[i] = new Location(i, 0, 0, 0);
        }
    }

    @Override
    public Location getLocation(int deviceId, double time) {
        return locs[deviceId];
    }
}
