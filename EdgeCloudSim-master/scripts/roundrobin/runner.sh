#!/bin/bash

CONFIGS=("AutonomousConfig" "EmergencyConfig" "ManufacturingConfig")
ITERATIONS=3
BASE_OUTPUT_DIR="../../simulation_results"
BIN_DIR="./bin"

mkdir -p "$BASE_OUTPUT_DIR"
cd "$(dirname "$0")"

# Compile once before looping
rm -rf "$BIN_DIR"
mkdir -p "$BIN_DIR"
find ../../src -name "*.java" -not -path "*/tests/*" > sources.txt
javac -d "$BIN_DIR" -cp "../../lib/*" @sources.txt
rm sources.txt

# Check MainApp exists
if [ ! -f "$BIN_DIR/edu/boun/edgecloudsim/applications/RR2/MainApp.class" ]; then
  echo "Compilation failed! MainApp.class not found!"
  exit 1
fi

for config in "${CONFIGS[@]}"; do
  for ((i=1; i<=ITERATIONS; i++)); do
    echo "Running $config - Iteration $i"

    RUN_DIR="$BASE_OUTPUT_DIR/${config}_Run${i}"
    mkdir -p "$RUN_DIR/logs" "$RUN_DIR/results" active_config

    cp "$config"/applications.xml active_config/
    cp "$config"/default_config.properties active_config/
    cp "$config"/edge_devices.xml active_config/

    java -cp "./bin;../../lib/*" edu.boun.edgecloudsim.applications.RR2.MainApp > "$RUN_DIR/output.txt" 2>&1

    [ -d "../../log" ] && mv ../../log/* "$RUN_DIR/logs/" && rm -r ../../log
    [ -d "../../deep_log" ] && mv ../../deep_log/* "$RUN_DIR/logs/" && rm -r ../../deep_log
    [ -d "../../output_results" ] && mv ../../output_results/* "$RUN_DIR/results/" && rm -r ../../output_results

    echo "Finished $config - Iteration $i"
  done
done
