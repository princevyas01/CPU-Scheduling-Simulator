package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

/**
 * Preemptive Priority Scheduling:
 * Lower numeric priority indicates higher priority.
 * If a process in the ready queue has strictly higher priority (lower numeric value)
 * than the currently running process, preemption occurs immediately.
 */
public class PriorityPreemptiveScheduler extends BaseScheduler {

    public PriorityPreemptiveScheduler() {
        super(SchedulingAlgorithmType.PRIORITY_P);
    }

    private void sortReadyQueue() {
        readyQueue.sort((a, b) -> {
            if (a.getEffectivePriority() != b.getEffectivePriority()) {
                return Integer.compare(a.getEffectivePriority(), b.getEffectivePriority());
            }
            return tieBreak(a, b);
        });
    }

    @Override
    public SchedulingDecision selectProcess(int currentTime, ProcessModel running, SimulationConfig config) {
        sortReadyQueue();

        if (running == null || running.getRemainingBurst() <= 0) {
            if (readyQueue.isEmpty()) {
                return SchedulingDecision.idle("Ready queue empty; CPU is idle.");
            }
            ProcessModel next = readyQueue.remove(0);
            String agingNote = next.getEffectivePriority() != next.getConfiguredPriority()
                    ? " (aged from " + next.getConfiguredPriority() + ")" : "";
            return SchedulingDecision.dispatch(
                    next,
                    "Process " + next.getId() + " dispatched with highest priority " + next.getEffectivePriority() + agingNote + ".",
                    "Effective Priority = " + next.getEffectivePriority() + ", Burst = " + next.getBurstTime()
            );
        }

        if (!readyQueue.isEmpty()) {
            ProcessModel best = readyQueue.get(0);
            if (best.getEffectivePriority() < running.getEffectivePriority()) {
                readyQueue.remove(0);
                String agingNote = best.getEffectivePriority() != best.getConfiguredPriority()
                        ? " (aged from " + best.getConfiguredPriority() + ")" : "";
                return SchedulingDecision.preempt(
                        best,
                        "Process " + best.getId() + " (priority " + best.getEffectivePriority() + agingNote + ") preempts running "
                                + running.getId() + " (priority " + running.getEffectivePriority() + ") with higher priority.",
                        "New Priority=" + best.getEffectivePriority() + " < Current Priority=" + running.getEffectivePriority()
                );
            }
        }

        return SchedulingDecision.continueRunning(
                running,
                "Process " + running.getId() + " retains CPU with most urgent priority " + running.getEffectivePriority() + ".",
                "Priority = " + running.getEffectivePriority() + ", Remaining Burst = " + running.getRemainingBurst()
        );
    }
}
