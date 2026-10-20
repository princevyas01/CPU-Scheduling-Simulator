package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

/**
 * First Come First Served (FCFS) Scheduling:
 * Strictly non-preemptive. Processes are dispatched in order of arrival time.
 * Ties are broken by original process input order.
 */
public class FCFSScheduler extends BaseScheduler {

    public FCFSScheduler() {
        super(SchedulingAlgorithmType.FCFS);
    }

    @Override
    public SchedulingDecision selectProcess(int currentTime, ProcessModel running, SimulationConfig config) {
        if (running != null && running.getRemainingBurst() > 0) {
            return SchedulingDecision.continueRunning(
                    running,
                    "Process " + running.getId() + " continues execution non-preemptively until burst completion.",
                    "Remaining Burst = " + running.getRemainingBurst()
            );
        }

        if (readyQueue.isEmpty()) {
            return SchedulingDecision.idle("No processes in ready queue; CPU is idle.");
        }

        readyQueue.sort((a, b) -> {
            if (a.getArrivalTime() != b.getArrivalTime()) {
                return Integer.compare(a.getArrivalTime(), b.getArrivalTime());
            }
            return Integer.compare(a.getOrder(), b.getOrder());
        });

        ProcessModel next = readyQueue.remove(0);
        return SchedulingDecision.dispatch(
                next,
                "Process " + next.getId() + " selected by earliest arrival time (AT=" + next.getArrivalTime() + ") in FIFO order.",
                "Arrival Time = " + next.getArrivalTime() + ", Burst = " + next.getBurstTime()
        );
    }
}
