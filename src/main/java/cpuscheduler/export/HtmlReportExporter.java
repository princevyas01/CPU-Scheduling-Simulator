package cpuscheduler.export;

import cpuscheduler.model.GanttSegment;
import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SchedulingEvent;
import cpuscheduler.model.SimulationResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Generates self-contained, offline-ready, print-friendly HTML reports
 * with embedded SVG Gantt charts, per-process metrics, decision trace highlights,
 * and comparative evaluation tables.
 */
public class HtmlReportExporter {

    public static void exportToFile(SimulationResult result, List<SimulationResult> comparisons, File file) throws IOException {
        String html = generateReport(result, comparisons);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.print(html);
        }
    }

    public static String generateReport(SimulationResult result, List<SimulationResult> comparisons) {
        StringBuilder sb = new StringBuilder();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n");
        sb.append("<title>CPU Scheduling Studio - Simulation Report</title>\n");
        sb.append("<style>\n");
        sb.append("  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #0a0d19; color: #ecf0fc; margin: 0; padding: 24px; line-height: 1.5; }\n");
        sb.append("  .container { max-width: 1140px; margin: 0 auto; }\n");
        sb.append("  h1, h2, h3 { color: #38cfdc; margin-top: 24px; }\n");
        sb.append("  .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #2b3450; padding-bottom: 16px; margin-bottom: 24px; }\n");
        sb.append("  .badge { background: #7c5cff; color: #fff; padding: 4px 12px; border-radius: 12px; font-size: 13px; font-weight: bold; }\n");
        sb.append("  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 14px; margin-bottom: 24px; }\n");
        sb.append("  .card { background: #151a2c; border: 1px solid #2b3450; border-radius: 10px; padding: 14px 18px; }\n");
        sb.append("  .card-title { font-size: 11px; text-transform: uppercase; color: #96a0bc; font-weight: bold; margin-bottom: 6px; }\n");
        sb.append("  .card-val { font-size: 22px; font-weight: bold; color: #ecf0fc; }\n");
        sb.append("  .card-val.cyan { color: #38cfdc; }\n");
        sb.append("  .card-val.amber { color: #ffb84d; }\n");
        sb.append("  .card-val.green { color: #4ade80; }\n");
        sb.append("  .card-val.blue { color: #6096ff; }\n");
        sb.append("  .card-val.purple { color: #c084fc; }\n");
        sb.append("  table { width: 100%; border-collapse: collapse; margin-top: 12px; margin-bottom: 24px; background: #151a2c; border-radius: 8px; overflow: hidden; border: 1px solid #2b3450; }\n");
        sb.append("  th { background: #1c233a; color: #38cfdc; text-align: center; padding: 10px 12px; font-size: 12px; border-bottom: 1px solid #2b3450; }\n");
        sb.append("  td { padding: 9px 12px; text-align: center; font-size: 13px; border-bottom: 1px solid #1c233a; color: #ecf0fc; }\n");
        sb.append("  tr:nth-child(even) { background: #1a2034; }\n");
        sb.append("  .timeline-container { background: #151a2c; border: 1px solid #2b3450; border-radius: 10px; padding: 18px; margin-bottom: 24px; overflow-x: auto; }\n");
        sb.append("  .interpretation { background: #1c233a; border-left: 4px solid #7c5cff; padding: 16px 20px; border-radius: 0 8px 8px 0; margin-bottom: 24px; }\n");
        sb.append("  @media print { body { background: #fff; color: #000; } .card, table, .timeline-container, .interpretation { background: #fff; border-color: #ccc; color: #000; } th { background: #f0f0f0; color: #000; } td { color: #000; } h1, h2, h3 { color: #000; } }\n");
        sb.append("</style>\n</head>\n<body>\n<div class=\"container\">\n");

        // Header
        sb.append("<div class=\"header\">\n");
        sb.append("  <div>\n");
        sb.append("    <h1 style=\"margin: 0;\">CPU Scheduling Studio</h1>\n");
        sb.append("    <div style=\"color: #96a0bc; font-size: 13px; margin-top: 4px;\">Simulation Report &amp; Performance Evaluation</div>\n");
        sb.append("  </div>\n");
        sb.append("  <div style=\"text-align: right;\">\n");
        sb.append("    <span class=\"badge\">").append(escape(result.getAlgorithmName())).append("</span>\n");
        sb.append("    <div style=\"color: #96a0bc; font-size: 12px; margin-top: 6px;\">Generated: ").append(dateStr).append("</div>\n");
        sb.append("  </div>\n");
        sb.append("</div>\n");

        // Summary KPI Cards
        sb.append("<h2>Performance Summary</h2>\n");
        sb.append("<div class=\"grid\">\n");
        sb.append("  <div class=\"card\"><div class=\"card-title\">Avg Waiting Time</div><div class=\"card-val amber\">")
                .append(String.format(Locale.US, "%.2f", result.getAvgWaitingTime())).append("</div></div>\n");
        sb.append("  <div class=\"card\"><div class=\"card-title\">Avg Turnaround</div><div class=\"card-val cyan\">")
                .append(String.format(Locale.US, "%.2f", result.getAvgTurnaroundTime())).append("</div></div>\n");
        sb.append("  <div class=\"card\"><div class=\"card-title\">Avg Response</div><div class=\"card-val green\">")
                .append(String.format(Locale.US, "%.2f", result.getAvgResponseTime())).append("</div></div>\n");
        sb.append("  <div class=\"card\"><div class=\"card-title\">CPU Utilisation</div><div class=\"card-val purple\">")
                .append(String.format(Locale.US, "%.1f%%", result.getCpuUtilization())).append("</div></div>\n");
        sb.append("  <div class=\"card\"><div class=\"card-title\">Context Switches</div><div class=\"card-val blue\">")
                .append(result.getContextSwitchCount()).append("</div></div>\n");
        sb.append("  <div class=\"card\"><div class=\"card-title\">Jain's Fairness</div><div class=\"card-val green\">")
                .append(String.format(Locale.US, "%.3f", result.getJainsFairnessIndex())).append("</div></div>\n");
        sb.append("</div>\n");

        // Gantt Chart (SVG)
        sb.append("<h2>Execution Timeline (Gantt Chart)</h2>\n");
        sb.append("<div class=\"timeline-container\">\n");
        sb.append(generateSvgGantt(result));
        sb.append("</div>\n");

        // Process Results Table
        sb.append("<h2>Process Execution Metrics</h2>\n");
        sb.append("<table>\n");
        sb.append("  <thead><tr><th>PID</th><th>Arrival</th><th>Burst</th><th>Configured Priority</th><th>Start Time</th><th>Completion Time</th><th>Turnaround (TAT)</th><th>Waiting (WT)</th><th>Response (RT)</th></tr></thead>\n");
        sb.append("  <tbody>\n");
        for (ProcessModel p : result.getProcesses()) {
            sb.append("    <tr>")
              .append("<td><strong>").append(escape(p.getId())).append("</strong></td>")
              .append("<td>").append(p.getArrivalTime()).append("</td>")
              .append("<td>").append(p.getBurstTime()).append("</td>")
              .append("<td>").append(p.getConfiguredPriority()).append("</td>")
              .append("<td>").append(p.getFirstStartTime()).append("</td>")
              .append("<td>").append(p.getCompletionTime()).append("</td>")
              .append("<td>").append(p.getTurnaroundTime()).append("</td>")
              .append("<td>").append(p.getWaitingTime()).append("</td>")
              .append("<td>").append(p.getResponseTime()).append("</td>")
              .append("</tr>\n");
        }
        sb.append("  </tbody>\n</table>\n");

        // Decision Log Highlights
        sb.append("<h2>Scheduling Decision Log Highlights</h2>\n");
        sb.append("<table>\n");
        sb.append("  <thead><tr><th>Time</th><th>Event</th><th>Process</th><th>Running</th><th>Ready Queue</th><th>Decision Reason</th><th>Values</th><th>Outcome</th></tr></thead>\n");
        sb.append("  <tbody>\n");
        int maxEvents = Math.min(30, result.getEvents().size());
        for (int i = 0; i < maxEvents; i++) {
            SchedulingEvent ev = result.getEvents().get(i);
            sb.append("    <tr>")
              .append("<td>t=").append(ev.getTime()).append("</td>")
              .append("<td>").append(escape(ev.getEventType().name())).append("</td>")
              .append("<td><strong>").append(escape(ev.getProcessId())).append("</strong></td>")
              .append("<td>").append(escape(ev.getRunningProcessId())).append("</td>")
              .append("<td>").append(escape(ev.getReadyProcesses())).append("</td>")
              .append("<td style=\"text-align: left;\">").append(escape(ev.getDecisionReason())).append("</td>")
              .append("<td>").append(escape(ev.getRelevantValues())).append("</td>")
              .append("<td>").append(escape(ev.getOutcome())).append("</td>")
              .append("</tr>\n");
        }
        if (result.getEvents().size() > maxEvents) {
            sb.append("    <tr><td colspan=\"8\" style=\"color: #96a0bc;\">... and ").append(result.getEvents().size() - maxEvents).append(" additional scheduling events.</td></tr>\n");
        }
        sb.append("  </tbody>\n</table>\n");

        // Comparison Table (if provided)
        if (comparisons != null && !comparisons.isEmpty()) {
            sb.append("<h2>Algorithm Comparison Benchmark</h2>\n");
            sb.append("<table>\n");
            sb.append("  <thead><tr><th>Algorithm</th><th>Avg WT</th><th>Avg TAT</th><th>Avg RT</th><th>CPU Util %</th><th>Context Switches</th><th>Total Time</th><th>Jain's Fairness</th></tr></thead>\n");
            sb.append("  <tbody>\n");
            for (SimulationResult cmp : comparisons) {
                boolean isCurrent = cmp.getAlgorithmName().equals(result.getAlgorithmName());
                sb.append("    <tr").append(isCurrent ? " style=\"background: #252e50; font-weight: bold;\"" : "").append(">")
                  .append("<td style=\"text-align: left;\">").append(escape(cmp.getAlgorithmName())).append("</td>")
                  .append("<td>").append(String.format(Locale.US, "%.2f", cmp.getAvgWaitingTime())).append("</td>")
                  .append("<td>").append(String.format(Locale.US, "%.2f", cmp.getAvgTurnaroundTime())).append("</td>")
                  .append("<td>").append(String.format(Locale.US, "%.2f", cmp.getAvgResponseTime())).append("</td>")
                  .append("<td>").append(String.format(Locale.US, "%.1f%%", cmp.getCpuUtilization())).append("</td>")
                  .append("<td>").append(cmp.getContextSwitchCount()).append("</td>")
                  .append("<td>").append(cmp.getTotalTime()).append("</td>")
                  .append("<td>").append(String.format(Locale.US, "%.3f", cmp.getJainsFairnessIndex())).append("</td>")
                  .append("</tr>\n");
            }
            sb.append("  </tbody>\n</table>\n");
        }

        // Data-driven Interpretation
        sb.append("<h2>Experimental Analysis &amp; Interpretation</h2>\n");
        sb.append("<div class=\"interpretation\">\n");
        sb.append("  <p><strong>Workload Characteristics:</strong> ").append(result.getProcesses().size())
          .append(" processes with total CPU execution requirement of ").append(result.getBusyTime()).append(" time units.</p>\n");
        sb.append("  <p><strong>Context Switch Overhead:</strong> Configured at ").append(result.getConfig().getContextSwitchOverhead())
          .append(" time units per process-to-process switch, accounting for ").append(result.getContextSwitchTime())
          .append(" units (").append(String.format(Locale.US, "%.1f%%", result.getContextSwitchPercentage())).append(" of total elapsed time).</p>\n");
        if (result.getConfig().isAgingEnabled()) {
            sb.append("  <p><strong>Priority Aging:</strong> Enabled (interval: ").append(result.getConfig().getAgingInterval())
              .append(", boost: ").append(result.getConfig().getPriorityBoost())
              .append(", minimum effective priority: ").append(result.getConfig().getMinPriority()).append(").</p>\n");
        }
        sb.append("  <p><strong>Fairness Analysis:</strong> Jain's Fairness Index is calculated at ")
          .append(String.format(Locale.US, "%.3f", result.getJainsFairnessIndex()))
          .append(" (1.000 indicates absolute equity of normalized turnaround time across all processes).</p>\n");
        sb.append("</div>\n");

        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    private static String generateSvgGantt(SimulationResult result) {
        int total = Math.max(1, result.getTotalTime());
        int width = Math.max(700, total * 32);
        int height = 110;
        int barY = 30;
        int barHeight = 44;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg width=\"").append(width).append("\" height=\"").append(height).append("\" xmlns=\"http://www.w3.org/2000/svg\">\n");

        String[] colors = {
            "#7c5cff", "#22b8cf", "#ff8a5c", "#4ac882", "#ec62aa", "#f0b93c",
            "#5a96ff", "#be78ff", "#ff6e6e", "#3cd2b4", "#aac846", "#ffa0c8"
        };

        for (GanttSegment seg : result.getSegments()) {
            double x = (double) seg.getStartTime() / total * (width - 40) + 20;
            double w = (double) seg.getDuration() / total * (width - 40);
            String fill = seg.isIdle() ? "#40465e" : seg.isContextSwitch() ? "#e056fd" : colors[Math.abs(seg.getProcessIndex()) % colors.length];

            svg.append("  <rect x=\"").append(String.format(Locale.US, "%.1f", x))
               .append("\" y=\"").append(barY)
               .append("\" width=\"").append(String.format(Locale.US, "%.1f", Math.max(2.0, w)))
               .append("\" height=\"").append(barHeight)
               .append("\" rx=\"5\" ry=\"5\" fill=\"").append(fill)
               .append("\" stroke=\"#2b3450\" stroke-width=\"1\" />\n");

            if (w > 20) {
                String label = seg.isIdle() ? "idle" : seg.isContextSwitch() ? "CS" : escape(seg.getProcessId());
                double textX = x + w / 2.0;
                double textY = barY + barHeight / 2.0 + 5;
                svg.append("  <text x=\"").append(String.format(Locale.US, "%.1f", textX))
                   .append("\" y=\"").append(String.format(Locale.US, "%.1f", textY))
                   .append("\" font-size=\"12\" font-weight=\"bold\" fill=\"#ffffff\" text-anchor=\"middle\">")
                   .append(label).append("</text>\n");
            }

            // Ticks
            double tickY = barY + barHeight + 18;
            svg.append("  <text x=\"").append(String.format(Locale.US, "%.1f", x))
               .append("\" y=\"").append(String.format(Locale.US, "%.1f", tickY))
               .append("\" font-size=\"11\" fill=\"#96a0bc\" text-anchor=\"middle\">")
               .append(seg.getStartTime()).append("</text>\n");
        }

        double endX = (width - 40) + 20;
        double tickY = barY + barHeight + 18;
        svg.append("  <text x=\"").append(String.format(Locale.US, "%.1f", endX))
           .append("\" y=\"").append(String.format(Locale.US, "%.1f", tickY))
           .append("\" font-size=\"11\" fill=\"#96a0bc\" text-anchor=\"middle\">")
           .append(total).append("</text>\n");

        svg.append("</svg>\n");
        return svg.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
