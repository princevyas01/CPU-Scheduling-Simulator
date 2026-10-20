package cpuscheduler.model;

import java.util.*;

/**
 * Immutable outcome of a completed simulation run.
 * Contains process metrics, execution timeline, Gantt segments,
 * ready-queue snapshots, decision events, and summary statistics.
 */
public class SimulationResult {
    private final String algorithmName;
    private final SimulationConfig config;
    private final List<ProcessModel> processes;
    private final List<GanttSegment> segments;
    private final int[] timeline;
    private final int[][] queueSnapshots;
    private final List<SchedulingEvent> events;

    // Summary Metrics
    private final int totalTime;
    private final int busyTime;
    private final int idleTime;
    private final int contextSwitchTime;
    private final int contextSwitchCount;
    private final double avgWaitingTime;
    private final double avgTurnaroundTime;
    private final double avgResponseTime;
    private final double cpuUtilization;
    private final double idlePercentage;
    private final double contextSwitchPercentage;
    private final double throughput;

    // Fairness & Distribution Metrics
    private final int minWaitingTime;
    private final int maxWaitingTime;
    private final double medianWaitingTime;
    private final double waitingTimeVariance;
    private final double waitingTimeStdDev;
    private final double jainsFairnessIndex;
    private final List<String> starvationWarnings;

    public SimulationResult(String algorithmName,
                            SimulationConfig config,
                            List<ProcessModel> processes,
                            List<GanttSegment> segments,
                            int[] timeline,
                            int[][] queueSnapshots,
                            List<SchedulingEvent> events,
                            int totalTime,
                            int busyTime,
                            int idleTime,
                            int contextSwitchTime,
                            int contextSwitchCount) {
        this.algorithmName = algorithmName;
        this.config = config;
        this.processes = Collections.unmodifiableList(new ArrayList<>(processes));
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
        this.timeline = timeline != null ? timeline.clone() : new int[0];
        this.queueSnapshots = queueSnapshots != null ? queueSnapshots.clone() : new int[0][];
        this.events = Collections.unmodifiableList(new ArrayList<>(events));

        this.totalTime = totalTime;
        this.busyTime = busyTime;
        this.idleTime = idleTime;
        this.contextSwitchTime = contextSwitchTime;
        this.contextSwitchCount = contextSwitchCount;

        int n = processes.size();
        if (n > 0) {
            double sumWT = 0, sumTAT = 0, sumRT = 0;
            int minW = Integer.MAX_VALUE, maxW = Integer.MIN_VALUE;
            double[] wtArray = new double[n];
            List<String> starvList = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                ProcessModel p = processes.get(i);
                int wt = p.getWaitingTime();
                sumWT += wt;
                sumTAT += p.getTurnaroundTime();
                sumRT += p.getResponseTime();
                wtArray[i] = wt;
                minW = Math.min(minW, wt);
                maxW = Math.max(maxW, wt);

                if (wt >= config.getStarvationThreshold()) {
                    starvList.add(p.getId() + " (Wait=" + wt + " >= Threshold=" + config.getStarvationThreshold() + ")");
                }
            }

            this.avgWaitingTime = sumWT / n;
            this.avgTurnaroundTime = sumTAT / n;
            this.avgResponseTime = sumRT / n;
            this.minWaitingTime = minW == Integer.MAX_VALUE ? 0 : minW;
            this.maxWaitingTime = maxW == Integer.MIN_VALUE ? 0 : maxW;
            this.starvationWarnings = Collections.unmodifiableList(starvList);

            // Median WT
            Arrays.sort(wtArray);
            if (n % 2 == 1) {
                this.medianWaitingTime = wtArray[n / 2];
            } else {
                this.medianWaitingTime = (wtArray[(n / 2) - 1] + wtArray[n / 2]) / 2.0;
            }

            // Variance & StdDev of WT
            double variance = 0;
            for (double w : wtArray) {
                variance += Math.pow(w - this.avgWaitingTime, 2);
            }
            this.waitingTimeVariance = variance / n;
            this.waitingTimeStdDev = Math.sqrt(this.waitingTimeVariance);

            // Jain's Fairness Index for Waiting Times (or normalized service turnaround)
            // Using Normalized Turnaround or Throughput / Waiting.
            // When computing fairness on waiting times, to avoid zero or division issues:
            // Let x_i = (BT_i / TAT_i) representing service progress ratio (between 0 and 1).
            // Jain's Index J = (sum x_i)^2 / (n * sum x_i^2). J in [1/n, 1.0], 1.0 = perfectly fair.
            double sumProgress = 0;
            double sumProgressSq = 0;
            for (ProcessModel p : processes) {
                double progress = p.getTurnaroundTime() > 0 ? (double) p.getBurstTime() / p.getTurnaroundTime() : 1.0;
                sumProgress += progress;
                sumProgressSq += progress * progress;
            }
            if (sumProgressSq > 0) {
                this.jainsFairnessIndex = Math.min(1.0, Math.max(0.0, (sumProgress * sumProgress) / (n * sumProgressSq)));
            } else {
                this.jainsFairnessIndex = 1.0;
            }
        } else {
            this.avgWaitingTime = 0;
            this.avgTurnaroundTime = 0;
            this.avgResponseTime = 0;
            this.minWaitingTime = 0;
            this.maxWaitingTime = 0;
            this.medianWaitingTime = 0;
            this.waitingTimeVariance = 0;
            this.waitingTimeStdDev = 0;
            this.jainsFairnessIndex = 1.0;
            this.starvationWarnings = Collections.emptyList();
        }

        if (totalTime > 0) {
            this.cpuUtilization = (busyTime * 100.0) / totalTime;
            this.idlePercentage = (idleTime * 100.0) / totalTime;
            this.contextSwitchPercentage = (contextSwitchTime * 100.0) / totalTime;
            this.throughput = (double) n / totalTime;
        } else {
            this.cpuUtilization = 0;
            this.idlePercentage = 0;
            this.contextSwitchPercentage = 0;
            this.throughput = 0;
        }
    }

    public String getAlgorithmName() { return algorithmName; }
    public SimulationConfig getConfig() { return config; }
    public List<ProcessModel> getProcesses() { return processes; }
    public List<GanttSegment> getSegments() { return segments; }
    public int[] getTimeline() { return timeline.clone(); }
    public int[][] getQueueSnapshots() { return queueSnapshots.clone(); }
    public List<SchedulingEvent> getEvents() { return events; }

    public int getTotalTime() { return totalTime; }
    public int getBusyTime() { return busyTime; }
    public int getIdleTime() { return idleTime; }
    public int getContextSwitchTime() { return contextSwitchTime; }
    public int getContextSwitchCount() { return contextSwitchCount; }
    public double getAvgWaitingTime() { return avgWaitingTime; }
    public double getAvgTurnaroundTime() { return avgTurnaroundTime; }
    public double getAvgResponseTime() { return avgResponseTime; }
    public double getCpuUtilization() { return cpuUtilization; }
    public double getIdlePercentage() { return idlePercentage; }
    public double getContextSwitchPercentage() { return contextSwitchPercentage; }
    public double getThroughput() { return throughput; }

    public int getMinWaitingTime() { return minWaitingTime; }
    public int getMaxWaitingTime() { return maxWaitingTime; }
    public double getMedianWaitingTime() { return medianWaitingTime; }
    public double getWaitingTimeVariance() { return waitingTimeVariance; }
    public double getWaitingTimeStdDev() { return waitingTimeStdDev; }
    public double getJainsFairnessIndex() { return jainsFairnessIndex; }
    public List<String> getStarvationWarnings() { return starvationWarnings; }
}
