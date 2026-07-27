package com.os.cpuscheduling.cpuscheduling.model;

public class Process {
    private final String id;
    private final int arrivalTime;
    private int burstTimeRemaining;
    private final int totalBurstTime;
    private final int priority; // lower value = higher priority

    private Integer startTime; // first time it gets CPU
    private Integer completionTime;
    private int waitingTimeAccumulated;
    private int lastReadyEnqueueTime;

    public Process(String id, int arrivalTime, int burstTime, int priority) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTimeRemaining = burstTime;
        this.totalBurstTime = burstTime;
        this.priority = priority;
        this.lastReadyEnqueueTime = arrivalTime;
    }

    public String getId() {
        return id;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getBurstTimeRemaining() {
        return burstTimeRemaining;
    }

    public int getTotalBurstTime() {
        return totalBurstTime;
    }

    public int getPriority() {
        return priority;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public Integer getCompletionTime() {
        return completionTime;
    }

    public void onScheduledAt(int currentTime) {
        if (startTime == null) {
            startTime = currentTime;
        }
        // accumulate waiting time since last enqueued
        waitingTimeAccumulated += Math.max(0, currentTime - lastReadyEnqueueTime);
    }

    public int executeFor(int timeSlice) {
        int executed = Math.min(timeSlice, burstTimeRemaining);
        burstTimeRemaining -= executed;
        return executed;
    }

    public boolean isCompleted() {
        return burstTimeRemaining <= 0;
    }

    public void markCompletedAt(int currentTime) {
        completionTime = currentTime;
    }

    public void markEnqueuedAt(int currentTime) {
        lastReadyEnqueueTime = currentTime;
    }

    public int getWaitingTime() {
        return waitingTimeAccumulated;
    }

    public int getTurnaroundTime() {
        if (completionTime == null) return -1;
        return completionTime - arrivalTime;
    }

    public int getResponseTime() {
        if (startTime == null) return -1;
        return startTime - arrivalTime;
    }
}





