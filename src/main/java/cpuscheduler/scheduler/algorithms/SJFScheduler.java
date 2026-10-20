package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

/**
 * Shortest Job First (SJF) Scheduling:
 * Non-preemptive. Selects the arrived process with the smallest initial CPU burst.
 * Ties are broken by arrival time, then input order.
 */
public class SJFScheduler extends BaseScheduler {

    public SJFScheduler() {
        super(SchedulingAlgorithmType.SJF);
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
                return Integer.compare(a.getBurstTime(), b.getBurstTime());
            }
            return tieBreak(a, b);
        });

        ProcessModel next = readyQueue.remove(0);
        return SchedulingDecision.dispatch(
                next,
                "Process " + next.getId() + " selected with shortest burst time (BT=" + next.getBurstTime() + ") among ready processes.",
                "Burst = " + next.getBurstTime() + ", AT = " + next.getArrivalTime()
        );
    }
}
