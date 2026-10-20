package cpuscheduler.scheduler;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import java.util.List;

/**
 * Common interface implemented by all ten CPU scheduling algorithms.
 */
public interface Scheduler {
    /** Returns the algorithm type this scheduler implements. */
    SchedulingAlgorithmType getAlgorithmType();

    /** Called when a new process arrives and becomes ready at currentTime. */
    void onProcessArrived(ProcessModel process, int currentTime);

    /**
     * Determines which process should execute next.
     *
     * @param currentTime Current simulation clock tick
     * @param running Currently executing process, or null if CPU was idle or context switching
     * @param config Simulation configuration (quantum, aging, context switch, etc.)
     * @return Decision indicating selected process and educational reason
     */
    SchedulingDecision selectProcess(int currentTime, ProcessModel running, SimulationConfig config);

    /** Called when a running process finishes its quantum. */
    void onQuantumExpired(ProcessModel running, int currentTime, SimulationConfig config);

    /** Called when a process is preempted by a higher priority process or higher queue. */
    void onProcessPreempted(ProcessModel running, int currentTime);

    /** Called when a process has completed its total CPU burst. */
    void onProcessCompleted(ProcessModel running, int currentTime);

    /** Returns an ordered snapshot of currently ready processes waiting in the queue(s). */
    List<ProcessModel> getReadyQueueSnapshot();

    /** Resets any internal scheduler state. */
    void reset();
}
