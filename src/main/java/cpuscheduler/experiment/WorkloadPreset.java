package cpuscheduler.experiment;

/**
 * Standard workload generation presets for experimentation and comparison.
 */
public enum WorkloadPreset {
    SHORT_JOBS("Short Jobs", "Workload dominated by short bursts (1-4 units) to observe low response times."),
    LONG_JOBS("Long Jobs", "Workload dominated by long bursts (10-25 units) to evaluate batch scheduling."),
    MIXED_WORKLOAD("Mixed Workload", "Combines short and long CPU bursts to highlight convoy effects and preemption."),
    STAGGERED_ARRIVALS("Staggered Arrivals", "Processes arrive with distinct time gaps to inspect idle periods and preemption."),
    PRIORITY_CONFLICT("Priority Conflict", "Varied priorities with late urgent arrivals to contrast static priority vs aging."),
    HEAVY_CONTENTION("Heavy Contention", "Many processes (8-15) competing simultaneously for CPU resources.");

    private final String displayName;
    private final String description;

    WorkloadPreset(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public static WorkloadPreset fromDisplayName(String name) {
        for (WorkloadPreset p : values()) {
            if (p.displayName.equalsIgnoreCase(name) || p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return MIXED_WORKLOAD;
    }

    public static String[] getDisplayNames() {
        WorkloadPreset[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].displayName;
        }
        return names;
    }
}
