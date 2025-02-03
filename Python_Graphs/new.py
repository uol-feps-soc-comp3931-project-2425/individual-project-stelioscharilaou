import matplotlib.pyplot as plt
import networkx as nx
import pandas as pd
from matplotlib.widgets import Button

# --- Configuration ---
NUM_DEVICES = 2  # Change to dynamically adjust number of devices
NUM_EDGE_NODES = 3  # Change to dynamically adjust number of edge nodes

# --- Network Topology Setup ---
G = nx.DiGraph()

# Define Nodes (Base Structure)
nodes = {
    "Internet": (0, 0),
    "R-W-Router": (-2, -2),
    "R-Router": (2, -2),
    "L-Router-1": (-3, 1),
    "L-Router-2": (0, 1),
    "L-Router-3": (3, 1),
}

# Define Edge Node Mapping based on the success log
EDGE_NODE_MAPPING = {1003: "Edge-Node-1", 1004: "Edge-Node-2", 1005: "Edge-Node-3"}

# Dynamically Add Edge Nodes
for i in range(1, NUM_EDGE_NODES + 1):
    nodes[f"Edge-Node-{i}"] = (-3 + (i - 1) * 3, 2)
    G.add_edge(f"L-Router-{i}", f"Edge-Node-{i}")

# Dynamically Add Devices
for i in range(1, NUM_DEVICES + 1):
    nodes[f"Device-{i}"] = (-2 + (i - 1) * 4, -3)
    G.add_edge(f"Device-{i}", "R-W-Router" if i == 1 else "R-Router")

# Add Static Edges
edges = [
    ("R-W-Router", "Internet"),
    ("R-Router", "Internet"),
    ("Internet", "L-Router-1"),
    ("Internet", "L-Router-2"),
    ("Internet", "L-Router-3"),
]
G.add_edges_from(edges)
G.add_nodes_from(nodes.keys())

# --- Read and Sort Simulation Log ---
file_path = r"C:\Users\Stelios\OneDrive - University of Leeds\Year 3\FInal Project\individual-project-stelioscharilaou\EdgeCloudSim-master\sim_results\ite1\SIMRESULT_TWO_TIER_WITH_EO_ROUND_ROBIN_2DEVICES_SUCCESS.log"
df = pd.read_csv(file_path, delimiter=";", header=None, skiprows=1, usecols=[0, 1, 2])
df.columns = ["Time", "DeviceID", "EdgeNode"]
df = df.sort_values(by=["Time"]).reset_index(
    drop=True
)  # Sort by Time before processing

# --- Visualization ---
pos = nodes
fig, ax = plt.subplots(figsize=(8, 6))
ax.axis("off")  # Hide x and y axis

step = 0  # Step counter

# Button for moving to the next packet
ax_next = plt.axes([0.8, 0.05, 0.1, 0.075])
btn_next = Button(ax_next, "Next Packet")


def update(event):
    """Update function for animation."""
    global step
    if step >= len(df):
        return

    time = df.iloc[step, 0]
    device_id = df.iloc[step, 1]
    edge_node_id = df.iloc[step, 2]
    node_name = EDGE_NODE_MAPPING.get(edge_node_id, None)
    device_name = "Device-1" if device_id == 0 else "Device-2"
    path = []

    if node_name and node_name in pos and device_name in pos:
        path = [device_name, "R-W-Router" if device_id == 0 else "R-Router", "Internet"]
        if node_name in ["Edge-Node-1", "Edge-Node-2", "Edge-Node-3"]:
            path.append(
                "L-Router-1"
                if node_name == "Edge-Node-1"
                else "L-Router-2" if node_name == "Edge-Node-2" else "L-Router-3"
            )
        path.append(node_name)

    step += 1

    # Highlight path
    node_colors = ["red" if node in path else "lightblue" for node in G.nodes()]
    edge_colors = [
        "red" if (u, v) in zip(path, path[1:]) else "gray" for u, v in G.edges()
    ]
    ax.clear()
    ax.axis("off")  # Hide x and y axis again after clearing
    nx.draw(
        G,
        pos,
        with_labels=True,
        node_size=2000,
        node_color=node_colors,
        edge_color=edge_colors,
        ax=ax,
    )

    plt.draw()


btn_next.on_clicked(update)
plt.show()
