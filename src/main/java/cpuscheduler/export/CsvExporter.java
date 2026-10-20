package cpuscheduler.export;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Exports complete simulation results to compliant CSV format with proper escaping.
 */
public class CsvExporter {

    public static void exportToFile(SimulationResult result, File file) throws IOException {
        String csv = generateCsv(result);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.print(csv);
        }
    }

    public static String generateCsv(SimulationResult result) {
        StringBuilder sb = new StringBuilder();

        // 1. Process Table
        sb.append("PID,Arrival Time,Burst Time,Priority,Start Time,Completion Time,Turnaround Time,Waiting Time,Response Time\n");
        for (ProcessModel p : result.getProcesses()) {
            sb.append(escape(p.getId())).append(",")
              .append(p.getArrivalTime()).append(",")
              .append(p.getBurstTime()).append(",")
              .append(p.getConfiguredPriority()).append(",")
              .append(p.getFirstStartTime()).append(",")
              .append(p.getCompletionTime()).append(",")
              .append(p.getTurnaroundTime()).append(",")
              .append(p.getWaitingTime()).append(",")
              .append(p.getResponseTime()).append("\n");
        }

        sb.append("\n");
        sb.append("Metric,Value\n");
        sb.append("Algorithm,").append(escape(result.getAlgorithmName())).append("\n");
        sb.append("Time Quantum,").append(result.getConfig().getQuantum()).append("\n");
        sb.append("Context Switch Overhead,").append(result.getConfig().getContextSwitchOverhead()).append("\n");
        sb.append("Priority Aging Enabled,").append(result.getConfig().isAgingEnabled()).append("\n");
        if (result.getConfig().isAgingEnabled()) {
            sb.append("Aging Interval,").append(result.getConfig().getAgingInterval()).append("\n");
            sb.append("Priority Boost,").append(result.getConfig().getPriorityBoost()).append("\n");
        }
        sb.append("Total Elapsed Time,").append(result.getTotalTime()).append("\n");
        sb.append("CPU Busy Time,").append(result.getBusyTime()).append("\n");
        sb.append("CPU Idle Time,").append(result.getIdleTime()).append("\n");
        sb.append("Context Switch Time,").append(result.getContextSwitchTime()).append("\n");
        sb.append("Context Switch Count,").append(result.getContextSwitchCount()).append("\n");
        sb.append("Average Waiting Time,").append(String.format(Locale.US, "%.2f", result.getAvgWaitingTime())).append("\n");
        sb.append("Average Turnaround Time,").append(String.format(Locale.US, "%.2f", result.getAvgTurnaroundTime())).append("\n");
        sb.append("Average Response Time,").append(String.format(Locale.US, "%.2f", result.getAvgResponseTime())).append("\n");
        sb.append("CPU Utilisation %,").append(String.format(Locale.US, "%.2f%%", result.getCpuUtilization())).append("\n");
        sb.append("Throughput,").append(String.format(Locale.US, "%.4f", result.getThroughput())).append("\n");
        sb.append("Jain's Fairness Index,").append(String.format(Locale.US, "%.4f", result.getJainsFairnessIndex())).append("\n");
        sb.append("Max Waiting Time,").append(result.getMaxWaitingTime()).append("\n");
        sb.append("Waiting Time StdDev,").append(String.format(Locale.US, "%.2f", result.getWaitingTimeStdDev())).append("\n");

        return sb.toString();
    }

    public static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
