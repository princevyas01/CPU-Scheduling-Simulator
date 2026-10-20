package cpuscheduler.scheduler;

import cpuscheduler.model.ProcessModel;

/**
 * Result of a scheduler's decision at a given scheduling point.
 */
public class SchedulingDecision {
    private final ProcessModel selectedProcess;
    private final boolean shouldPreempt;
    private final String decisionReason;
    private final String relevantValues;
    private final String outcome;

    public SchedulingDecision(ProcessModel selectedProcess,
                              boolean shouldPreempt,
                              String decisionReason,
                              String relevantValues,
                              String outcome) {
        this.selectedProcess = selectedProcess;
        this.shouldPreempt = shouldPreempt;
        this.decisionReason = decisionReason;
        this.relevantValues = relevantValues;
        this.outcome = outcome;
    }

    public static SchedulingDecision idle(String reason) {
        return new SchedulingDecision(null, false, reason, "CPU idle", "Idle");
    }

    public static SchedulingDecision continueRunning(ProcessModel running, String reason, String values) {
        return new SchedulingDecision(running, false, reason, values, "Continues Execution");
    }

    public static SchedulingDecision dispatch(ProcessModel proc, String reason, String values) {
        return new SchedulingDecision(proc, false, reason, values, "Dispatched");
    }

    public static SchedulingDecision preempt(ProcessModel newProc, String reason, String values) {
        return new SchedulingDecision(newProc, true, reason, values, "Preempts Running Process");
    }

    public ProcessModel getSelectedProcess() { return selectedProcess; }
    public boolean isShouldPreempt() { return shouldPreempt; }
    public String getDecisionReason() { return decisionReason; }
    public String getRelevantValues() { return relevantValues; }
    public String getOutcome() { return outcome; }
}
