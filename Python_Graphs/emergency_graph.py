import matplotlib.pyplot as plt

# Emergency scenario data
device_counts = [20, 40, 60, 80]

# Metrics for emergency scenario
metrics = [
    (
        "Service Time vs Number of Devices",
        [0.765740333, 0.975011667, 2.09644, 5.339824],
        [0.672796667, 0.702802333, 0.780233, 0.699194],
        [0.768448333, 1.31161, 2.726872667, 43.08491467],
        "Service Time (s)",
    ),
    (
        "Processing Time vs Number of Devices",
        [0.757468333, 0.965622333, 2.085506, 5.322228333],
        [0.663874333, 0.694002, 0.769654667, 0.679913],
        [0.760193, 1.302372333, 2.715505667, 43.07180767],
        "Processing Time (s)",
    ),
    (
        "Network Delay vs Number of Devices",
        [0.008272333, 0.009389667, 0.010933667, 0.017596],
        [0.008922333, 0.008800667, 0.010578333, 0.019280667],
        [0.008255333, 0.009237667, 0.011367, 0.013106667],
        "Network Delay (s)",
    ),
    (
        "Server Utilization vs Number of Devices",
        [18.17502767, 44.03400233, 146.819398, 489.7265887],
        [15.987737, 31.92029, 54.04710167, 62.41722433],
        [17.66694533, 61.155797, 197.5960143, 5146.971014],
        "Server Utilization (%)",
    ),
    (
        "Failure Rate vs Number of Devices",
        [0.005641667, 0.011425667, 0.012344333, 0.037255667],
        [0.009671, 0.002734333, 0.998806, 5.761911667],
        [0.007689333, 0.008681333, 0.028461, 0.544368667],
        "Failure Rate (%)",
    ),
]

# Distinct, visually appealing colors
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
