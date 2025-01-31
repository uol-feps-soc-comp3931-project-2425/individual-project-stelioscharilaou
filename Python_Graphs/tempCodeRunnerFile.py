import matplotlib.pyplot as plt
import networkx as nx
from matplotlib.widgets import Button

# Define network topology
G = nx.DiGraph()

# Nodes
devices = ["Device 1", "Device 2"]
routers = ["R-W-Router", "R-Router"]
l_routers = ["L-Router-1", "L-Router-2", "L-Router-3"]
edge_nodes = ["Edge-Node-1", "Edge-Node-2", "Edge-Node-3"]

# Add nodes
G.add_nodes_from(devices, color="blue")
G.add_nodes_from(routers, color="gray")
G.add_nodes_from(l_routers, color="gray")
G.add_nodes_from(edge_nodes, color="green")

# Edges (Network Links)
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
G.add_edges_from(edges)

# Define positions
pos = {
    "Device 1": (0, 0),
    "Device 2": (4, 0),
    "R-W-Router": (0, 1),
    "R-Router": (4, 1),
    "Internet": (2, 2),
    "L-Router-1": (0, 3),
    "L-Router-2": (2, 3),
    "L-Router-3": (4, 3),
    "Edge-Node-1": (0, 4),
    "Edge-Node-2": (2, 4),
    "Edge-Node-3": (4, 4),
}

# Get default node colors
node_colors = [G.nodes[n].get("color", "blue") for n in G.nodes]

# Create figure
fig, ax = plt.subplots(figsize=(7, 6))
plt.subplots_adjust(bottom=0.2)

# Initialize step counter
step = 0


# Function to update the graph dynamically
def update_graph():
    ax.clear()
    plt.title(f"Packet Transmission Step {step + 1}")

    # Reset colors
    edge_colors = ["gray" for _ in G.edges]

    # Define active paths
    active_paths = [
        ("Device 1", "R-W-Router"),
        ("R-W-Router", "Internet"),
        ("Internet", "L-Router-1"),
        ("L-Router-1", "Edge-Node-1"),
        ("Device 2", "R-Router"),
        ("R-Router", "Internet"),
        ("Internet", "L-Router-3"),
        ("L-Router-3", "Edge-Node-3"),
    ]

    # Highlight active path in red based on step
    if step % 2 == 0:
        for edge in active_paths[:4]:  # First device path
            if edge in G.edges:
                edge_colors[list(G.edges).index(edge)] = "red"
    else:
        for edge in active_paths[4:]:  # Second device path
            if edge in G.edges:
                edge_colors[list(G.edges).index(edge)] = "red"

    # Draw the graph
    nx.draw(
        G,
        pos,
        with_labels=True,
        node_size=3000,
        node_color=node_colors,
        font_size=10,
        edge_color=edge_colors,
        width=2,
        ax=ax,
    )
    plt.draw()


# Function to go to the next step
def next_step(event):
    global step
    step += 1
    update_graph()


# Function to go to the previous step
def prev_step(event):
    global step
    if step > 0:
        step -= 1
    update_graph()


# Create Next & Previous buttons
ax_prev = plt.axes([0.1, 0.05, 0.2, 0.075])
ax_next = plt.axes([0.7, 0.05, 0.2, 0.075])
btn_prev = Button(ax_prev, "Previous")
btn_next = Button(ax_next, "Next")

# Assign button click events
btn_prev.on_clicked(prev_step)
btn_next.on_clicked(next_step)

# Initialize the first graph
update_graph()

plt.show()
