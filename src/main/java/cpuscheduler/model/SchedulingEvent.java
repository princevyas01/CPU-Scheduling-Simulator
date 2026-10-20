package cpuscheduler.model;

/**
 * Structured scheduling event representing a scheduling decision point.
 * Serves as the single source of truth for:
 *  - Decision Log table
 *  - Timeline synchronization & Gantt playback
 *  - Educational explanations
 *  - HTML and CSV reports
 */
public class SchedulingEvent {
    public enum EventType {
        PROCESS_ARRIVED,
        PROCESS_ENQUEUED,
        PROCESS_DISPATCHED,
        PROCESS_PREEMPTED,
        QUANTUM_EXPIRED,
        PROCESS_DEMOTED,
        PRIORITY_AGED,
        CONTEXT_SWITCH_STARTED,
        CONTEXT_SWITCH_COMPLETED,
        PROCESS_COMPLETED,
        CPU_IDLE
    }

    private final int time;
    private final EventType eventType;
    private final String processId;
    private final String runningProcessId;
    private final String readyProcesses;
    private final String decisionReason;
    private final String relevantValues;
    private final String outcome;

    public SchedulingEvent(int time,
                           EventType eventType,
                           String processId,
                           String runningProcessId,
                           String readyProcesses,
                           String decisionReason,
                           String relevantValues,
                           String outcome) {
        this.time = time;
        this.eventType = eventType;
        this.processId = processId != null ? processId : "-";
        this.runningProcessId = runningProcessId != null ? runningProcessId : "-";
        this.readyProcesses = readyProcesses != null ? readyProcesses : "[]";
        this.decisionReason = decisionReason != null ? decisionReason : "";
        this.relevantValues = relevantValues != null ? relevantValues : "";
        this.outcome = outcome != null ? outcome : "";
    }

    public int getTime() { return time; }
    public EventType getEventType() { return eventType; }
    public String getProcessId() { return processId; }
    public String getRunningProcessId() { return runningProcessId; }
    public String getReadyProcesses() { return readyProcesses; }
    public String getDecisionReason() { return decisionReason; }
    public String getRelevantValues() { return relevantValues; }
    public String getOutcome() { return outcome; }

    @Override
    public String toString() {
        return String.format("t=%d [%s] Proc: %s, Running: %s | Ready: %s | %s | %s -> %s",
                time, eventType, processId, runningProcessId, readyProcesses, decisionReason, relevantValues, outcome);
    }
}
