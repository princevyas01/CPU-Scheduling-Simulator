package cpuscheduler.scheduler;

import cpuscheduler.scheduler.algorithms.*;

/**
 * Factory for creating scheduler instances.
 */
public class SchedulerFactory {

    public static Scheduler create(SchedulingAlgorithmType type) {
        switch (type) {
            case FCFS:
                return new FCFSScheduler();
            case SJF:
                return new SJFScheduler();
            case SRTF:
                return new SRTFScheduler();
            case LJF:
                return new LJFScheduler();
            case LRTF:
                return new LRTFScheduler();
            case HRRN:
                return new HRRNScheduler();
            case PRIORITY_NP:
                return new PriorityNonPreemptiveScheduler();
            case PRIORITY_P:
                return new PriorityPreemptiveScheduler();
            case ROUND_ROBIN:
                return new RoundRobinScheduler();
            case MLFQ:
                return new MLFQScheduler();
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + type);
        }
    }

    public static Scheduler create(String displayName) {
        return create(SchedulingAlgorithmType.fromDisplayName(displayName));
    }
}
