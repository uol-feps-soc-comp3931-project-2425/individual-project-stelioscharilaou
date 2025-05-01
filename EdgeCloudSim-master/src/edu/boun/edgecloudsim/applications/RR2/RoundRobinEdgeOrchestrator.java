package edu.boun.edgecloudsim.applications.RR2;

import java.util.List;


import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;

import org.cloudbus.cloudsim.core.SimEvent;


import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeVM;

import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.utils.SimLogger;

public class RoundRobinEdgeOrchestrator extends EdgeOrchestrator {
	private int currentHostIndex;       // For round robin host selection
	private int[] currentVmIndexes;       // For round robin VM selection per host
	private int numberOfHost;             // Total number of edge hosts

	public RoundRobinEdgeOrchestrator(String _policy, String _simScenario) {
		super(_policy, _simScenario);
	}

	@Override
	public void initialize() {
		numberOfHost = SimSettings.getInstance().getNumOfEdgeHosts();
		currentHostIndex = 0;
		currentVmIndexes = new int[numberOfHost];
		for (int i = 0; i < numberOfHost; i++) {
			currentVmIndexes[i] = -1; // Initialise VM indexes for each host
		}
		SimLogger.printLine("Initialised Round Robin Orchestrator with " + numberOfHost + " edge hosts.");
	}

	@Override
	public int getDeviceToOffload(Task task) {
		// Create a dummy task with predetermined upload and download file sizes.
//		Task dummyTask = new Task(0, 0, 0, 0, 128, 128,
//				new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());

		// Use the dummy task to simulate network conditions.
//		double networkDelay = SimManager.getInstance().getNetworkModel()
//				.getUploadDelay(task.getMobileDeviceId(), SimSettings.GENERIC_EDGE_DEVICE_ID, dummyTask);
//		SimLogger.printLine("Simulated network delay for dummy task: " + networkDelay);

		// As this orchestrator is intended to run on edge nodes only, use round robin to select an edge host.
		int result = SimSettings.GENERIC_EDGE_DEVICE_ID + (currentHostIndex % numberOfHost);
		currentHostIndex++; // Increment to point to the next host

//		SimLogger.printLine("Task assigned to edge device ID: " + result);
		return result;
	}

	@Override
	public Vm getVmToOffload(Task task, int deviceId) {
		Vm selectedVM = null;
		if (deviceId >= SimSettings.GENERIC_EDGE_DEVICE_ID) {
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

//		if (selectedVM != null) {
//			SimLogger.printLine("Task assigned to edge VM ID: " + selectedVM.getId());
//		} else {
//			SimLogger.printLine("No suitable VM found for the task on edge device: " + deviceId);
//		}
		return selectedVM;
	}

	@Override
	public void processEvent(SimEvent arg0) {
		// Not used in this implementation.
	}

	@Override
	public void shutdownEntity() {
		// Not used in this implementation.
	}

	@Override
	public void startEntity() {
		// Not used in this implementation.
	}
}
