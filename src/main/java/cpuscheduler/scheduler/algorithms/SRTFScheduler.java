package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

/**
 * Shortest Remaining Time First (SRTF) Scheduling:
 * Preemptive SJF. At each scheduling decision, the process with the smallest
 * remaining CPU burst runs. If a ready process has strictly less remaining burst
 * than the currently running process, preemption occurs.
 */
public class SRTFScheduler extends BaseScheduler {

    public SRTFScheduler() {
        super(SchedulingAlgorithmType.SRTF);
    }

    private void sortReadyQueue() {
        readyQueue.sort((a, b) -> {
            if (a.getRemainingBurst() != b.getRemainingBurst()) {
                return Integer.compare(a.getRemainingBurst(), b.getRemainingBurst());
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
                    "Process " + next.getId() + " dispatched with shortest remaining burst (rem=" + next.getRemainingBurst() + ").",
                    "Remaining Burst = " + next.getRemainingBurst() + ", AT = " + next.getArrivalTime()
            );
        }

        // Running process is active: check for preemption
        if (!readyQueue.isEmpty()) {
            ProcessModel best = readyQueue.get(0);
            if (best.getRemainingBurst() < running.getRemainingBurst()) {
                readyQueue.remove(0);
                return SchedulingDecision.preempt(
                        best,
                        "Process " + best.getId() + " (rem=" + best.getRemainingBurst() + ") preempts running "
                                + running.getId() + " (rem=" + running.getRemainingBurst() + ") with shorter remaining burst.",
                        "New rem=" + best.getRemainingBurst() + " < Current rem=" + running.getRemainingBurst()
                );
            }
        }

        return SchedulingDecision.continueRunning(
                running,
                "Process " + running.getId() + " retains CPU with shortest remaining burst (rem=" + running.getRemainingBurst() + ").",
                "Remaining Burst = " + running.getRemainingBurst()
        );
    }
}
