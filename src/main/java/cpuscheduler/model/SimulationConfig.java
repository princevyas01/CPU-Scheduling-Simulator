package cpuscheduler.model;

/**
 * Configuration parameters for a CPU scheduling simulation run.
 */
public class SimulationConfig {
    private final int quantum;
    private final int contextSwitchOverhead;
    private final boolean agingEnabled;
    private final int agingInterval;
    private final int priorityBoost;
    private final int minPriority;
    private final int starvationThreshold;

    public SimulationConfig(int quantum,
                            int contextSwitchOverhead,
                            boolean agingEnabled,
                            int agingInterval,
                            int priorityBoost,
                            int minPriority,
                            int starvationThreshold) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("Time quantum must be strictly positive (got " + quantum + ").");
        }
        if (contextSwitchOverhead < 0) {
            throw new IllegalArgumentException("Context switch overhead must be non-negative (got " + contextSwitchOverhead + ").");
        }
        if (agingInterval <= 0) {
            throw new IllegalArgumentException("Aging interval must be positive (got " + agingInterval + ").");
        }
        if (priorityBoost <= 0) {
            throw new IllegalArgumentException("Priority boost must be positive (got " + priorityBoost + ").");
        }
        if (minPriority < 0) {
            throw new IllegalArgumentException("Minimum priority must be non-negative (got " + minPriority + ").");
        }
        if (starvationThreshold <= 0) {
            throw new IllegalArgumentException("Starvation threshold must be positive (got " + starvationThreshold + ").");
        }

        this.quantum = quantum;
        this.contextSwitchOverhead = contextSwitchOverhead;
        this.agingEnabled = agingEnabled;
        this.agingInterval = agingInterval;
        this.priorityBoost = priorityBoost;
        this.minPriority = minPriority;
        this.starvationThreshold = starvationThreshold;
    }

    public static SimulationConfig defaultConfig() {
        return new SimulationConfig(2, 0, false, 5, 1, 0, 15);
    }

    public static SimulationConfig withQuantum(int q) {
        return new SimulationConfig(q, 0, false, 5, 1, 0, 15);
    }

    public int getQuantum() { return quantum; }
    public int getContextSwitchOverhead() { return contextSwitchOverhead; }
    public boolean isAgingEnabled() { return agingEnabled; }
    public int getAgingInterval() { return agingInterval; }
    public int getPriorityBoost() { return priorityBoost; }
    public int getMinPriority() { return minPriority; }
    public int getStarvationThreshold() { return starvationThreshold; }

    @Override
    public String toString() {
        return "Config{quantum=" + quantum +
                ", csOverhead=" + contextSwitchOverhead +
                ", aging=" + agingEnabled +
                (agingEnabled ? " (interval=" + agingInterval + ", boost=" + priorityBoost + ", minPr=" + minPriority + ")" : "") +
                ", starvThreshold=" + starvationThreshold + "}";
    }
}
