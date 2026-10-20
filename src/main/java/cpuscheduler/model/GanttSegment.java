package cpuscheduler.model;

/**
 * Represents a contiguous block of time in the execution timeline.
 * Type:
 *  - PROCESS: A specific process is executing (processIndex >= 0)
 *  - IDLE: The CPU is idle (processIndex = -1)
 *  - CONTEXT_SWITCH: Context-switch overhead interval (processIndex = -2)
 */
public class GanttSegment {
    public enum Type {
        PROCESS,
        IDLE,
        CONTEXT_SWITCH
    }

    private final Type type;
    private final int processIndex;
    private final String processId;
    private final int startTime;
    private final int endTime;

    public GanttSegment(Type type, int processIndex, String processId, int startTime, int endTime) {
        this.type = type;
        this.processIndex = processIndex;
        this.processId = processId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static GanttSegment process(int processIndex, String processId, int start, int end) {
        return new GanttSegment(Type.PROCESS, processIndex, processId, start, end);
    }

    public static GanttSegment idle(int start, int end) {
        return new GanttSegment(Type.IDLE, -1, "IDLE", start, end);
    }

    public static GanttSegment contextSwitch(int start, int end) {
        return new GanttSegment(Type.CONTEXT_SWITCH, -2, "CS", start, end);
    }

    public Type getType() { return type; }
    public int getProcessIndex() { return processIndex; }
    public String getProcessId() { return processId; }
    public int getStartTime() { return startTime; }
    public int getEndTime() { return endTime; }
    public int getDuration() { return endTime - startTime; }

    public boolean isIdle() { return type == Type.IDLE; }
    public boolean isContextSwitch() { return type == Type.CONTEXT_SWITCH; }
    public boolean isProcess() { return type == Type.PROCESS; }

    @Override
    public String toString() {
        return String.format("[%s: %d -> %d (%s)]", type, startTime, endTime, processId);
    }
}
