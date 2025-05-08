package edu.boun.edgecloudsim.task_generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.TaskProperty;

/**
 * Generates tasks for three scenarios, including some type=2 (“large”) jobs:
 *  - MANUFACTURING: 128–512 KB every 5–10 s (types 0 or 2 if ≥400 KB)
 *  - AUTONOMOUS:   256 KB–1 MB every 0.1–0.2 s (types 1 or 2 if ≥800 KB)
 *  - EMERGENCY:    512 KB–2 MB every 0.1–0.2 s (20% chance type=2)
 */
public class MyScenarioLoadGeneratorModel extends LoadGeneratorModel {
    private static final long MI_PER_KB = 1000; // MI per KB
    private final Random rand = new Random();

    public MyScenarioLoadGeneratorModel(int numberOfMobileDevices,
                                        double simulationTime,
                                        String simScenario) {
        super(numberOfMobileDevices, simulationTime, simScenario);
    }

    @Override
    public void initializeModel() {
        taskList = new ArrayList<>();
        String sc = simScenario.toUpperCase();

        for (int deviceId = 0; deviceId < numberOfMobileDevices; deviceId++) {
            double time = 0.0;
            while (time < simulationTime) {
                double interval;
                long sizeKb;
                int type;

                switch (sc) {
                    case "MANUFACTURING":
                        interval = 5.0 + rand.nextDouble() * 5.0;      // 5–10 s
                        sizeKb   = 128 + rand.nextInt(385);           // 128–512 KB
                        // any very large dump becomes type 2
                        type     = (sizeKb >= 400) ? 2 : 0;
                        break;

                    case "AUTONOMOUS":
                        interval = 0.1 + rand.nextDouble() * 0.1;     // 0.1–0.2 s
                        sizeKb   = 256 + rand.nextInt(769);           // 256 KB–1 MB
                        // occasional big map update
                        type     = (sizeKb >= 800) ? 2 : 1;
                        break;

                    case "EMERGENCY":
                        interval = 0.1 + rand.nextDouble() * 0.1;     // 0.1–0.2 s
                        sizeKb   = 512 + rand.nextInt(1537);          // 512 KB–2 MB
                        // 20% of bursts are large analysis jobs
                        type     = (rand.nextDouble() < 0.2) ? 2 : 1;
                        break;

                    default:
                        interval = 1.0;
                        sizeKb   = 100;
                        type     = 0;
                }

                long length = sizeKb * MI_PER_KB;
                int pes     = 1;

                TaskProperty tp = new TaskProperty(
                        time,        // startTime
                        deviceId,    // mobileDeviceId
                        type,        // taskType
                        pes,         // pesNumber
                        length,      // length (MI)
                        sizeKb,      // inputFileSize (KB)
                        sizeKb       // outputFileSize (KB)
                );
                taskList.add(tp);
                time += interval;
            }
        }
    }

    @Override
    public int getTaskTypeOfDevice(int deviceId) {
        // fallback: return first matching
        return taskList.stream()
                .filter(tp -> tp.getMobileDeviceId() == deviceId)
                .findFirst()
                .map(TaskProperty::getTaskType)
                .orElse(0);
    }
}
