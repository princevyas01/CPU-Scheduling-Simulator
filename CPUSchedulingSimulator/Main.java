/**
 * CPU Scheduling Studio
 * ---------------------------------------------------------------
 * BTech Computer Engineering Mini Project
 *
 * Modular Architecture:
 *   - cpuscheduler.model        : ProcessModel, SimulationResult, SchedulingEvent, GanttSegment
 *   - cpuscheduler.scheduler    : Scheduler interface and all 10 algorithm implementations
 *   - cpuscheduler.simulation   : SimulationEngine, context-switch overhead, priority aging
 *   - cpuscheduler.experiment   : WorkloadGenerator, WorkloadPreset, ExperimentLab
 *   - cpuscheduler.export       : CsvImporter, CsvExporter, HtmlReportExporter
 *   - cpuscheduler.ui           : Swing dark-theme GUI, GanttPanel, DecisionLogPanel, ExperimentPanel, ComparisonPanel
 *
 * To run the application, use:
 *   run.bat
 * or:
 *   java -cp target/classes cpuscheduler.Main
 */
public class Main {
    public static void main(String[] args) {
        cpuscheduler.ui.MainFrame.main(args);
    }
}