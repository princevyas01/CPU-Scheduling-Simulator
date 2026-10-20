package cpuscheduler.model;

import java.util.Objects;

/**
 * Represents a single process in the CPU scheduling simulation.
 * Tracks both configured parameters (arrival, burst, priority)
 * and execution lifecycle metrics (start, completion, TAT, WT, RT).
 */
public class ProcessModel implements Comparable<ProcessModel> {
    private final String id;
    private final int arrivalTime;
    private final int burstTime;
    private final int configuredPriority;
    private final int order;

    // Dynamic execution state
    private int remainingBurst;
    private int effectivePriority;
    private int firstStartTime = -1;
    private int completionTime = 0;
    private int turnaroundTime = 0;
    private int waitingTime = 0;
    private int responseTime = 0;

    // Queue & Aging state
    private int currentQueueLevel = 0;
    private int remainingQuantum = 0;
    private int readyWaitTime = 0;
    private int agingEventsCount = 0;
    private boolean completed = false;

    public ProcessModel(String id, int arrivalTime, int burstTime, int configuredPriority, int order) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Process ID cannot be empty.");
        }
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("Arrival time must be non-negative (" + id + ").");
        }
        if (burstTime <= 0) {
            throw new IllegalArgumentException("Burst time must be positive (" + id + ").");
        }
        if (configuredPriority < 0) {
            throw new IllegalArgumentException("Priority must be non-negative (" + id + ").");
        }

        this.id = id.trim();
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.configuredPriority = configuredPriority;
        this.order = order;

        this.remainingBurst = burstTime;
        this.effectivePriority = configuredPriority;
    }

    public ProcessModel(String id, int arrivalTime, int burstTime, int configuredPriority) {
        this(id, arrivalTime, burstTime, configuredPriority, 0);
    }

    public ProcessModel copy() {
        ProcessModel cp = new ProcessModel(this.id, this.arrivalTime, this.burstTime, this.configuredPriority, this.order);
        cp.remainingBurst = this.remainingBurst;
        cp.effectivePriority = this.effectivePriority;
        cp.firstStartTime = this.firstStartTime;
        cp.completionTime = this.completionTime;
        cp.turnaroundTime = this.turnaroundTime;
        cp.waitingTime = this.waitingTime;
        cp.responseTime = this.responseTime;
        cp.currentQueueLevel = this.currentQueueLevel;
        cp.remainingQuantum = this.remainingQuantum;
        cp.readyWaitTime = this.readyWaitTime;
        cp.agingEventsCount = this.agingEventsCount;
        cp.completed = this.completed;
        return cp;
    }

    public void resetExecutionState() {
        this.remainingBurst = this.burstTime;
        this.effectivePriority = this.configuredPriority;
        this.firstStartTime = -1;
        this.completionTime = 0;
        this.turnaroundTime = 0;
        this.waitingTime = 0;
        this.responseTime = 0;
        this.currentQueueLevel = 0;
        this.remainingQuantum = 0;
        this.readyWaitTime = 0;
        this.agingEventsCount = 0;
        this.completed = false;
    }

    // Getters & Setters
    public String getId() { return id; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getConfiguredPriority() { return configuredPriority; }
    public int getOrder() { return order; }

    public int getRemainingBurst() { return remainingBurst; }
    public void setRemainingBurst(int remainingBurst) {
        this.remainingBurst = Math.max(0, remainingBurst);
    }
    public void decrementRemainingBurst() {
        if (this.remainingBurst > 0) this.remainingBurst--;
    }

    public int getEffectivePriority() { return effectivePriority; }
    public void setEffectivePriority(int effectivePriority) {
        this.effectivePriority = effectivePriority;
    }
    public void agePriority(int delta, int minPriority) {
        this.effectivePriority = Math.max(minPriority, this.effectivePriority - delta);
        this.agingEventsCount++;
    }

    public int getFirstStartTime() { return firstStartTime; }
    public void setFirstStartTime(int firstStartTime) {
        if (this.firstStartTime < 0) {
            this.firstStartTime = firstStartTime;
            this.responseTime = Math.max(0, firstStartTime - arrivalTime);
        }
    }

    public int getCompletionTime() { return completionTime; }
    public void setCompletionTime(int completionTime) {
        this.completionTime = completionTime;
        this.turnaroundTime = completionTime - arrivalTime;
        this.waitingTime = this.turnaroundTime - burstTime;
        this.completed = true;
    }

    public int getTurnaroundTime() { return turnaroundTime; }
    public int getWaitingTime() { return waitingTime; }
    public int getResponseTime() { return responseTime; }

    public int getCurrentQueueLevel() { return currentQueueLevel; }
    public void setCurrentQueueLevel(int currentQueueLevel) {
        this.currentQueueLevel = currentQueueLevel;
    }

    public int getRemainingQuantum() { return remainingQuantum; }
    public void setRemainingQuantum(int remainingQuantum) {
        this.remainingQuantum = remainingQuantum;
    }
    public void decrementRemainingQuantum() {
        if (this.remainingQuantum > 0) this.remainingQuantum--;
    }

    public int getReadyWaitTime() { return readyWaitTime; }
    public void setReadyWaitTime(int readyWaitTime) {
        this.readyWaitTime = readyWaitTime;
    }
    public void incrementReadyWaitTime() {
        this.readyWaitTime++;
    }

    public int getAgingEventsCount() { return agingEventsCount; }
    public boolean isCompleted() { return completed; }

    @Override
    public int compareTo(ProcessModel o) {
        if (this.arrivalTime != o.arrivalTime) {
            return Integer.compare(this.arrivalTime, o.arrivalTime);
        }
        return Integer.compare(this.order, o.order);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessModel that = (ProcessModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Process{" + id + ", AT=" + arrivalTime + ", BT=" + burstTime + ", PR=" + configuredPriority + "}";
    }
}
