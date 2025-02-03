/*
 * Title:        EdgeCloudSim - Round Robin Edge Orchestrator
 *
 * Description:
 * Implements a Round Robin algorithm to offload tasks to devices and VMs.
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.edge_orchestrator;

import java.util.List;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.SimEvent;
import edu.boun.edgecloudsim.cloud_server.CloudServerManager;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.utils.SimLogger;

public class RoundRobinEdgeOrchestrator extends EdgeOrchestrator {
	private int currentHostIndex; // For Round Robin host selection
	private int[] currentVmIndexes; // For Round Robin VM selection per host
	private int numberOfHost; // Total number of hosts

	public RoundRobinEdgeOrchestrator(String _policy, String _simScenario) {
		super(_policy, _simScenario);
	}

	@Override
	public void initialize() {
		numberOfHost = SimSettings.getInstance().getNumOfEdgeHosts();
		currentHostIndex = 0;
		currentVmIndexes = new int[numberOfHost];
		for (int i = 0; i < numberOfHost; i++) {
			currentVmIndexes[i] = -1; // Initialize VM indexes
		}
		SimLogger.printLine("Initialized Round Robin Orchestrator with " + numberOfHost + " edge hosts.");
	}

	@Override
	public int getDeviceToOffload(Task task) {
		int result;

		// Round Robin for edge devices only
		result = SimSettings.GENERIC_EDGE_DEVICE_ID + (currentHostIndex % numberOfHost);
		currentHostIndex++; // Increment to point to the next host

		SimLogger.printLine("Task assigned to edge device ID: " + result);
		return result;
	}

	@Override
	public Vm getVmToOffload(Task task, int deviceId) {
		Vm selectedVM = null;

		if (deviceId >= SimSettings.GENERIC_EDGE_DEVICE_ID) {
			// Round Robin for edge VMs
			int hostId = deviceId - SimSettings.GENERIC_EDGE_DEVICE_ID;
			if (hostId >= 0 && hostId < numberOfHost) { // Validate host ID range
				List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostId);
				if (!vmArray.isEmpty()) {
					int vmIndex = (currentVmIndexes[hostId] + 1) % vmArray.size();
					selectedVM = vmArray.get(vmIndex);
					currentVmIndexes[hostId] = vmIndex; // Update the current VM index for the host
				} else {
					SimLogger.printLine("Error: No VMs available on edge host ID: " + hostId);
				}
			} else {
				SimLogger.printLine("Error: Invalid host ID for edge device: " + hostId);
			}
		} else {
			SimLogger.printLine("Unknown device ID! Terminating simulation...");
			System.exit(0);
		}

		SimLogger.printLine("Task assigned to edge VM ID: " + (selectedVM != null ? selectedVM.getId() : -1));
		return selectedVM;
	}
	@Override
	public void processEvent(SimEvent arg0) {
		// Not used in this implementation
	}

	@Override
	public void shutdownEntity() {
		// Not used in this implementation
	}

	@Override
	public void startEntity() {
		// Not used in this implementation
	}

}
