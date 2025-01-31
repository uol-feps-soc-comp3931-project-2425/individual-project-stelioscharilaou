import pandas as pd
import matplotlib.pyplot as plt

# Define the log file path (Update this path if needed)
log_file_path = r"C:\Users\Stelios\OneDrive - University of Leeds\Year 3\Final Project\EdgeCloudSim-master\sim_results\ite1\SIMRESULT_TWO_TIER_WITH_EO_ROUND_ROBIN_3DEVICES_ALL_APPS_GENERIC.log"

# Load the log file into a DataFrame (Skipping first auto-generated line)
df = pd.read_csv(log_file_path, sep=";", header=None, skiprows=1)

# Extract key values from the log
total_tasks = df.iloc[0, 0]  # Total tasks executed
failed_tasks = df.iloc[0, 1]  # Number of failed tasks
completed_tasks = total_tasks - failed_tasks  # Successfully completed tasks

avg_service_time = df.iloc[0, 4]  # Average service time
avg_processing_time = df.iloc[0, 5]  # Average processing time
avg_network_delay = df.iloc[0, 6]  # Average network delay

# Network delay breakdown (LAN, MAN, WAN)
network_delays = df.iloc[4, :3].values
network_labels = ["LAN", "MAN", "WAN"]

# --- Plot 1: Task Completion vs Failed Tasks ---
plt.figure(figsize=(6, 4))
plt.bar(
    ["Completed", "Failed"], [completed_tasks, failed_tasks], color=["green", "red"]
)
plt.ylabel("Number of Tasks")
plt.title("Task Completion vs Failure")
plt.show()

# --- Plot 2: Average Processing & Network Delays ---
plt.figure(figsize=(6, 4))
plt.bar(
    ["Service Time", "Processing Time", "Network Delay"],
    [avg_service_time, avg_processing_time, avg_network_delay],
    color=["blue", "purple", "orange"],
)
plt.xlabel("Metrics")
plt.ylabel("Time (Seconds)")
plt.title("Task Processing and Network Delays")
plt.show()

# --- Plot 3: Network Delay Breakdown ---
plt.figure(figsize=(6, 4))
plt.bar(network_labels, network_delays, color=["purple", "orange", "cyan"])
plt.xlabel("Network Type")
plt.ylabel("Delay (Seconds)")
plt.title("Network Delay Breakdown")
plt.show()
