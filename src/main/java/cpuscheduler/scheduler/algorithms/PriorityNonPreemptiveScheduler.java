package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

/**
 * Non-preemptive Priority Scheduling:
 * Lower numeric priority indicates higher priority.
 * Dispatches the process with the highest effective priority.
 * If priority aging is active, waiting processes will have improved effective priority.
 */
public class PriorityNonPreemptiveScheduler extends BaseScheduler {

    public PriorityNonPreemptiveScheduler() {
        super(SchedulingAlgorithmType.PRIORITY_NP);
    }

    @Override
    public SchedulingDecision selectProcess(int currentTime, ProcessModel running, SimulationConfig config) {
        if (running != null && running.getRemainingBurst() > 0) {
            return SchedulingDecision.continueRunning(
                    running,
                    "Process " + running.getId() + " continues execution non-preemptively until completion.",
                    "Remaining Burst = " + running.getRemainingBurst() + ", Priority = " + running.getEffectivePriority()
            );
        }

        if (readyQueue.isEmpty()) {
            return SchedulingDecision.idle("Ready queue empty; CPU is idle.");
        }

        readyQueue.sort((a, b) -> {
            if (a.getEffectivePriority() != b.getEffectivePriority()) {
                return Integer.compare(a.getEffectivePriority(), b.getEffectivePriority());
            }
            return tieBreak(a, b);
        });

        ProcessModel next = readyQueue.remove(0);
        String agingNote = next.getEffectivePriority() != next.getConfiguredPriority()
                ? " (aged from " + next.getConfiguredPriority() + ")" : "";

        return SchedulingDecision.dispatch(
                next,
                "Process " + next.getId() + " dispatched with highest priority " + next.getEffectivePriority() + agingNote + ".",
                "Effective Priority = " + next.getEffectivePriority() + ", Configured = " + next.getConfiguredPriority()
        );
    }
}
