import os
import re
import numpy as np
import matplotlib.pyplot as plt
import networkx as nx
import pandas as pd
from matplotlib.widgets import Button
from matplotlib.offsetbox import OffsetImage, AnnotationBbox

# Attempt to use a nice style; if unavailable, fall back to ggplot.
try:
    plt.style.use("seaborn-whitegrid")
except OSError:
    plt.style.use("ggplot")

# --- File Path & Device Count Determination ---
file_path = r"C:\Users\Stelios\OneDrive - University of Leeds\Year 3\FInal Project\individual-project-stelioscharilaou\EdgeCloudSim-master\sim_results\ite1\SIMRESULT_SINGLE_TIER_ROUND_ROBIN_10DEVICES_SUCCESS.log"
# Extract the number of devices from the file name (e.g. "5DEVICES" will yield 5).
match = re.search(r"(\d+)devices", file_path, re.IGNORECASE)
if match:
    NUM_DEVICES = int(match.group(1))
else:
    NUM_DEVICES = 0  # Or set a default value

# --- Read and Sort Simulation Log ---
df = pd.read_csv(file_path, delimiter=";", header=None, skiprows=1, usecols=[0, 1, 2])
df.columns = ["Time", "DeviceID", "EdgeNode"]
# Force the DeviceID column to be integer type.
df["DeviceID"] = df["DeviceID"].astype(int)
df = df.sort_values(by=["Time"]).reset_index(drop=True)

# --- Device Setup Using File Name ---
# We ignore the simulation log’s device IDs and instead create devices 0..NUM_DEVICES-1.
unique_device_ids = list(range(NUM_DEVICES))
num_devices = len(unique_device_ids)
# Create a mapping: device 0 becomes Device-1, device 1 becomes Device-2, etc.
device_id_map = {dev: dev + 1 for dev in unique_device_ids}

# --- Dynamic Detection of Edge Nodes ---
# Instead of mapping the edge codes to sequential indexes, we now use the actual code.
unique_edge_codes = sorted(df["EdgeNode"].unique())
num_edge_nodes = len(unique_edge_codes)
edge_node_map = {code: f"Edge-Node-{code}" for code in unique_edge_codes}
# Create a corresponding mapping for local routers.
local_router_map = {code: f"L-Router-{code}" for code in unique_edge_codes}

# --- Network Topology Setup ---
G = nx.DiGraph()
nodes = {}

# Static Internet node at the top.
nodes["Internet"] = (0, 0)

# --- Device-Specific Routers and Devices ---
# Place devices evenly along the x-axis at y = -3.
x_positions_devices = np.linspace(-3, 3, num=num_devices)
device_names = [f"Device-{device_id_map[dev]}" for dev in unique_device_ids]
for name, x in zip(device_names, x_positions_devices):
    nodes[name] = (x, -3)

# For each device, create a unique router named R{n}-Router directly above the device (y = -2).
device_router_names = [f"R{device_id_map[dev]}-Router" for dev in unique_device_ids]
for name, x in zip(device_router_names, x_positions_devices):
    nodes[name] = (x, -2)

# --- Local Routers and Edge Nodes (for the edge simulation) ---
# Create local routers for each edge code using the actual identifier.
local_router_names = [local_router_map[code] for code in sorted(unique_edge_codes)]
x_positions_local = np.linspace(-3, 3, num=num_edge_nodes)
for name, x in zip(local_router_names, x_positions_local):
    nodes[name] = (x, 1)

# Create edge nodes based on the simulation log using the actual code.
edge_node_names = [edge_node_map[code] for code in sorted(unique_edge_codes)]
x_positions_edge = np.linspace(-3, 3, num=num_edge_nodes)
for name, x in zip(edge_node_names, x_positions_edge):
    nodes[name] = (x, 2)

# Add all nodes to the graph.
G.add_nodes_from(nodes.keys())

# --- Adding Edges ---
# Connect each device to its unique router.
for device, router in zip(device_names, device_router_names):
    G.add_edge(device, router)
    # Connect each device-specific router to the Internet.
    G.add_edge(router, "Internet")

# Connect the Internet to each local router.
for local_router in local_router_names:
    G.add_edge("Internet", local_router)

# Connect each local router to its corresponding edge node.
for code in unique_edge_codes:
    local_router = local_router_map[code]
    edge_node = edge_node_map[code]
    G.add_edge(local_router, edge_node)

# --- Load Images for Nodes ---
image_dir = "images"
node_image_paths = {
    "internet": os.path.join(image_dir, "internet.png"),
    "router": os.path.join(image_dir, "cisco_router.png"),
    "edge": os.path.join(image_dir, "cisco_edge.png"),
    "device": os.path.join(image_dir, "cisco_device.png"),
}
node_images = {}
for key, path in node_image_paths.items():
    if os.path.exists(path):
        node_images[key] = plt.imread(path)
    else:
        print(f"Warning: {path} not found. {key} nodes will not display an image.")
        node_images[key] = None


def get_node_category(node):
    """Return the category for a node so that the correct image is used."""
    if node == "Internet":
        return "internet"
    elif "Device" in node:
        return "device"
    elif "Edge-Node" in node:
        return "edge"
    elif "Router" in node:
        return "router"
    else:
        return None


# --- Visualisation Setup ---
pos = nodes
fig, ax = plt.subplots(figsize=(10, 8))
ax.axis("off")
fig.suptitle("Dynamic Network Simulation Visualisation", fontsize=16)

step = 0  # Global simulation step counter


def draw_network(step_index):
    """Draw the network graph and highlight the simulation path using images and coloured edges."""
    ax.clear()
    ax.axis("off")
    ax.set_title(f"Step: {step_index} / {len(df)}", fontsize=14)

    # Determine the simulation path for the current step.
    path = []
    if 0 <= step_index < len(df):
        row = df.iloc[step_index]
        sim_time = row["Time"]
        device_val = int(row["DeviceID"])  # Ensure device value is an integer.
        edge_code = row["EdgeNode"]

        # Map the device value to node names.
        if device_val not in device_id_map:
            print(f"Device id {device_val} not in expected range from file name.")
        else:
            device_name = f"Device-{device_id_map[device_val]}"
            router_name = f"R{device_id_map[device_val]}-Router"
            # Use the edge code directly from the log.
            if edge_code in edge_node_map:
                # Build the simulation path: device -> device-specific router -> Internet -> local router -> edge node.
                path = [device_name, router_name, "Internet"]
                local_router = local_router_map[edge_code]
                path.extend([local_router, edge_node_map[edge_code]])

    # Draw all edges in grey.
    nx.draw_networkx_edges(
        G, pos, ax=ax, edge_color="gray", arrows=True, arrowstyle="-|>", width=1.0
    )

    # If a valid simulation path exists, highlight the edges in that path.
    if len(path) > 1:
        highlighted_edges = list(zip(path, path[1:]))
        nx.draw_networkx_edges(
            G,
            pos,
            edgelist=highlighted_edges,
            ax=ax,
            edge_color="tomato",
            arrows=True,
            arrowstyle="-|>",
            width=3.0,
        )

    # For each node, place its corresponding image.
    for node in G.nodes():
        category = get_node_category(node)
        image = node_images.get(category)
        if image is None:
            continue
        im = OffsetImage(image, zoom=0.15)  # Adjust zoom as needed.
        # Highlight nodes that are in the simulation path.
        if node in path:
            ab = AnnotationBbox(
                im,
                pos[node],
                frameon=True,
                bboxprops=dict(edgecolor="tomato", linewidth=2),
                xycoords="data",
            )
        else:
            ab = AnnotationBbox(im, pos[node], frameon=False, xycoords="data")
        ax.add_artist(ab)
        # Optionally add a text label below the image.
        ax.text(pos[node][0], pos[node][1] - 0.25, node, ha="center", fontsize=8)

    # Display simulation time in the bottom left corner (if available).
    if 0 <= step_index < len(df):
        ax.text(
            0.02,
            0.02,
            f"Simulation Time: {row['Time']}",
            transform=ax.transAxes,
            fontsize=12,
            color="navy",
        )

    plt.draw()


def next_packet(event):
    global step
    if step < len(df):
        draw_network(step)
        step += 1


def previous_packet(event):
    global step
    if step > 0:
        step -= 1
        draw_network(step)


# Initialise with the first simulation step.
draw_network(step)

# Create navigation buttons.
ax_next = plt.axes([0.81, 0.05, 0.1, 0.075])
btn_next = Button(ax_next, "Next Packet")
btn_next.on_clicked(next_packet)

ax_back = plt.axes([0.70, 0.05, 0.1, 0.075])
btn_back = Button(ax_back, "Back Packet")
btn_back.on_clicked(previous_packet)

plt.show()
