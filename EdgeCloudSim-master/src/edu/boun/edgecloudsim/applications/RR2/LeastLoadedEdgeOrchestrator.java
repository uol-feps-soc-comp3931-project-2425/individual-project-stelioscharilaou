/*
 * Title:        EdgeCloudSim - Least Loaded Edge Orchestrator
 *
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.RR2;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileVM;

import java.util.List;

public class LeastLoadedEdgeOrchestrator extends EdgeOrchestrator {
    private int numberOfHost;

    public LeastLoadedEdgeOrchestrator(String _policy, String _simScenario) {
        super(_policy, _simScenario);
    }

    @Override
    public void initialize() {
        numberOfHost = SimSettings.getInstance().getNumOfEdgeHosts();
    }

    @Override
    public int getDeviceToOffload(Task task) {
        int result = SimSettings.GENERIC_EDGE_DEVICE_ID;

        // Start with maximum possible utilization
        double selectedDeviceCapacity = 100;

        // Iterate through all edge hosts to find least loaded
        for(int hostIndex = 0; hostIndex < numberOfHost; hostIndex++) {
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
            double totalHostUtilization = 0;

            // Calculate total utilization for all VMs on this host
            for (EdgeVM edgeVM : vmArray) {
                totalHostUtilization += edgeVM.getCloudletScheduler()
                        .getTotalUtilizationOfCpu(CloudSim.clock());
            }

            // Calculate average host utilization
            double avgHostUtilization = totalHostUtilization / vmArray.size();

            // Update selection if this host has lower utilization
            if(avgHostUtilization < selectedDeviceCapacity) {
                selectedDeviceCapacity = avgHostUtilization;
                result = hostIndex;
            }
        }

        // Check if we should offload to cloud instead
        // Add threshold check for cloud offloading
//        if(selectedDeviceCapacity > 80) { // 80% utilization threshold
//            result = SimSettings.CLOUD_DATACENTER_ID;
//        }
        return result;
    }

    @Override
    public Vm getVmToOffload(Task task, int deviceId) {
        Vm selectedVM = null;

        if(deviceId == SimSettings.CLOUD_DATACENTER_ID) {
            // Select VM on cloud devices via Least Loaded algorithm
            double selectedVmCapacity = 0; // Start with min value
            Host host = SimManager.getInstance().getCloudServerManager().getDatacenter().getHostList().get(0);
            List<CloudVM> vmList = SimManager.getInstance().getCloudServerManager().getVmList(0); // Use host index 0

            for(CloudVM vm : vmList) {
                double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu())
                        .predictUtilization(vm.getVmType());
                double targetVmCapacity = 100.0 - vm.getCloudletScheduler()
                        .getTotalUtilizationOfCpu(CloudSim.clock());

                if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity) {
                    selectedVM = vm;
                    selectedVmCapacity = targetVmCapacity;
                }
            }
        }
        else if(deviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            // Select VM on edge devices via Least Loaded algorithm
            double selectedVmCapacity = 0;
            for(int hostIndex=0; hostIndex<numberOfHost; hostIndex++) {
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
                for(EdgeVM vm : vmArray) {
                    double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu())
                            .predictUtilization(vm.getVmType());
                    double targetVmCapacity = 100.0 - vm.getCloudletScheduler()
                            .getTotalUtilizationOfCpu(CloudSim.clock());

                    if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity) {
                        selectedVM = vm;
                        selectedVmCapacity = targetVmCapacity;
                    }
                }
            }
        }
        else {
            // For specific host
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(deviceId);
            double selectedVmCapacity = 0;

            for(EdgeVM vm : vmArray) {
                double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu())
                        .predictUtilization(vm.getVmType());
                double targetVmCapacity = 100.0 - vm.getCloudletScheduler()
                        .getTotalUtilizationOfCpu(CloudSim.clock());

                if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity) {
                    selectedVM = vm;
                    selectedVmCapacity = targetVmCapacity;
                }
            }
        }

        return selectedVM;
    }

    private <T extends Vm> Vm findLeastLoadedVM(Task task, List<T> vmList, Vm currentBestVM, double currentBestCapacity) {
        for(Vm vm : vmList) {
            double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(getVmType(vm));
            double availableCapacity = 100.0 - vm.getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());

            if(requiredCapacity <= availableCapacity && availableCapacity > currentBestCapacity) {
                currentBestVM = vm;
                currentBestCapacity = availableCapacity;
            }
        }
        return currentBestVM;
    }

    private SimSettings.VM_TYPES getVmType(Vm vm) {
        if (vm instanceof CloudVM) {
            return ((CloudVM)vm).getVmType();
        }
        else if (vm instanceof EdgeVM) {
            return SimSettings.VM_TYPES.EDGE_VM;
        }
        else if (vm instanceof MobileVM) {
            return SimSettings.VM_TYPES.MOBILE_VM;
        }
        else {
            SimLogger.printLine("Unknown VM type! Terminating simulation...");
            System.exit(1);
            return null;
        }
    }

    private double getMinEdgeUtilization() {
        double minUtilization = 100.0;
        for(int hostIndex = 0; hostIndex < numberOfHost; hostIndex++) {
            List<EdgeVM> vmList = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
            double hostUtilization = 0;
            for(EdgeVM vm : vmList) {
                hostUtilization += vm.getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
            }
            hostUtilization /= vmList.size();
            minUtilization = Math.min(minUtilization, hostUtilization);
        }
        return minUtilization;
    }

    private double getMinCloudUtilization() {
        double minUtilization = 100.0;
        List<Host> hostList = SimManager.getInstance().getCloudServerManager().getDatacenter().getHostList();
        for(Host host : hostList) {
            List<CloudVM> vmList = SimManager.getInstance().getCloudServerManager().getVmList(host.getId());
            double hostUtilization = 0;
            for(CloudVM vm : vmList) {
                hostUtilization += vm.getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
            }
            hostUtilization /= vmList.size();
            minUtilization = Math.min(minUtilization, hostUtilization);
        }
        return minUtilization;
    }

    @Override
    public void processEvent(SimEvent ev) {
        // Nothing to do
    }

    @Override
    public void shutdownEntity() {
        // Nothing to do
    }

    @Override
    public void startEntity() {
        // Nothing to do
    }
}