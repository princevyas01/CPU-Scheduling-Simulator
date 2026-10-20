package cpuscheduler.experiment;

import cpuscheduler.model.SimulationResult;
import cpuscheduler.scheduler.SchedulingAlgorithmType;

import java.util.*;

/**
 * Encapsulates the results of an experimental evaluation across all 10 scheduling algorithms.
 */
public class ExperimentResult {

    public static class AlgoStats {
        public final SchedulingAlgorithmType algorithm;
        public double avgWaitingTime;
        public double avgTurnaroundTime;
        public double avgResponseTime;
        public double cpuUtilization;
        public double throughput;
        public double contextSwitches;
        public double maxWaitingTime;
        public double jainsFairnessIndex;

        public AlgoStats(SchedulingAlgorithmType algorithm) {
            this.algorithm = algorithm;
        }
    }

    private final String description;
    private final int workloadCount;
    private final Long seed;
    private final Map<SchedulingAlgorithmType, AlgoStats> statsMap = new LinkedHashMap<>();
    private final Map<String, SchedulingAlgorithmType> winnersByObjective = new LinkedHashMap<>();
    private String conclusionText;

    public ExperimentResult(String description, int workloadCount, Long seed) {
        this.description = description;
        this.workloadCount = workloadCount;
        this.seed = seed;
    }

    public void addStats(SchedulingAlgorithmType algo, AlgoStats stats) {
        statsMap.put(algo, stats);
    }

    public Map<SchedulingAlgorithmType, AlgoStats> getStatsMap() {
        return Collections.unmodifiableMap(statsMap);
    }

    public AlgoStats getStats(SchedulingAlgorithmType algo) {
        return statsMap.get(algo);
    }

    public void setWinner(String objective, SchedulingAlgorithmType algo) {
        winnersByObjective.put(objective, algo);
    }

    public SchedulingAlgorithmType getWinner(String objective) {
        return winnersByObjective.get(objective);
    }

    public Map<String, SchedulingAlgorithmType> getWinnersByObjective() {
        return Collections.unmodifiableMap(winnersByObjective);
    }

    public String getConclusionText() { return conclusionText; }
    public void setConclusionText(String conclusionText) { this.conclusionText = conclusionText; }

    public String getDescription() { return description; }
    public int getWorkloadCount() { return workloadCount; }
    public Long getSeed() { return seed; }
}
