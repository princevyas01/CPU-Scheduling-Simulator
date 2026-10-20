package cpuscheduler.simulation;

import cpuscheduler.model.*;
import cpuscheduler.scheduler.Scheduler;
import cpuscheduler.scheduler.SchedulerFactory;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

import java.util.*;

/**
 * Unified simulation engine for all CPU scheduling algorithms.
 * Centralizes lifecycle management, timeline generation, context-switch overhead,
 * priority aging, mathematical invariant verification, and event tracing.
 */
public class SimulationEngine {
    public static final int MAX_TIMELINE_TICKS = 50000;

    /**
     * Executes a full simulation of the given workload under the specified algorithm and configuration.
     */
    public static SimulationResult simulate(List<ProcessModel> workload, SchedulingAlgorithmType algorithmType, SimulationConfig config) {
        if (workload == null || workload.isEmpty()) {
            throw new IllegalArgumentException("Workload cannot be empty.");
        }
        if (config == null) {
            config = SimulationConfig.defaultConfig();
        }

        // 1. Independent deep-copy of workload
        int n = workload.size();
        List<ProcessModel> processes = new ArrayList<>(n);
        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i < n; i++) {
            ProcessModel orig = workload.get(i);
            if (!seenIds.add(orig.getId())) {
                throw new IllegalArgumentException("Duplicate process ID detected: " + orig.getId());
            }
            ProcessModel copy = new ProcessModel(orig.getId(), orig.getArrivalTime(), orig.getBurstTime(), orig.getConfiguredPriority(), i);
            processes.add(copy);
        }

        // Sort by arrival time, then order for arrival check
        List<ProcessModel> arrivalList = new ArrayList<>(processes);
        arrivalList.sort((a, b) -> {
            if (a.getArrivalTime() != b.getArrivalTime()) {
                return Integer.compare(a.getArrivalTime(), b.getArrivalTime());
            }
            return Integer.compare(a.getOrder(), b.getOrder());
        });

        // 2. Initialize scheduler
        Scheduler scheduler = SchedulerFactory.create(algorithmType);
        scheduler.reset();

        // 3. Simulation state tracking
        List<Integer> timeline = new ArrayList<>();
        List<int[]> queueSnapshots = new ArrayList<>();
        List<SchedulingEvent> eventTrace = new ArrayList<>();

        boolean[] admitted = new boolean[n];
        int completedCount = 0;
        int t = 0;
        ProcessModel running = null;
        ProcessModel lastExecuted = null; // To track direct process transitions

        // Context switch overhead tracking
        int csRemaining = 0;
        ProcessModel csTargetProcess = null;
        int totalContextSwitches = 0;
        int totalContextSwitchTime = 0;
        int totalIdleTime = 0;
        int totalBusyTime = 0;

        int csOverhead = config.getContextSwitchOverhead();
        boolean agingActive = config.isAgingEnabled() && algorithmType.supportsAging();

        // Map for fast index lookup
        Map<ProcessModel, Integer> processIndexMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            processIndexMap.put(processes.get(i), i);
        }

        while (completedCount < n) {
            if (t > MAX_TIMELINE_TICKS) {
                throw new IllegalStateException("Simulation exceeded maximum allowed timeline limit (" + MAX_TIMELINE_TICKS + " units). " +
                        "Please adjust arrival or burst time values.");
            }

            // A) Check and admit new arrivals at current time t
            for (ProcessModel p : arrivalList) {
                int idx = processIndexMap.get(p);
                if (!admitted[idx] && p.getArrivalTime() <= t) {
                    admitted[idx] = true;
                    scheduler.onProcessArrived(p, t);
                    eventTrace.add(new SchedulingEvent(
                            t,
                            SchedulingEvent.EventType.PROCESS_ARRIVED,
                            p.getId(),
                            running != null ? running.getId() : "None",
                            formatQueueSnapshot(scheduler.getReadyQueueSnapshot()),
                            "Process " + p.getId() + " arrived and placed in ready queue.",
                            "AT=" + p.getArrivalTime() + ", BT=" + p.getBurstTime() + ", Priority=" + p.getConfiguredPriority(),
                            "Enqueued"
                    ));
                }
            }

            // B) Priority Aging: waiting processes in ready queue
            if (agingActive) {
                for (ProcessModel readyP : scheduler.getReadyQueueSnapshot()) {
                    if (readyP != running) {
                        readyP.incrementReadyWaitTime();
                        if (readyP.getReadyWaitTime() > 0 && readyP.getReadyWaitTime() % config.getAgingInterval() == 0) {
                            int oldPr = readyP.getEffectivePriority();
                            readyP.agePriority(config.getPriorityBoost(), config.getMinPriority());
                            if (readyP.getEffectivePriority() < oldPr) {
                                eventTrace.add(new SchedulingEvent(
                                        t,
                                        SchedulingEvent.EventType.PRIORITY_AGED,
                                        readyP.getId(),
                                        running != null ? running.getId() : "None",
                                        formatQueueSnapshot(scheduler.getReadyQueueSnapshot()),
                                        "Process " + readyP.getId() + " priority boosted from " + oldPr + " to " + readyP.getEffectivePriority() + " due to aging.",
                                        "Wait Time = " + readyP.getReadyWaitTime() + ", Interval = " + config.getAgingInterval(),
                                        "Priority Aged"
                                ));
                            }
                        }
                    }
                }
            }

            // B.2) Check Quantum Expiration (after admitting arrivals at t, before selecting next process)
            if (running != null && running.getRemainingBurst() > 0 && running.getRemainingQuantum() == 0 &&
                    (algorithmType.usesQuantum() || algorithmType == SchedulingAlgorithmType.MLFQ)) {
                eventTrace.add(new SchedulingEvent(
                        t,
                        SchedulingEvent.EventType.QUANTUM_EXPIRED,
                        running.getId(),
                        running.getId(),
                        formatQueueSnapshot(scheduler.getReadyQueueSnapshot()),
                        "Process " + running.getId() + " exhausted its time quantum allotment at t=" + t + ".",
                        "Remaining Burst = " + running.getRemainingBurst(),
                        "Quantum Expired"
                ));

                scheduler.onQuantumExpired(running, t, config);
                running = null;
            }

            // C) If currently performing context switch overhead:
            if (csRemaining > 0) {
                timeline.add(-2); // -2 represents CONTEXT_SWITCH
                queueSnapshots.add(toSnapshotArray(scheduler.getReadyQueueSnapshot(), processIndexMap));
                totalContextSwitchTime++;
                csRemaining--;
                t++;

                if (csRemaining == 0) {
                    // Context switch interval completed; dispatch target process
                    running = csTargetProcess;
                    lastExecuted = running;
                    csTargetProcess = null;

                    eventTrace.add(new SchedulingEvent(
                            t,
                            SchedulingEvent.EventType.CONTEXT_SWITCH_COMPLETED,
                            running.getId(),
                            running.getId(),
                            formatQueueSnapshot(scheduler.getReadyQueueSnapshot()),
                            "Context switch overhead finished. Process " + running.getId() + " takes CPU.",
                            "Overhead = " + csOverhead + " units",
                            "Dispatched"
                    ));
                }
                continue;
            }

            // D) Make scheduling decision
            SchedulingDecision decision = scheduler.selectProcess(t, running, config);

            // Handle preemption
            if (decision.isShouldPreempt()) {
                ProcessModel preempted = running;
                ProcessModel newcomer = decision.getSelectedProcess();

                eventTrace.add(new SchedulingEvent(
                        t,
                        SchedulingEvent.EventType.PROCESS_PREEMPTED,
                        preempted.getId(),
                        preempted.getId(),
                        formatQueueSnapshot(scheduler.getReadyQueueSnapshot()),
                        decision.getDecisionReason(),
                        decision.getRelevantValues(),
                        decision.getOutcome()
                ));

                scheduler.onProcessPreempted(preempted, t);
                running = null;

                // Check context switch overhead from preempted to newcomer
                if (csOverhead > 0 && preempted != newcomer) {
                    totalContextSwitches++;
                    csRemaining = csOverhead;
                    csTargetProcess = newcomer;

                    eventTrace.add(new SchedulingEvent(
                            t,
                            SchedulingEvent.EventType.CONTEXT_SWITCH_STARTED,
                            preempted.getId() + " -> " + newcomer.getId(),
                            "CS",
                            formatQueueSnapshot(scheduler.getReadyQueueSnapshot()),
                            "Context switch initiated between " + preempted.getId() + " and " + newcomer.getId() + ".",
                            "Overhead = " + csOverhead + " units",
                            "Switching"
                    ));
                    continue; // Loop re-enters at C) to simulate the switch interval
                } else {
                    if (preempted != newcomer) totalContextSwitches++;
                    running = newcomer;
                    lastExecuted = running;
                }
            } else if (running == null && decision.getSelectedProcess() != null) {
                // Dispatching when CPU was idle or after previous process finished
                ProcessModel nextProc = decision.getSelectedProcess();

                // If previous was a different process without idle in between, charge context switch
                if (csOverhead > 0 && lastExecuted != null && lastExecuted != nextProc) {
                    totalContextSwitches++;
                    csRemaining = csOverhead;
                    csTargetProcess = nextProc;

                    eventTrace.add(new SchedulingEvent(
                            t,
                            SchedulingEvent.EventType.CONTEXT_SWITCH_STARTED,
                            lastExecuted.getId() + " -> " + nextProc.getId(),
                            "CS",
                            formatQueueSnapshot(scheduler.getReadyQueueSnapshot()),
                            "Context switch initiated between " + lastExecuted.getId() + " and " + nextProc.getId() + ".",
                            "Overhead = " + csOverhead + " units",
                            "Switching"
                    ));
                    continue;
                } else {
                    if (lastExecuted != null && lastExecuted != nextProc) {
                        totalContextSwitches++;
                    }
                    running = nextProc;
                    lastExecuted = running;

                    eventTrace.add(new SchedulingEvent(
                            t,
                            SchedulingEvent.EventType.PROCESS_DISPATCHED,
                            running.getId(),
                            running.getId(),
                            formatQueueSnapshot(scheduler.getReadyQueueSnapshot()),
                            decision.getDecisionReason(),
                            decision.getRelevantValues(),
                            decision.getOutcome()
                    ));
                }
            } else if (running != null && decision.getSelectedProcess() == running) {
                // Continue running
            } else if (decision.getSelectedProcess() == null) {
                // CPU is Idle
                running = null;
                lastExecuted = null; // Intervening idle resets process-to-process transition!

                timeline.add(-1); // -1 represents IDLE
                queueSnapshots.add(toSnapshotArray(scheduler.getReadyQueueSnapshot(), processIndexMap));
                totalIdleTime++;

                eventTrace.add(new SchedulingEvent(
                        t,
                        SchedulingEvent.EventType.CPU_IDLE,
                        "-",
                        "IDLE",
                        "[]",
                        decision.getDecisionReason(),
                        "No ready processes",
                        "Idle"
                ));
                t++;
                continue;
            }

            // E) Execute 1 time unit of running process
            if (running != null) {
                if (running.getFirstStartTime() < 0) {
                    running.setFirstStartTime(t);
                }

                timeline.add(processIndexMap.get(running));
                queueSnapshots.add(toSnapshotArray(scheduler.getReadyQueueSnapshot(), processIndexMap));
                totalBusyTime++;

                running.decrementRemainingBurst();
                running.decrementRemainingQuantum();
                t++;

                // Check completion
                if (running.getRemainingBurst() == 0) {
                    running.setCompletionTime(t);
                    completedCount++;

                    eventTrace.add(new SchedulingEvent(
                            t,
                            SchedulingEvent.EventType.PROCESS_COMPLETED,
                            running.getId(),
                            running.getId(),
                            formatQueueSnapshot(scheduler.getReadyQueueSnapshot()),
                            "Process " + running.getId() + " completed its CPU burst at t=" + t + ".",
                            "CT=" + t + ", TAT=" + running.getTurnaroundTime() + ", WT=" + running.getWaitingTime() + ", RT=" + running.getResponseTime(),
                            "Completed"
                    ));

                    scheduler.onProcessCompleted(running, t);
                    // running is now finished, next step may pick new process
                    running = null;
                }
                // Note: If running.getRemainingQuantum() == 0, it will be handled at the start of next tick
                // after admitting any new arrivals that arrive at time t!
            }
        }

        // 4. Construct Gantt segments from timeline
        List<GanttSegment> segments = buildGanttSegments(timeline, processes);

        // 5. Invariant verification (Phase 1, Section 4.3)
        verifyInvariants(processes, segments, totalBusyTime, totalIdleTime, totalContextSwitchTime, t, algorithmType);

        // 6. Return immutable SimulationResult
        int[] tlArray = new int[timeline.size()];
        for (int i = 0; i < tlArray.length; i++) tlArray[i] = timeline.get(i);

        int[][] snapArray = queueSnapshots.toArray(new int[0][]);

        return new SimulationResult(
                algorithmType.getDisplayName(),
                config,
                processes,
                segments,
                tlArray,
                snapArray,
                eventTrace,
                t,
                totalBusyTime,
                totalIdleTime,
                totalContextSwitchTime,
                totalContextSwitches
        );
    }

    private static List<GanttSegment> buildGanttSegments(List<Integer> timeline, List<ProcessModel> processes) {
        List<GanttSegment> segments = new ArrayList<>();
        if (timeline.isEmpty()) return segments;

        int start = 0;
        int currentCode = timeline.get(0);

        for (int i = 1; i <= timeline.size(); i++) {
            if (i == timeline.size() || timeline.get(i) != currentCode) {
                if (currentCode == -1) {
                    segments.add(GanttSegment.idle(start, i));
                } else if (currentCode == -2) {
                    segments.add(GanttSegment.contextSwitch(start, i));
                } else if (currentCode >= 0 && currentCode < processes.size()) {
                    segments.add(GanttSegment.process(currentCode, processes.get(currentCode).getId(), start, i));
                }
                if (i < timeline.size()) {
                    start = i;
                    currentCode = timeline.get(i);
                }
            }
        }
        return segments;
    }

    private static int[] toSnapshotArray(List<ProcessModel> queue, Map<ProcessModel, Integer> indexMap) {
        int[] arr = new int[queue.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = indexMap.getOrDefault(queue.get(i), -1);
        }
        return arr;
    }

    private static String formatQueueSnapshot(List<ProcessModel> queue) {
        if (queue.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < queue.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(queue.get(i).getId());
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Strictly verifies all mathematical invariants specified in Phase 1 Section 4.3.
     */
    private static void verifyInvariants(List<ProcessModel> processes,
                                         List<GanttSegment> segments,
                                         int totalBusy,
                                         int totalIdle,
                                         int totalCS,
                                         int totalTime,
                                         SchedulingAlgorithmType algo) {
        // Invariant: Total elapsed time must equal busy + idle + context switch
        if (totalTime != totalBusy + totalIdle + totalCS) {
            throw new IllegalStateException("Invariant violated: totalTime (" + totalTime +
                    ") != busy (" + totalBusy + ") + idle (" + totalIdle + ") + CS (" + totalCS + ").");
        }

        int sumBursts = 0;
        for (ProcessModel p : processes) {
            sumBursts += p.getBurstTime();

            // Invariant: Process must be marked completed
            if (!p.isCompleted()) {
                throw new IllegalStateException("Invariant violated: Process " + p.getId() + " did not complete.");
            }

            // Invariant: Remaining burst must be zero
            if (p.getRemainingBurst() != 0) {
                throw new IllegalStateException("Invariant violated: Process " + p.getId() + " remaining burst is " + p.getRemainingBurst() + " != 0.");
            }

            // Invariant: Completion time >= Arrival time
            if (p.getCompletionTime() < p.getArrivalTime()) {
                throw new IllegalStateException("Invariant violated: Completion time " + p.getCompletionTime() + " < arrival time " + p.getArrivalTime());
            }

            // Invariant: First start time >= Arrival time
            if (p.getFirstStartTime() < p.getArrivalTime()) {
                throw new IllegalStateException("Invariant violated: First start time " + p.getFirstStartTime() + " < arrival time " + p.getArrivalTime());
            }

            // Invariant: Turnaround Time = Completion Time - Arrival Time
            int expectedTAT = p.getCompletionTime() - p.getArrivalTime();
            if (p.getTurnaroundTime() != expectedTAT) {
                throw new IllegalStateException("Invariant violated: Turnaround Time " + p.getTurnaroundTime() + " != " + expectedTAT);
            }

            // Invariant: Waiting Time = Turnaround Time - Burst Time
            int expectedWT = p.getTurnaroundTime() - p.getBurstTime();
            if (p.getWaitingTime() != expectedWT) {
                throw new IllegalStateException("Invariant violated: Waiting Time " + p.getWaitingTime() + " != " + expectedWT);
            }

            // Invariant: Response Time = First Start Time - Arrival Time
            int expectedRT = p.getFirstStartTime() - p.getArrivalTime();
            if (p.getResponseTime() != expectedRT) {
                throw new IllegalStateException("Invariant violated: Response Time " + p.getResponseTime() + " != " + expectedRT);
            }

            if (p.getWaitingTime() < 0) {
                throw new IllegalStateException("Invariant violated: Waiting Time is negative (" + p.getWaitingTime() + ") for " + p.getId());
            }
            if (p.getResponseTime() < 0) {
                throw new IllegalStateException("Invariant violated: Response Time is negative (" + p.getResponseTime() + ") for " + p.getId());
            }
        }

        // Invariant: Total executed CPU time must equal sum of all burst times
        if (totalBusy != sumBursts) {
            throw new IllegalStateException("Invariant violated: Total busy time " + totalBusy + " != sum of bursts " + sumBursts);
        }

        // Invariant: Non-preemptive policies must not preempt a running process
        if (!algo.isPreemptive() && algo != SchedulingAlgorithmType.ROUND_ROBIN && algo != SchedulingAlgorithmType.MLFQ) {
            Map<String, Integer> segmentCountByProcess = new HashMap<>();
            for (GanttSegment seg : segments) {
                if (seg.isProcess()) {
                    segmentCountByProcess.put(seg.getProcessId(), segmentCountByProcess.getOrDefault(seg.getProcessId(), 0) + 1);
                }
            }
            for (Map.Entry<String, Integer> entry : segmentCountByProcess.entrySet()) {
                if (entry.getValue() > 1) {
                    throw new IllegalStateException("Invariant violated: Non-preemptive algorithm " + algo.getDisplayName() +
                            " split process " + entry.getKey() + " into " + entry.getValue() + " segments.");
                }
            }
        }
    }
}
