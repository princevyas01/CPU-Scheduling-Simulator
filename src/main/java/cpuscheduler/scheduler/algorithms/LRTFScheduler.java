package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

/**
 * Longest Remaining Time First (LRTF) Scheduling:
 * Preemptive LJF. At each scheduling step, the process with the largest
 * remaining burst time runs. If another process has more remaining burst,
 * or ties with an earlier arrival time/index, preemption occurs.
 */
public class LRTFScheduler extends BaseScheduler {

    public LRTFScheduler() {
        super(SchedulingAlgorithmType.LRTF);
    }

    private void sortReadyQueue() {
        readyQueue.sort((a, b) -> {
            if (a.getRemainingBurst() != b.getRemainingBurst()) {
                return Integer.compare(b.getRemainingBurst(), a.getRemainingBurst()); // Largest first
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
            return SchedulingDecision.dispatch(
                    next,
                    "Process " + next.getId() + " dispatched with largest remaining burst (rem=" + next.getRemainingBurst() + ").",
                    "Remaining Burst = " + next.getRemainingBurst() + ", AT = " + next.getArrivalTime()
            );
        }

        if (!readyQueue.isEmpty()) {
            ProcessModel best = readyQueue.get(0);
            boolean shouldPreempt = false;
            if (best.getRemainingBurst() > running.getRemainingBurst()) {
                shouldPreempt = true;
            } else if (best.getRemainingBurst() == running.getRemainingBurst() && tieBreak(best, running) < 0) {
                shouldPreempt = true;
            }

            if (shouldPreempt) {
                readyQueue.remove(0);
                return SchedulingDecision.preempt(
                        best,
                        "Process " + best.getId() + " (rem=" + best.getRemainingBurst() + ") preempts running "
                                + running.getId() + " (rem=" + running.getRemainingBurst() + ") with larger remaining burst.",
                        "New rem=" + best.getRemainingBurst() + " >= Current rem=" + running.getRemainingBurst()
                );
            }
        }

        return SchedulingDecision.continueRunning(
                running,
                "Process " + running.getId() + " retains CPU with longest remaining burst (rem=" + running.getRemainingBurst() + ").",
                "Remaining Burst = " + running.getRemainingBurst()
        );
    }
}
