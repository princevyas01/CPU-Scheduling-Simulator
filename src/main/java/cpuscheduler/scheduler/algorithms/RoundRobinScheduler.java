package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

/**
 * Round Robin (RR) Scheduling:
 * Preemptive scheduling based on a fixed time quantum.
 * Arriving processes enter the tail of a FIFO ready queue.
 * When a process's quantum expires without burst completion,
 * it is re-enqueued at the tail of the ready queue.
 */
public class RoundRobinScheduler extends BaseScheduler {

    public RoundRobinScheduler() {
        super(SchedulingAlgorithmType.ROUND_ROBIN);
    }

    @Override
    public SchedulingDecision selectProcess(int currentTime, ProcessModel running, SimulationConfig config) {
        // If current process still has remaining quantum and remaining burst, continue
        if (running != null && running.getRemainingBurst() > 0 && running.getRemainingQuantum() > 0) {
            return SchedulingDecision.continueRunning(
                    running,
                    "Process " + running.getId() + " continues execution within active time quantum.",
                    "Remaining Quantum = " + running.getRemainingQuantum() + ", Remaining Burst = " + running.getRemainingBurst()
            );
        }

        // Ready queue dispatch
        if (readyQueue.isEmpty()) {
            if (running != null && running.getRemainingBurst() > 0) {
                // Sole process retains CPU with fresh quantum
                running.setRemainingQuantum(config.getQuantum());
                return SchedulingDecision.continueRunning(
                        running,
                        "Process " + running.getId() + " retains CPU (sole ready process) with fresh quantum Q=" + config.getQuantum() + ".",
                        "Quantum reset to " + config.getQuantum() + ", Burst = " + running.getRemainingBurst()
                );
            }
            return SchedulingDecision.idle("Ready queue empty; CPU is idle.");
        }

        ProcessModel next = readyQueue.remove(0);
        next.setRemainingQuantum(config.getQuantum());

        return SchedulingDecision.dispatch(
                next,
                "Process " + next.getId() + " dispatched from head of ready queue with fresh quantum Q=" + config.getQuantum() + ".",
                "Time Quantum = " + config.getQuantum() + ", Remaining Burst = " + next.getRemainingBurst()
        );
    }

    @Override
    public void onQuantumExpired(ProcessModel running, int currentTime, SimulationConfig config) {
        if (running != null && running.getRemainingBurst() > 0) {
            readyQueue.add(running);
        }
    }
}
