import pandas as pd
import matplotlib.pyplot as plt
import os

# Define the directory containing log files
log_dir = r"C:\Users\Stelios\OneDrive - University of Leeds\Year 3\Final Project\EdgeCloudSim-master\sim_results\ite1"

# Define log file paths
log_files = {
    "Augmented Reality": os.path.join(
        log_dir,
        "SIMRESULT_TWO_TIER_WITH_EO_ROUND_ROBIN_3DEVICES_AUGMENTED_REALITY_GENERIC.log",
    ),
    "Health App": os.path.join(
        log_dir,
        "SIMRESULT_TWO_TIER_WITH_EO_ROUND_ROBIN_3DEVICES_HEALTH_APP_GENERIC.log",
    ),
    "Heavy Compute": os.path.join(
        log_dir,
        "SIMRESULT_TWO_TIER_WITH_EO_ROUND_ROBIN_3DEVICES_HEAVY_COMP_APP_GENERIC.log",
    ),
    "Infotainment": os.path.join(
        log_dir,
        "SIMRESULT_TWO_TIER_WITH_EO_ROUND_ROBIN_3DEVICES_INFOTAINMENT_APP_GENERIC.log",
    ),
    "Location": os.path.join(
        log_dir, "SIMRESULT_TWO_TIER_WITH_EO_ROUND_ROBIN_3DEVICES_LOCATION.log"
    ),
    "VM Load": os.path.join(
        log_dir, "SIMRESULT_TWO_TIER_WITH_EO_ROUND_ROBIN_3DEVICES_VM_LOAD.log"
    ),
}

# Dictionary to store extracted metrics
data = {}

# Process each log file
for category, file_path in log_files.items():
    try:
        # Read log file
        df = pd.read_csv(file_path, sep=";", header=None, skiprows=1)

        # Ensure the log file has enough rows before accessing index 4
        if len(df) < 5:
            print(f"Skipping {category} due to insufficient data.")
            continue

        # Extract relevant data safely
        total_tasks = df.iloc[0, 0] if len(df) > 0 else 0
        failed_tasks = df.iloc[0, 1] if len(df) > 0 else 0
        completed_tasks = total_tasks - failed_tasks
        avg_service_time = df.iloc[0, 4] if len(df) > 4 else 0
        avg_processing_time = df.iloc[0, 5] if len(df) > 5 else 0
        avg_network_delay = df.iloc[0, 6] if len(df) > 6 else 0

        # Store data
        data[category] = {
            "Total Tasks": total_tasks,
            "Failed Tasks": failed_tasks,
            "Completed Tasks": completed_tasks,
            "Avg Service Time": avg_service_time,
            "Avg Processing Time": avg_processing_time,
            "Avg Network Delay": avg_network_delay,
        }

    except Exception as e:
        print(f"Error processing {category}: {e}")

# Convert to DataFrame
df_results = pd.DataFrame(data).T

# Print structured log data
print("\nSimulation Metrics Overview:")
print(df_results.head())

# --- Plot 1: Task Completion for Each Application ---
plt.figure(figsize=(8, 5))
df_results[["Completed Tasks", "Failed Tasks"]].plot(
    kind="bar", stacked=True, color=["green", "red"], alpha=0.75
)
plt.xlabel("Application Type")
plt.ylabel("Number of Tasks")
plt.title("Task Completion vs Failure for Different Applications")
plt.xticks(rotation=45)
plt.legend(["Completed", "Failed"])
plt.show()

# --- Plot 2: Average Processing & Network Delays ---
plt.figure(figsize=(8, 5))
df_results[["Avg Service Time", "Avg Processing Time", "Avg Network Delay"]].plot(
    kind="bar", alpha=0.75
)
plt.xlabel("Application Type")
plt.ylabel("Time (Seconds)")
plt.title("Processing & Network Delays for Different Applications")
plt.xticks(rotation=45)
plt.legend(["Service Time", "Processing Time", "Network Delay"])
plt.show()
