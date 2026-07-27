package com.os.cpuscheduling.cpuscheduling.model;

public class QueueConfig {
    public enum Algorithm {
        FCFS,
        ROUND_ROBIN,
        PRIORITY
    }

    private final String name;
    private final int priorityLevel; // lower value = higher queue priority
    private final Algorithm algorithm;
    private final int timeQuantum; // used when ROUND_ROBIN

    public QueueConfig(String name, int priorityLevel, Algorithm algorithm, int timeQuantum) {
        this.name = name;
        this.priorityLevel = priorityLevel;
        this.algorithm = algorithm;
        this.timeQuantum = timeQuantum;
    }

    public String getName() {
        return name;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public int getTimeQuantum() {
        return timeQuantum;
    }
}





