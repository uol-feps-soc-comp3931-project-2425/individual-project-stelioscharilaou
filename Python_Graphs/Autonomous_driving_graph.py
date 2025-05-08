import matplotlib.pyplot as plt

# Data
device_counts = [20, 40, 60, 80]

# Metrics
metrics = [
    ('Service Time vs Number of Devices', [0.286432333, 0.285876333, 0.289018333, 0.301571333],
     [0.303767667, 0.290053, 0.301811333, 0.335217333],
     [0.28045, 0.282032333, 0.300027667, 0.396178], 'Service Time (s)'),
    ('Processing Time vs Number of Devices', [0.270479667, 0.267841667, 0.266364667, 0.273233333],
     [0.288462333, 0.270700333, 0.280106333, 0.303336],
     [0.270478667, 0.271227333, 0.287686667, 0.382891333], 'Processing Time (s)'),
    ('Network Delay vs Number of Devices', [0.015953, 0.018035, 0.022654, 0.028248333],
     [0.015305333, 0.019352667, 0.021705, 0.031881333],
     [0.009971333, 0.010805333, 0.012341, 0.013287], 'Network Delay (s)'),
    ('Server Utilization vs Number of Devices', [8.696349, 15.973383, 26.020763, 35.079432],
     [8.982720333, 17.92363467, 25.78525633, 40.744844],
     [8.062988, 18.59601467, 27.171823, 46.880574], 'Server Utilization (%)'),
    ('Failure Rate vs Number of Devices', [0.00868, 0.004087, 0.002625667, 0.002477667],
     [0.001404333, 0.001610333, 0.001747667, 0.056082667],
     [0.001847333, 0.000727667, 0.001047667, 0.004032], 'Failure Rate (%)'),
]

# Distinct, visually appealing colors from tab10
colors = plt.get_cmap('tab10').colors

# Create subplots grid (3 rows x 2 cols)
fig, axes = plt.subplots(3, 2, figsize=(14, 12))
axes = axes.flatten()

for ax, (title, rr, ll, hy, ylabel) in zip(axes[:5], metrics):
    ax.plot(device_counts, rr, marker='o', color=colors[0], label='RoundRobin')
    ax.plot(device_counts, ll, marker='s', color=colors[1], label='LeastLoaded')
    ax.plot(device_counts, hy, marker='^', color=colors[2], label='Hybrid')
    ax.set_xlabel('Number of Devices')
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.legend()
    ax.grid(True)

# Remove the empty subplot
fig.delaxes(axes[5])

plt.tight_layout()
plt.show()
