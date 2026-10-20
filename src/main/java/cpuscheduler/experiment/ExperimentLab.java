package cpuscheduler.experiment;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.model.SimulationResult;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.simulation.SimulationEngine;

import java.util.*;
import java.util.function.Consumer;

/**
 * Runs experimental workloads across all ten algorithms,
 * calculates statistical aggregates, determines optimal algorithms per objective,
 * and formulates educational data-driven conclusions.
 */
public class ExperimentLab {

    public interface ProgressListener {
        void onProgress(int currentStep, int totalSteps, String status);
    }

    public static ExperimentResult runExperiment(WorkloadPreset preset,
                                                 int processCount,
                                                 int workloadCount,
                                                 SimulationConfig config,
                                                 Long seed,
                                                 ProgressListener listener) {
        ExperimentResult expResult = new ExperimentResult(
                preset.getDisplayName() + " (N=" + processCount + ", Workloads=" + workloadCount + ")",
                workloadCount,
                seed
        );

        SchedulingAlgorithmType[] algorithms = SchedulingAlgorithmType.values();
        int totalSteps = workloadCount * algorithms.length;
        int currentStep = 0;

        // Maps to accumulate totals across workloads
        Map<SchedulingAlgorithmType, ExperimentResult.AlgoStats> statsMap = new LinkedHashMap<>();
        for (SchedulingAlgorithmType algo : algorithms) {
            statsMap.put(algo, new ExperimentResult.AlgoStats(algo));
        }

        Random seedRnd = seed != null ? new Random(seed) : new Random();

        for (int w = 0; w < workloadCount; w++) {
            long currentSeed = seedRnd.nextLong();
            List<ProcessModel> workload = WorkloadGenerator.generate(preset, processCount, currentSeed);

            for (SchedulingAlgorithmType algo : algorithms) {
                if (listener != null) {
                    listener.onProgress(currentStep, totalSteps, "Evaluating " + algo.getDisplayName() + " on workload " + (w + 1) + "/" + workloadCount);
                }

                // Simulate using exact same workload copy
                SimulationResult res = SimulationEngine.simulate(workload, algo, config);

                ExperimentResult.AlgoStats stats = statsMap.get(algo);
                stats.avgWaitingTime += res.getAvgWaitingTime();
                stats.avgTurnaroundTime += res.getAvgTurnaroundTime();
                stats.avgResponseTime += res.getAvgResponseTime();
                stats.cpuUtilization += res.getCpuUtilization();
                stats.throughput += res.getThroughput();
                stats.contextSwitches += res.getContextSwitchCount();
                stats.maxWaitingTime = Math.max(stats.maxWaitingTime, res.getMaxWaitingTime());
                stats.jainsFairnessIndex += res.getJainsFairnessIndex();

                currentStep++;
            }
        }

        // Compute averages across workloads
        for (SchedulingAlgorithmType algo : algorithms) {
            ExperimentResult.AlgoStats stats = statsMap.get(algo);
            stats.avgWaitingTime /= workloadCount;
            stats.avgTurnaroundTime /= workloadCount;
            stats.avgResponseTime /= workloadCount;
            stats.cpuUtilization /= workloadCount;
            stats.throughput /= workloadCount;
            stats.contextSwitches /= workloadCount;
            stats.jainsFairnessIndex /= workloadCount;
            expResult.addStats(algo, stats);
        }

        // Determine winners for each objective
        SchedulingAlgorithmType bestWT = null, bestTAT = null, bestRT = null, bestThr = null, bestCS = null, bestFair = null;
        double minWT = Double.MAX_VALUE, minTAT = Double.MAX_VALUE, minRT = Double.MAX_VALUE;
        double maxThr = -1.0, minCS = Double.MAX_VALUE, maxFair = -1.0;

        for (SchedulingAlgorithmType algo : algorithms) {
            ExperimentResult.AlgoStats s = statsMap.get(algo);
            if (s.avgWaitingTime < minWT) { minWT = s.avgWaitingTime; bestWT = algo; }
            if (s.avgTurnaroundTime < minTAT) { minTAT = s.avgTurnaroundTime; bestTAT = algo; }
            if (s.avgResponseTime < minRT) { minRT = s.avgResponseTime; bestRT = algo; }
            if (s.throughput > maxThr) { maxThr = s.throughput; bestThr = algo; }
            if (s.contextSwitches < minCS) { minCS = s.contextSwitches; bestCS = algo; }
            if (s.jainsFairnessIndex > maxFair) { maxFair = s.jainsFairnessIndex; bestFair = algo; }
        }

        expResult.setWinner("Min Waiting Time", bestWT);
        expResult.setWinner("Min Turnaround Time", bestTAT);
        expResult.setWinner("Min Response Time", bestRT);
        expResult.setWinner("Max Throughput", bestThr);
        expResult.setWinner("Min Context Switches", bestCS);
        expResult.setWinner("Max Fairness", bestFair);

        // Formulate educational conclusion
        StringBuilder conclusion = new StringBuilder();
        conclusion.append("Analysis based on ").append(workloadCount).append(" evaluated workload(s):\n");
        conclusion.append("• Shortest-job policies (").append(bestWT.getDisplayName()).append(") achieved the lowest average waiting time (")
                .append(String.format(Locale.US, "%.2f", minWT)).append(" units).\n");
        conclusion.append("• Most responsive policy was ").append(bestRT.getDisplayName()).append(" with average response time of ")
                .append(String.format(Locale.US, "%.2f", minRT)).append(" units.\n");
        conclusion.append("• Minimum context-switch overhead was delivered by ").append(bestCS.getDisplayName())
                .append(" with ").append(String.format(Locale.US, "%.1f", minCS)).append(" switches.\n");
        conclusion.append("• Highest fairness (Jain's Index = ").append(String.format(Locale.US, "%.3f", maxFair))
                .append(") was achieved by ").append(bestFair.getDisplayName()).append(".");

        expResult.setConclusionText(conclusion.toString());

        if (listener != null) {
            listener.onProgress(totalSteps, totalSteps, "Experiment complete.");
        }

        return expResult;
    }
}
