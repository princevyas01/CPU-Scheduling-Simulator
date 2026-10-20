package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.Scheduler;
import cpuscheduler.scheduler.SchedulingAlgorithmType;

import java.util.ArrayList;
import java.util.List;

/**
 * Base scheduler providing common ready-queue operations, tie-breaking logic,
 * and string formatting helpers for educational explanations.
 */
public abstract class BaseScheduler implements Scheduler {
    protected final SchedulingAlgorithmType algorithmType;
    protected final List<ProcessModel> readyQueue = new ArrayList<>();

    protected BaseScheduler(SchedulingAlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    @Override
    public SchedulingAlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    @Override
    public void onProcessArrived(ProcessModel process, int currentTime) {
        readyQueue.add(process);
    }

    @Override
    public void onQuantumExpired(ProcessModel running, int currentTime, SimulationConfig config) {
        // Default non-RR: re-add to ready queue if not complete
        if (running != null && running.getRemainingBurst() > 0 && !readyQueue.contains(running)) {
            readyQueue.add(running);
        }
    }

    @Override
    public void onProcessPreempted(ProcessModel running, int currentTime) {
        if (running != null && running.getRemainingBurst() > 0 && !readyQueue.contains(running)) {
            readyQueue.add(running);
        }
    }

    @Override
    public void onProcessCompleted(ProcessModel running, int currentTime) {
        readyQueue.remove(running);
    }

    @Override
    public List<ProcessModel> getReadyQueueSnapshot() {
        return new ArrayList<>(readyQueue);
    }

    @Override
    public void reset() {
        readyQueue.clear();
    }

    protected static int tieBreak(ProcessModel a, ProcessModel b) {
        if (a.getArrivalTime() != b.getArrivalTime()) {
            return Integer.compare(a.getArrivalTime(), b.getArrivalTime());
        }
        return Integer.compare(a.getOrder(), b.getOrder());
    }

    protected String formatReadyQueue() {
        if (readyQueue.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < readyQueue.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(readyQueue.get(i).getId());
        }
        sb.append("]");
        return sb.toString();
    }
}
