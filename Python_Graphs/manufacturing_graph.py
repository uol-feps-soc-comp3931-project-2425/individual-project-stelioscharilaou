import matplotlib.pyplot as plt

# Manufacturing scenario data
device_counts = [50, 100, 150, 200]

# Metrics for manufacturing scenario
metrics = [
    (
        "Service Time vs Number of Devices",
        [0.19997333, 0.192653667, 0.184034, 0.181499333],  # RoundRobin
        [0.288545, 0.195119333, 0.190945333, 0.183618667],  # LeastLoaded
        [0.200697667, 0.183557667, 0.184318, 0.177732],  # Hybrid
        "Service Time (s)",
    ),
    (
        "Processing Time vs Number of Devices",
        [0.191202667, 0.182616, 0.174195, 0.172501333],  # RoundRobin
        [0.280568, 0.189522, 0.181784333, 0.174084],  # LeastLoaded
        [0.193937, 0.174228, 0.177355, 0.172565667],  # Hybrid
        "Processing Time (s)",
    ),
    (
        "Network Delay vs Number of Devices",
        [0.008795, 0.010038, 0.009838667, 0.008983333],  # RoundRobin
        [0.007976, 0.005597333, 0.009161333, 0.009534667],  # LeastLoaded
        [0.006760667, 0.009329667, 0.006963, 0.005166333],  # Hybrid
        "Network Delay (s)",
    ),
    (
        "Server Utilization vs Number of Devices",
        [2.142326333, 4.023597, 5.812895, 7.915273333],  # RoundRobin
        [3.089003333, 4.369193667, 5.939242, 7.710888333],  # LeastLoaded
        [2.054069, 3.767187, 5.775734333, 7.50929],  # Hybrid
        "Server Utilization (%)",
    ),
    (
        "Failure Rate vs Number of Devices",
        [0.003153667, 0.005010333, 0.004199333, 0.000826667],  # RoundRobin
        [0.006494333, 0.003121667, 0.002292333, 0.001652333],  # LeastLoaded
        [0.0, 0.0, 0.001110667, 0.003322],  # Hybrid
        "Failure Rate (%)",
    ),
]

# Distinct, visually appealing colors from tab10
colors = plt.get_cmap("tab10").colors

# Create a 3x2 subplot grid
fig, axes = plt.subplots(3, 2, figsize=(14, 12))
axes = axes.flatten()

# Plot each metric
for ax, (title, rr, ll, hy, ylabel) in zip(axes[:5], metrics):
    ax.plot(device_counts, rr, marker="o", color=colors[0], label="RoundRobin")
    ax.plot(device_counts, ll, marker="s", color=colors[1], label="LeastLoaded")
    ax.plot(device_counts, hy, marker="^", color=colors[2], label="Hybrid")
    ax.set_xlabel("Number of Devices")
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.legend()
    ax.grid(True)

# Remove the empty subplot
fig.delaxes(axes[5])

plt.tight_layout()
plt.show()
