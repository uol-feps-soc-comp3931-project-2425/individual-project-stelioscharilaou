# Load Distribution in Edge Computing Simulator

This repository contains the code and data behind my BSc dissertation on developing an optimized load‑balancing algorithm for edge computing.  
The work is built on **EdgeCloudSim**, extended with new scheduling policies, mobility and network models, plus a small analysis toolkit for turning raw logs into publication‑ready figures.

## Repository layout

| Path | Description |
| :--- | :--- |
| **EdgeCloudSim‑master/** | The complete, final state of the simulator including custom Java classes for the Round‑Robin, Least‑Loaded and Hybrid orchestrators, the scenario factory, and modified network and mobility modules. |
| **Python_Graphs/** | Lightweight Python scripts used to generate the graphs used in the dissertation report. |
| **Dissertation_results.xlsx** | A single spreadsheet that collects the summarised results from all simulation runs and find the mean values for all the iterations |
| **EdgeCloudSim‑master/scripts/roundrobin/** | In this folder the different configuration files are located and the runner shell file. |
| **EdgeCloudSim‑master/simulation_results/** | Output directory containing the results of every scenario and all their iterations. |
| **EdgeCloudSim‑master/src/edu/boun/edgecloudsim/applications/RR2** | Contains all the orchestrator and extended modules needed to run the simulator |
| **EdgeCloudSim‑master/src/edu/boun/edgecloudsim/applications/RR2/tests/** | JUnit test suite validating the new scheduling components and helper classes. |

# Make the runner script executable (first time only)
chmod +x EdgeCloudSim-master/scripts/roundrobin/runner.sh

# Run the final simulation used in the dissertation
./EdgeCloudSim-master/scripts/roundrobin/runner.sh
