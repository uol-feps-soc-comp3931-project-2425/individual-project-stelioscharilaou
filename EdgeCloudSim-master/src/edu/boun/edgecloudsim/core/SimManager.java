/*
 * Title:        EdgeCloudSim - Simulation Manager
 * 
 * Description: 
 * SimManager is an singleton class providing many abstract classeses such as
 * Network Model, Mobility Model, Edge Orchestrator to other modules
 * Critical simulation related information would be gathered via this class 
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.core;

import java.io.IOException;
import java.util.List;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.edge_server.EdgeVmAllocationPolicy_Custom;
import edu.boun.edgecloudsim.cloud_server.CloudServerManager;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.SimLogger;

public class SimManager extends SimEntity {
	private static final int CREATE_TASK = 0;
	private static final int CHECK_ALL_VM = 1;
	private static final int GET_LOAD_LOG = 2;
	private static final int PRINT_PROGRESS = 3;
	private static final int STOP_SIMULATION = 4;
	
	private String simScenario;
	private String orchestratorPolicy;
	private int numOfMobileDevice;
	private NetworkModel networkModel;
	private MobilityModel mobilityModel;
	private ScenarioFactory scenarioFactory;
	private EdgeOrchestrator edgeOrchestrator;
	private EdgeServerManager edgeServerManager;
	private CloudServerManager cloudServerManager;
	private MobileServerManager mobileServerManager;
	private LoadGeneratorModel loadGeneratorModel;
	private MobileDeviceManager mobileDeviceManager;
	
	private static SimManager instance = null;
	
	public SimManager(ScenarioFactory _scenarioFactory, int _numOfMobileDevice, String _simScenario, String _orchestratorPolicy) throws Exception {
		super("SimManager");
		simScenario = _simScenario;
		scenarioFactory = _scenarioFactory;
		numOfMobileDevice = _numOfMobileDevice;
		orchestratorPolicy = _orchestratorPolicy;

		SimLogger.print("Creating tasks...");
		loadGeneratorModel = scenarioFactory.getLoadGeneratorModel();
		loadGeneratorModel.initializeModel();
		SimLogger.printLine("Done, ");
		
		SimLogger.print("Creating device locations...");
		mobilityModel = scenarioFactory.getMobilityModel();
		mobilityModel.initialize();
		SimLogger.printLine("Done.");

		//Generate network model
		networkModel = scenarioFactory.getNetworkModel();
		networkModel.initialize();
		
		//Generate edge orchestrator
		edgeOrchestrator = scenarioFactory.getEdgeOrchestrator();
		edgeOrchestrator.initialize();
		
		//Create Physical Servers
		edgeServerManager = scenarioFactory.getEdgeServerManager();
		edgeServerManager.initialize();
		
		//Create Physical Servers on cloud
		cloudServerManager = scenarioFactory.getCloudServerManager();
		cloudServerManager.initialize();
		
		//Create Physical Servers on mobile devices
		mobileServerManager = scenarioFactory.getMobileServerManager();
		mobileServerManager.initialize();

		//Create Client Manager
		mobileDeviceManager = scenarioFactory.getMobileDeviceManager();
		mobileDeviceManager.initialize();

		// ────────────────────────────────────────────────
		// Validation Section: print out all of the “big” knobs
		SimLogger.printLine("=== VALIDATION ===");
		SimLogger.printLine(" Scenario       : " + simScenario);
		SimLogger.printLine(" Orchestrator   : " + orchestratorPolicy);
		SimLogger.printLine(" # Devices      : " + numOfMobileDevice);
		SimLogger.printLine(String.format(
				" Simulation     : %.1f s   (warm-up: %.1f s)",
				SimSettings.getInstance().getSimulationTime(),
				SimSettings.getInstance().getWarmUpPeriod()));
		SimLogger.printLine(" # Edge DCs     : "
				+ SimSettings.getInstance().getNumOfEdgeDatacenters());
		SimLogger.printLine(" # Edge Hosts   : "
				+ SimSettings.getInstance().getNumOfEdgeHosts());
		int totalEdgeVMs = SimSettings.getInstance().getNumOfEdgeVMs();
		int hosts         = SimSettings.getInstance().getNumOfEdgeHosts();
		SimLogger.printLine(" VMs per host   : "
				+ (hosts > 0 ? totalEdgeVMs / hosts : 0));
		SimLogger.printLine(String.format(
				" Network delays : LAN=%.3f s   WAN=%.3f s   WLAN-BW=%d kbps",
				SimSettings.getInstance().getInternalLanDelay(),
				SimSettings.getInstance().getWanPropagationDelay(),
				SimSettings.getInstance().getWlanBandwidth() / 1000));
		SimLogger.printLine("==================");
		// ────────────────────────────────────────────────

		instance = this;
	}
	
	public static SimManager getInstance(){
		return instance;
	}
	
	/**
	 * Triggering CloudSim to start simulation
	 */
	public void startSimulation() throws Exception{
		//Starts the simulation
		SimLogger.print(super.getName()+" is starting...");
		
		//Start Edge Datacenters & Generate VMs
		edgeServerManager.startDatacenters();
		edgeServerManager.createVmList(mobileDeviceManager.getId());
		
		//Start Edge Datacenters & Generate VMs
		cloudServerManager.startDatacenters();
		cloudServerManager.createVmList(mobileDeviceManager.getId());
		
		//Start Mobile Datacenters & Generate VMs
		mobileServerManager.startDatacenters();
		mobileServerManager.createVmList(mobileDeviceManager.getId());
		
		CloudSim.startSimulation();
	}

	public String getSimulationScenario(){
		return simScenario;
	}

	public String getOrchestratorPolicy(){
		return orchestratorPolicy;
	}
	
	public ScenarioFactory getScenarioFactory(){
		return scenarioFactory;
	}
	
	public int getNumOfMobileDevice(){
		return numOfMobileDevice;
	}
	
	public NetworkModel getNetworkModel(){
		return networkModel;
	}

	public MobilityModel getMobilityModel(){
		return mobilityModel;
	}
	
	public EdgeOrchestrator getEdgeOrchestrator(){
		return edgeOrchestrator;
	}
	
	public EdgeServerManager getEdgeServerManager(){
		return edgeServerManager;
	}
	
	public CloudServerManager getCloudServerManager(){
		return cloudServerManager;
	}
	
	public MobileServerManager getMobileServerManager(){
		return mobileServerManager;
	}

	public LoadGeneratorModel getLoadGeneratorModel(){
		return loadGeneratorModel;
	}
	
	public MobileDeviceManager getMobileDeviceManager(){
		return mobileDeviceManager;
	}
	
	@Override
	public void startEntity() {
		int hostCounter=0;

		for(int i= 0; i<edgeServerManager.getDatacenterList().size(); i++) {
			List<? extends Host> list = edgeServerManager.getDatacenterList().get(i).getHostList();
			for (int j=0; j < list.size(); j++) {
				mobileDeviceManager.submitVmList(edgeServerManager.getVmList(hostCounter));
				hostCounter++;
			}
		}
		
		for(int i = 0; i<SimSettings.getInstance().getNumOfCloudHost(); i++) {
			mobileDeviceManager.submitVmList(cloudServerManager.getVmList(i));
		}

		for(int i=0; i<numOfMobileDevice; i++){
			if(mobileServerManager.getVmList(i) != null)
				mobileDeviceManager.submitVmList(mobileServerManager.getVmList(i));
		}
		
		//Creation of tasks are scheduled here!
		for(int i=0; i< loadGeneratorModel.getTaskList().size(); i++)
			schedule(getId(), loadGeneratorModel.getTaskList().get(i).getStartTime(), CREATE_TASK, loadGeneratorModel.getTaskList().get(i));
		
		//Periodic event loops starts from here!
		schedule(getId(), 5, CHECK_ALL_VM);
		schedule(getId(), SimSettings.getInstance().getSimulationTime()/100, PRINT_PROGRESS);
		schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
		schedule(getId(), SimSettings.getInstance().getSimulationTime(), STOP_SIMULATION);
		
		SimLogger.printLine("Done.");
	}

	@Override
	public void processEvent(SimEvent ev) {
		synchronized(this){
			switch (ev.getTag()) {
			case CREATE_TASK:
				try {
					TaskProperty edgeTask = (TaskProperty) ev.getData();
					mobileDeviceManager.submitTask(edgeTask);						
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				break;
			case CHECK_ALL_VM:
				int totalNumOfVm = SimSettings.getInstance().getNumOfEdgeVMs();
				if(EdgeVmAllocationPolicy_Custom.getCreatedVmNum() != totalNumOfVm){
					SimLogger.printLine("All VMs cannot be created! Terminating simulation...");
					System.exit(1);
				}
				break;
			case GET_LOAD_LOG:
				SimLogger.getInstance().addVmUtilizationLog(
						CloudSim.clock(),
						edgeServerManager.getAvgUtilization(),
						cloudServerManager.getAvgUtilization(),
						mobileServerManager.getAvgUtilization());

				for (Datacenter dc : edgeServerManager.getDatacenterList()) {
					for (Host host : dc.getHostList()) {
						double totalUtil = 0;
						double totalMips = 0;

						for (Vm vm : host.getVmList()) {
							double util = vm.getTotalUtilizationOfCpu(CloudSim.clock());
							totalUtil += util;  // in MIPS
							totalMips += vm.getMips();  // capacity
						}

						double percent = (totalMips > 0) ? (totalUtil / totalMips) * 100 : 0;
						SimLogger.logPerHostVmLoad(CloudSim.clock(), host.getId(), percent);
					}
				}


				schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
				break;
			case PRINT_PROGRESS:
				int progress = (int)((CloudSim.clock()*100)/SimSettings.getInstance().getSimulationTime());
				if(progress % 10 == 0)
					SimLogger.print(Integer.toString(progress));
				else
					SimLogger.print(".");
				if(CloudSim.clock() < SimSettings.getInstance().getSimulationTime())
					schedule(getId(), SimSettings.getInstance().getSimulationTime()/100, PRINT_PROGRESS);

				break;
			case STOP_SIMULATION:
				SimLogger.printLine("100");
				CloudSim.terminateSimulation();
				try {
					SimLogger.getInstance().simStopped();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				break;
			default:
				SimLogger.printLine(getName() + ": unknown event type");
				break;
			}
		}
	}

	@Override
	public void shutdownEntity() {
		edgeServerManager.terminateDatacenters();
		cloudServerManager.terminateDatacenters();
		mobileServerManager.terminateDatacenters();
	}
}
