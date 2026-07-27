package com.os.cpuscheduling.cpuscheduling.model;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public class ReadyQueue {
    private final QueueConfig config;
    private final Queue<Process> queue;

    public ReadyQueue(QueueConfig config) {
        this.config = config;
        if (config.getAlgorithm() == QueueConfig.Algorithm.PRIORITY) {
            this.queue = new PriorityQueue<>(Comparator.comparingInt(Process::getPriority));
        } else if (config.getAlgorithm() == QueueConfig.Algorithm.FCFS ||
                config.getAlgorithm() == QueueConfig.Algorithm.ROUND_ROBIN) {
            this.queue = new ArrayDeque<>();
        } else {
            this.queue = new ArrayDeque<>();
        }
    }

    public QueueConfig getConfig() {
        return config;
    }

    public void add(Process process, int currentTime) {
        process.markEnqueuedAt(currentTime);
        queue.add(process);
    }

    public Process poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public void requeueForRoundRobin(Process process, int currentTime) {
        if (config.getAlgorithm() == QueueConfig.Algorithm.ROUND_ROBIN) {
            add(process, currentTime);
        } else {
            add(process, currentTime);
        }
    }
}





