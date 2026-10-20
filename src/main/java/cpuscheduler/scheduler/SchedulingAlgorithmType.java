package cpuscheduler.scheduler;

/**
 * Enumeration of all 10 CPU scheduling algorithms supported by CPU Scheduling Studio.
 */
public enum SchedulingAlgorithmType {
    FCFS("FCFS", "FCFS", false, false, false,
            "First Come First Served. Runs processes strictly in arrival order, non-preemptive. " +
            "Simple and fair in admission order, but short jobs can get stuck behind a long one (convoy effect)."),

    SJF("SJF (Non-preemptive)", "SJF NP", false, false, false,
            "Shortest Job First. Among arrived processes, runs the one with the smallest burst time to completion. " +
            "Minimizes average waiting time for non-preemptive policies; longer jobs may experience starvation under heavy load."),

    SRTF("SRTF (Preemptive)", "SRTF P", true, false, false,
            "Shortest Remaining Time First (preemptive SJF). At every decision point, the process with the least remaining " +
            "CPU burst executes. A newly arrived short process will preempt a running process with longer remaining time."),

    LJF("LJF (Non-preemptive)", "LJF NP", false, false, false,
            "Longest Job First. Picks the largest initial burst time among arrived processes and runs it to completion. " +
            "Useful as an educational contrast to SJF to illustrate convoy effects and high turnaround times."),

    LRTF("LRTF (Preemptive)", "LRTF P", true, false, false,
            "Longest Remaining Time First (preemptive LJF). The process with the most remaining burst executes at each step. " +
            "Equally long jobs interleave their execution, providing balanced progress across long workloads."),

    HRRN("HRRN", "HRRN", false, false, false,
            "Highest Response Ratio Next. Picks the arrived process with the largest Response Ratio = (Waiting + Burst) / Burst. " +
            "Non-preemptive. As processes wait, their response ratio increases, naturally preventing starvation."),

    PRIORITY_NP("Priority (Non-preemptive)", "Priority NP", false, false, true,
            "Non-preemptive Priority Scheduling. The arrived process with the highest priority (lower numeric value = higher priority) " +
            "runs to completion. Configurable priority aging can boost waiting processes over time."),

    PRIORITY_P("Priority (Preemptive)", "Priority P", true, false, true,
            "Preemptive Priority Scheduling. The arrived process with the most urgent priority runs. If a more urgent process arrives, " +
            "it immediately preempts the running process. Configurable priority aging prevents starvation of low-priority tasks."),

    ROUND_ROBIN("Round Robin", "RR", true, true, false,
            "Round Robin. Each process runs for at most one time quantum, then returns to the back of the ready queue. " +
            "Highly responsive and fair; the quantum sets the trade-off between responsiveness and context-switch frequency."),

    MLFQ("Multilevel Feedback Queue", "MLFQ", true, true, false,
            "Multilevel Feedback Queue with 3 priority levels. New processes start in Queue 0 (Quantum Q). Processes that exhaust " +
            "their quantum without completing are demoted to Queue 1 (Quantum 2Q), and then Queue 2 (FCFS). " +
            "Higher queues preempt lower ones. Preempted processes retain their remaining quantum allotment upon resumption.");

    private final String displayName;
    private final String shortName;
    private final boolean preemptive;
    private final boolean usesQuantum;
    private final boolean supportsAging;
    private final String description;

    SchedulingAlgorithmType(String displayName, String shortName, boolean preemptive, boolean usesQuantum, boolean supportsAging, String description) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.preemptive = preemptive;
        this.usesQuantum = usesQuantum;
        this.supportsAging = supportsAging;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getShortName() { return shortName; }
    public boolean isPreemptive() { return preemptive; }
    public boolean usesQuantum() { return usesQuantum; }
    public boolean supportsAging() { return supportsAging; }
    public String getDescription() { return description; }

    public static SchedulingAlgorithmType fromDisplayName(String name) {
        for (SchedulingAlgorithmType t : values()) {
            if (t.displayName.equalsIgnoreCase(name) || t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        // Match partial
        for (SchedulingAlgorithmType t : values()) {
            if (t.displayName.toLowerCase().contains(name.toLowerCase())) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown scheduling algorithm: " + name);
    }

    public static String[] getDisplayNames() {
        SchedulingAlgorithmType[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].displayName;
        }
        return names;
    }
}
