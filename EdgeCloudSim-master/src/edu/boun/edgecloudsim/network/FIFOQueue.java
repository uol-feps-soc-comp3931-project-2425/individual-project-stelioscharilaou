package edu.boun.edgecloudsim.network;

import java.util.LinkedList;
import java.util.Queue;

public class FIFOQueue {
    private Queue<Double> taskQueue;
    private double serviceRate;
    private double lastServiceTime;

    public FIFOQueue(double _serviceRate) {
        this.serviceRate = _serviceRate;
        this.taskQueue = new LinkedList<>();
        this.lastServiceTime = 0.0;
    }

    public double getNextAvailableTime() {
        return lastServiceTime;
    }

    public double addTask(double currentTime) {
        double serviceTime = 1.0 / serviceRate;

        double startTime = Math.max(currentTime, lastServiceTime);
        double finishTime = startTime + serviceTime;

        taskQueue.add(finishTime);
        lastServiceTime = finishTime;

        return finishTime - currentTime;
    }

    public void removeCompletedTasks(double currentTime) {
        while (!taskQueue.isEmpty() && taskQueue.peek() <= currentTime) {
            taskQueue.poll();
        }
    }

    public int getQueueLength(double currentTime) {
        removeCompletedTasks(currentTime);
        return taskQueue.size();
    }

    public void reset() {
        taskQueue.clear();
        lastServiceTime = 0.0;
    }

    public double getServiceRate() {
        return serviceRate;
    }

    public void setServiceRate(double serviceRate) {
        this.serviceRate = serviceRate;
    }
}
