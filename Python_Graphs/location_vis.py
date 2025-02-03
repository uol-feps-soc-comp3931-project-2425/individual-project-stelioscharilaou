import matplotlib.pyplot as plt
import networkx as nx
import pandas as pd
from matplotlib.widgets import Button

# --- Network Topology Setup ---
G = nx.DiGraph()

# Define Nodes
nodes = {
    "Device 1": (-2, -3),
    "Device 2": (2, -3),
    "R-W-Router": (-2, -2),
    "R-Router": (2, -2),
    "Internet": (0, 0),
    "L-Router-1": (-3, 1),
    "L-Router-2": (0, 1),
    "L-Router-3": (3, 1),
    "Edge-Node-1": (-3, 2),
    "Edge-Node-2": (0, 2),
    "Edge-Node-3": (3, 2),
}

# Define Edges
edges = [
    ("Device 1", "R-W-Router"),
    ("Device 2", "R-Router"),
    ("R-W-Router", "Internet"),
    ("R-Router", "Internet"),
    ("Internet", "L-Router-1"),
    ("Internet", "L-Router-2"),
    ("Internet", "L-Router-3"),
    ("L-Router-1", "Edge-Node-1"),
    ("L-Router-2", "Edge-Node-2"),
    ("L-Router-3", "Edge-Node-3"),
]

G.add_nodes_from(nodes.keys())
G.add_edges_from(edges)

# --- Read Simulation Log ---
file_path = r"C:\Users\Stelios\OneDrive - University of Leeds\Year 3\FInal Project\individual-project-stelioscharilaou\EdgeCloudSim-master\sim_results\ite1\SIMRESULT_TWO_TIER_WITH_EO_ROUND_ROBIN_2DEVICES_LOCATION.log"
df = pd.read_csv(file_path, delimiter=";", header=None, usecols=[0, 1, 2])
df.columns = ["Time", "Device1_Location", "Device2_Location"]

# --- Visualization ---
pos = nodes  # Use predefined positions
fig, ax = plt.subplots(figsize=(8, 6))
plt.subplots_adjust(bottom=0.2)

# Draw the network
nx.draw(
    G,
    pos,
    with_labels=True,
    node_size=2000,
    node_color="lightblue",
    edge_color="gray",
    ax=ax,
)

# Packet visualization (moving dots)
(packet_device1,) = ax.plot(
    [], [], "bo", markersize=10, label="Packet Device 1"
)  # Blue
(packet_device2,) = ax.plot([], [], "ro", markersize=10, label="Packet Device 2")  # Red

# Step Tracker
step = 0


# --- Functions for Animation ---
def update_plot():
    """Update packet positions based on step index."""
    global step
    if step < 0:
        step = 0
    if step >= len(df):
        step = len(df) - 1

    time = df.iloc[step, 0]
    device1_loc = df.iloc[step, 1]
    device2_loc = df.iloc[step, 2]

    packet_device1.set_data(pos[f"Edge-Node-{device1_loc}"])
    packet_device2.set_data(pos[f"Edge-Node-{device2_loc}"])

    ax.set_title(f"Packet Transmission at Time: {time} sec")
    plt.draw()


def next_step(event):
    """Move to the next step."""
    global step
    step += 1
    update_plot()


def prev_step(event):
    """Move to the previous step."""
    global step
    step -= 1
    update_plot()


# --- Buttons ---
ax_next = plt.axes([0.8, 0.05, 0.1, 0.075])
ax_prev = plt.axes([0.65, 0.05, 0.1, 0.075])
btn_next = Button(ax_next, "Next Step")
btn_prev = Button(ax_prev, "Prev Step")

btn_next.on_clicked(next_step)
btn_prev.on_clicked(prev_step)

# Initialize
update_plot()
plt.legend()
plt.show()
