package com.os.cpuscheduling.cpuscheduling.sim;

import com.os.cpuscheduling.cpuscheduling.model.Process;
import com.os.cpuscheduling.cpuscheduling.model.QueueConfig;
import com.os.cpuscheduling.cpuscheduling.model.ReadyQueue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MultiLevelScheduler {
    private final List<ReadyQueue> queues = new ArrayList<>();
    private final List<Process> allProcesses = new ArrayList<>();
    private int currentTime = 0;
    private int cpuBusyTime = 0;
    private final boolean preemptive;

    public MultiLevelScheduler(boolean preemptive) {
        this.preemptive = preemptive;
    }

    public void addQueue(QueueConfig config) {
        queues.add(new ReadyQueue(config));
        queues.sort(Comparator.comparingInt(q -> q.getConfig().getPriorityLevel()));
    }

    public void addProcess(Process process) {
        allProcesses.add(process);
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public boolean isAllCompleted() {
        for (Process p : allProcesses) {
            if (!p.isCompleted()) return false;
        }
        return true;
    }

    public boolean hasReadyNow() {
        // ready if any queue has items, or any process arrives at currentTime
        for (ReadyQueue q : queues) {
            if (!q.isEmpty()) return true;
        }
        for (Process p : allProcesses) {
            if (!p.isCompleted() && p.getArrivalTime() == currentTime) return true;
        }
        return false;
    }

    private void enqueueArrivalsAt(int time) {
        for (Process p : allProcesses) {
            if (p.getArrivalTime() == time && !p.isCompleted()) {
                // by default, put by priority field into queue 0 or appropriate based on policy
                // Here we map based on priorityLevel index if available, else first queue
                ReadyQueue target = queues.get(Math.min(p.getPriority(), queues.size() - 1));
                target.add(p, time);
            }
        }
    }

    private void enqueueArrivalsBetween(int fromExclusive, int toInclusive) {
        if (toInclusive <= fromExclusive) return;
        for (int t = fromExclusive + 1; t <= toInclusive; t++) {
            enqueueArrivalsAt(t);
        }
    }

    private ReadyQueue getHighestNonEmptyQueue() {
        for (ReadyQueue q : queues) {
            if (!q.isEmpty()) return q;
        }
        return null;
    }

    public StepResult step() {
        enqueueArrivalsAt(currentTime);

        ReadyQueue queue = getHighestNonEmptyQueue();
        if (queue == null) {
            currentTime += 1; // idle tick
            return new StepResult(null, 0, currentTime, false, true);
        }

        Process running = queue.poll();
        running.onScheduledAt(currentTime);
        int quantum = queue.getConfig().getAlgorithm() == QueueConfig.Algorithm.ROUND_ROBIN
                ? Math.max(1, queue.getConfig().getTimeQuantum())
                : Integer.MAX_VALUE;

        int executed = running.executeFor(quantum);
        cpuBusyTime += executed;
        int startTimeBeforeRun = currentTime;
        currentTime += executed;

        boolean completed = running.isCompleted();
        if (completed) {
            running.markCompletedAt(currentTime);
        } else {
            // preempt or continue based on queue algo
            if (queue.getConfig().getAlgorithm() == QueueConfig.Algorithm.ROUND_ROBIN || preemptive) {
                queue.requeueForRoundRobin(running, currentTime);
            } else {
                // FCFS non-preemptive: continue running in same queue by placing at head is complex; here we requeue at tail
                queue.requeueForRoundRobin(running, currentTime);
            }
        }

        // after time advanced, enqueue any arrivals at each intermediate tick
        enqueueArrivalsBetween(startTimeBeforeRun, currentTime);

        return new StepResult(running, executed, currentTime, completed, false);
    }

    public Metrics computeMetrics() {
        int completed = 0;
        int totalTurnaround = 0;
        int totalWaiting = 0;
        int totalResponse = 0;
        for (Process p : allProcesses) {
            if (p.getCompletionTime() != null) {
                completed++;
                totalTurnaround += p.getTurnaroundTime();
                totalWaiting += p.getWaitingTime();
                totalResponse += p.getResponseTime();
            }
        }
        double utilization = currentTime == 0 ? 0.0 : (cpuBusyTime * 100.0) / currentTime;
        double throughput = currentTime == 0 ? 0.0 : (completed * 1.0) / currentTime;
        double avgTurnaround = completed == 0 ? 0.0 : (totalTurnaround * 1.0) / completed;
        double avgWaiting = completed == 0 ? 0.0 : (totalWaiting * 1.0) / completed;
        double avgResponse = completed == 0 ? 0.0 : (totalResponse * 1.0) / completed;
        return new Metrics(utilization, throughput, avgTurnaround, avgWaiting, avgResponse);
    }

    public static class StepResult {
        public final Process process;
        public final int executedTime;
        public final int timeNow;
        public final boolean completed;
        public final boolean idle;

        public StepResult(Process process, int executedTime, int timeNow, boolean completed, boolean idle) {
            this.process = process;
            this.executedTime = executedTime;
            this.timeNow = timeNow;
            this.completed = completed;
            this.idle = idle;
        }
    }

    public record Metrics(double cpuUtilizationPercent,
                          double throughputPerTimeUnit,
                          double avgTurnaroundTime,
                          double avgWaitingTime,
                          double avgResponseTime) { }
}


