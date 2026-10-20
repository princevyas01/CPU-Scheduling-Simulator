package cpuscheduler.scheduler.algorithms;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.scheduler.SchedulingDecision;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Multilevel Feedback Queue (MLFQ) Scheduling:
 * 3 Queues:
 *   Queue 0: Round Robin with quantum Q
 *   Queue 1: Round Robin with quantum 2*Q
 *   Queue 2: First Come First Served (FCFS)
 *
 * Rules:
 *   1. New arrivals enter Queue 0 with quantum Q.
 *   2. Higher-priority queues strictly preempt lower-priority queues.
 *   3. If preempted by a higher-priority queue, a process retains its remaining quantum allotment.
 *   4. If a process exhausts its quantum allotment at Queue k, it is demoted to Queue k+1.
 *   5. Queue 2 processes execute to completion unless preempted by new arrivals in Q0 or Q1.
 */
public class MLFQScheduler extends BaseScheduler {
    public static final int NUM_LEVELS = 3;
    private final List<ArrayDeque<ProcessModel>> queues = new ArrayList<>();

    public MLFQScheduler() {
        super(SchedulingAlgorithmType.MLFQ);
        for (int i = 0; i < NUM_LEVELS; i++) {
            queues.add(new ArrayDeque<>());
        }
    }

    public static int getQuantumForLevel(int level, int baseQuantum) {
        if (level == 0) return baseQuantum;
        if (level == 1) return baseQuantum * 2;
        return Integer.MAX_VALUE; // Queue 2 is FCFS
    }

    @Override
    public void onProcessArrived(ProcessModel process, int currentTime) {
        process.setCurrentQueueLevel(0);
        // Brand new process starts with fresh quantum of queue 0
        process.setRemainingQuantum(0); // Will be set upon dispatch
        queues.get(0).addLast(process);
    }

    @Override
    public SchedulingDecision selectProcess(int currentTime, ProcessModel running, SimulationConfig config) {
        int baseQ = config.getQuantum();

        // 1. Check if higher queue preemption must occur
        if (running != null && running.getRemainingBurst() > 0) {
            int currentLvl = running.getCurrentQueueLevel();
            boolean higherQueueHasWork = false;
            int higherLvlFound = -1;
            for (int l = 0; l < currentLvl; l++) {
                if (!queues.get(l).isEmpty()) {
                    higherQueueHasWork = true;
                    higherLvlFound = l;
                    break;
                }
            }

            if (higherQueueHasWork) {
                // Higher queue has work: preempt current process!
                // CRITICAL MLFQ REQUIREMENT: Preempted process RETAINS its remaining quantum at its current level!
                queues.get(currentLvl).addLast(running);

                ProcessModel higherProc = queues.get(higherLvlFound).pollFirst();
                if (higherProc.getRemainingQuantum() <= 0) {
                    higherProc.setRemainingQuantum(getQuantumForLevel(higherLvlFound, baseQ));
                }

                return SchedulingDecision.preempt(
                        higherProc,
                        "Process " + higherProc.getId() + " from higher Queue " + higherLvlFound
                                + " preempts running " + running.getId() + " (Queue " + currentLvl + "). "
                                + running.getId() + " retains remaining quantum allotment (" + running.getRemainingQuantum() + " units).",
                        "Preempted to Queue " + currentLvl + " (saved remQ=" + running.getRemainingQuantum() + ")"
                );
            }

            // If no higher queue work, can current process continue running?
            if (running.getRemainingQuantum() > 0) {
                return SchedulingDecision.continueRunning(
                        running,
                        "Process " + running.getId() + " continues execution in Queue " + currentLvl
                                + " (remaining quantum: " + (running.getRemainingQuantum() == Integer.MAX_VALUE ? "FCFS" : running.getRemainingQuantum()) + ").",
                        "Queue " + currentLvl + ", remQ=" + running.getRemainingQuantum() + ", remBT=" + running.getRemainingBurst()
                );
            }
        }

        // 2. Select next process from highest non-empty queue
        for (int l = 0; l < NUM_LEVELS; l++) {
            if (!queues.get(l).isEmpty()) {
                ProcessModel next = queues.get(l).pollFirst();
                // If it was resuming from preemption, it keeps its remaining quantum;
                // if it's new to this level, set fresh quantum for this level
                if (next.getRemainingQuantum() <= 0) {
                    next.setRemainingQuantum(getQuantumForLevel(l, baseQ));
                }

                String qDesc = l == 2 ? "FCFS" : "Q=" + next.getRemainingQuantum();
                return SchedulingDecision.dispatch(
                        next,
                        "Process " + next.getId() + " dispatched from Queue " + l + " with allotment " + qDesc + ".",
                        "Queue " + l + ", Quantum Allotment = " + qDesc + ", Burst = " + next.getRemainingBurst()
                );
            }
        }

        // If CPU was running and ready queues are empty, but running has remaining burst:
        if (running != null && running.getRemainingBurst() > 0) {
            int l = running.getCurrentQueueLevel();
            if (running.getRemainingQuantum() <= 0) {
                running.setRemainingQuantum(getQuantumForLevel(l, baseQ));
            }
            return SchedulingDecision.continueRunning(
                    running,
                    "Process " + running.getId() + " continues as sole active process in Queue " + l + ".",
                    "Queue " + l + ", remQ=" + running.getRemainingQuantum()
            );
        }

        return SchedulingDecision.idle("All MLFQ queues empty; CPU is idle.");
    }

    @Override
    public void onQuantumExpired(ProcessModel running, int currentTime, SimulationConfig config) {
        if (running != null && running.getRemainingBurst() > 0) {
            int curLvl = running.getCurrentQueueLevel();
            int nextLvl = Math.min(curLvl + 1, NUM_LEVELS - 1);
            running.setCurrentQueueLevel(nextLvl);
            // Demoted process gets fresh quantum when it reaches next level
            running.setRemainingQuantum(getQuantumForLevel(nextLvl, config.getQuantum()));
            queues.get(nextLvl).addLast(running);
        }
    }

    @Override
    public void onProcessPreempted(ProcessModel running, int currentTime) {
        // Preempted process retains its level and remaining quantum
        if (running != null && running.getRemainingBurst() > 0) {
            int lvl = running.getCurrentQueueLevel();
            if (!queues.get(lvl).contains(running)) {
                queues.get(lvl).addLast(running);
            }
        }
    }

    @Override
    public void onProcessCompleted(ProcessModel running, int currentTime) {
        for (ArrayDeque<ProcessModel> q : queues) {
            q.remove(running);
        }
    }

    @Override
    public List<ProcessModel> getReadyQueueSnapshot() {
        List<ProcessModel> all = new ArrayList<>();
        for (ArrayDeque<ProcessModel> q : queues) {
            all.addAll(q);
        }
        return all;
    }

    public List<List<ProcessModel>> getMultiQueueSnapshot() {
        List<List<ProcessModel>> snapshot = new ArrayList<>();
        for (ArrayDeque<ProcessModel> q : queues) {
            snapshot.add(new ArrayList<>(q));
        }
        return snapshot;
    }

    @Override
    public void reset() {
        super.reset();
        for (ArrayDeque<ProcessModel> q : queues) {
            q.clear();
        }
    }
}
