package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

/**
 * Longest Job First (LJF) Scheduling:
 * Non-preemptive. Selects the arrived process with the largest initial CPU burst.
 * Ties are broken by arrival time, then input order.
 */
public class LJFScheduler extends BaseScheduler {

    public LJFScheduler() {
        super(SchedulingAlgorithmType.LJF);
    }

    @Override
    public SchedulingDecision selectProcess(int currentTime, ProcessModel running, SimulationConfig config) {
        if (running != null && running.getRemainingBurst() > 0) {
            return SchedulingDecision.continueRunning(
                    running,
                    "Process " + running.getId() + " continues execution non-preemptively until completion.",
                    "Remaining Burst = " + running.getRemainingBurst()
            );
        }

        if (readyQueue.isEmpty()) {
            return SchedulingDecision.idle("Ready queue empty; CPU is idle.");
        }

        readyQueue.sort((a, b) -> {
            if (a.getBurstTime() != b.getBurstTime()) {
                return Integer.compare(b.getBurstTime(), a.getBurstTime()); // Largest first
            }
            return tieBreak(a, b);
        });

        ProcessModel next = readyQueue.remove(0);
        return SchedulingDecision.dispatch(
                next,
                "Process " + next.getId() + " selected with largest initial burst (BT=" + next.getBurstTime() + ").",
                "Burst = " + next.getBurstTime() + ", AT = " + next.getArrivalTime()
        );
    }
}
