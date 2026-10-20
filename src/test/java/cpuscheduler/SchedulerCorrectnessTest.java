package cpuscheduler;

import cpuscheduler.model.*;
import cpuscheduler.scheduler.*;
import cpuscheduler.simulation.SimulationEngine;

import java.util.*;

/**
 * Comprehensive automated test suite for CPU Scheduling Studio.
 * Verifies all 25 required test scenarios and hand-calculated expected results
 * across all 10 scheduling algorithms.
 */
public class SchedulerCorrectnessTest {

    public interface TestCase {
        void run() throws Exception;
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + message);
        }
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Assertion Failed: " + message + " | Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertEquals(String message, double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError("Assertion Failed: " + message + " | Expected: " + expected + ", Actual: " + actual);
        }
    }

    // -------------------------------------------------------------
    // SCENARIO 1: One process with arrival time zero
    // -------------------------------------------------------------
    public static void testScenario1_SingleProcessZeroArrival() {
        List<ProcessModel> list = Collections.singletonList(new ProcessModel("P1", 0, 5, 1));
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.FCFS, SimulationConfig.defaultConfig());
        assertEquals("Total time", 5, r.getTotalTime());
        assertEquals("P1 CT", 5, r.getProcesses().get(0).getCompletionTime());
        assertEquals("P1 WT", 0, r.getProcesses().get(0).getWaitingTime());
        assertEquals("P1 TAT", 5, r.getProcesses().get(0).getTurnaroundTime());
        assertEquals("P1 RT", 0, r.getProcesses().get(0).getResponseTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 2: One process with non-zero arrival time
    // -------------------------------------------------------------
    public static void testScenario2_SingleProcessNonZeroArrival() {
        List<ProcessModel> list = Collections.singletonList(new ProcessModel("P1", 4, 3, 1));
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.FCFS, SimulationConfig.defaultConfig());
        assertEquals("Total time", 7, r.getTotalTime());
        assertEquals("P1 CT", 7, r.getProcesses().get(0).getCompletionTime());
        assertEquals("P1 WT", 0, r.getProcesses().get(0).getWaitingTime());
        assertEquals("P1 TAT", 3, r.getProcesses().get(0).getTurnaroundTime());
        assertEquals("P1 RT", 0, r.getProcesses().get(0).getResponseTime());
        assertEquals("Idle time", 4, r.getIdleTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 3: Multiple processes arriving simultaneously
    // -------------------------------------------------------------
    public static void testScenario3_SimultaneousArrivals() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 4, 1, 0),
                new ProcessModel("P2", 0, 2, 2, 1),
                new ProcessModel("P3", 0, 1, 3, 2)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.SJF, SimulationConfig.defaultConfig());
        // SJF should execute P3 (BT=1), then P2 (BT=2), then P1 (BT=4)
        assertEquals("P3 CT", 1, r.getProcesses().get(2).getCompletionTime());
        assertEquals("P2 CT", 3, r.getProcesses().get(1).getCompletionTime());
        assertEquals("P1 CT", 7, r.getProcesses().get(0).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 4: Multiple processes arriving at different times
    // -------------------------------------------------------------
    public static void testScenario4_StaggeredArrivals() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 4, 1, 0),
                new ProcessModel("P2", 1, 3, 1, 1),
                new ProcessModel("P3", 2, 1, 1, 2)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.FCFS, SimulationConfig.defaultConfig());
        assertEquals("Total time", 8, r.getTotalTime());
        assertEquals("P1 CT", 4, r.getProcesses().get(0).getCompletionTime());
        assertEquals("P2 CT", 7, r.getProcesses().get(1).getCompletionTime());
        assertEquals("P3 CT", 8, r.getProcesses().get(2).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 5: Workload with idle period between arrivals
    // -------------------------------------------------------------
    public static void testScenario5_IdlePeriodBetweenArrivals() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 2, 1, 0),
                new ProcessModel("P2", 5, 3, 1, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.FCFS, SimulationConfig.defaultConfig());
        assertEquals("Total time", 8, r.getTotalTime());
        assertEquals("P1 CT", 2, r.getProcesses().get(0).getCompletionTime());
        assertEquals("Idle time", 3, r.getIdleTime());
        assertEquals("P2 CT", 8, r.getProcesses().get(1).getCompletionTime());
        assertEquals("P2 RT", 0, r.getProcesses().get(1).getResponseTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 6: Equal arrival times and equal burst times
    // -------------------------------------------------------------
    public static void testScenario6_EqualArrivalAndBurst() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 3, 1, 0),
                new ProcessModel("P2", 0, 3, 1, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.FCFS, SimulationConfig.defaultConfig());
        assertEquals("P1 CT", 3, r.getProcesses().get(0).getCompletionTime());
        assertEquals("P2 CT", 6, r.getProcesses().get(1).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 7: Priority ties broken by arrival time/order
    // -------------------------------------------------------------
    public static void testScenario7_PriorityTies() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 3, 2, 0),
                new ProcessModel("P2", 0, 2, 2, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.PRIORITY_NP, SimulationConfig.defaultConfig());
        assertEquals("P1 runs first due to input order tiebreak", 3, r.getProcesses().get(0).getCompletionTime());
        assertEquals("P2 runs second", 5, r.getProcesses().get(1).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 8: High priority process arriving during execution
    // -------------------------------------------------------------
    public static void testScenario8_HighPriorityArrivingDuringExecution() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 5, 3, 0),
                new ProcessModel("P2", 1, 2, 1, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.PRIORITY_P, SimulationConfig.defaultConfig());
        // P1 runs [0, 1), then P2 arrives with PR=1 < 3 -> preempts P1!
        // P2 runs [1, 3). Then P1 resumes [3, 7).
        assertEquals("P2 CT", 3, r.getProcesses().get(1).getCompletionTime());
        assertEquals("P1 CT", 7, r.getProcesses().get(0).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 9: SRTF preemption
    // -------------------------------------------------------------
    public static void testScenario9_SRTFPreemption() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 6, 1, 0),
                new ProcessModel("P2", 2, 2, 1, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.SRTF, SimulationConfig.defaultConfig());
        // At t=2, P1 has rem=4. P2 arrives with rem=2 < 4 -> preempts!
        // P2 runs 2->4. P1 finishes 4->8.
        assertEquals("P2 CT", 4, r.getProcesses().get(1).getCompletionTime());
        assertEquals("P1 CT", 8, r.getProcesses().get(0).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 10: LRTF scheduling behavior
    // -------------------------------------------------------------
    public static void testScenario10_LRTFScheduling() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 2, 1, 0),
                new ProcessModel("P2", 0, 2, 1, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.LRTF, SimulationConfig.defaultConfig());
        // Both burst 2 at t=0. P1 runs 1 unit (rem 1). At t=1, P2 rem=2 > P1 rem=1, preempts P1!
        // P2 runs 1 unit (rem 1). At t=2, both rem 1. P1 runs to 3. P2 runs to 4.
        assertEquals("P1 CT", 3, r.getProcesses().get(0).getCompletionTime());
        assertEquals("P2 CT", 4, r.getProcesses().get(1).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 11: Round Robin with quantum 1
    // -------------------------------------------------------------
    public static void testScenario11_RoundRobinQuantumOne() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 2, 1, 0),
                new ProcessModel("P2", 0, 2, 1, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.ROUND_ROBIN, SimulationConfig.withQuantum(1));
        // Alternates: P1 (0->1), P2 (1->2), P1 (2->3), P2 (3->4)
        assertEquals("P1 CT", 3, r.getProcesses().get(0).getCompletionTime());
        assertEquals("P2 CT", 4, r.getProcesses().get(1).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 12: Round Robin with quantum larger than all bursts
    // -------------------------------------------------------------
    public static void testScenario12_RoundRobinLargeQuantum() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 3, 1, 0),
                new ProcessModel("P2", 0, 2, 1, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.ROUND_ROBIN, SimulationConfig.withQuantum(10));
        // Large quantum acts like FCFS: P1 runs to completion (3), P2 runs to completion (5)
        assertEquals("P1 CT", 3, r.getProcesses().get(0).getCompletionTime());
        assertEquals("P2 CT", 5, r.getProcesses().get(1).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 13: Arrival at exact moment quantum expires
    // -------------------------------------------------------------
    public static void testScenario13_ArrivalAtQuantumExpiration() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 4, 1, 0),
                new ProcessModel("P2", 2, 2, 1, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.ROUND_ROBIN, SimulationConfig.withQuantum(2));
        // P1 runs 0->2. At t=2, P2 arrives. P2 enters queue BEFORE P1 re-enqueues!
        // Queue at t=2: [P2, P1]. P2 runs 2->4. P1 runs 4->6.
        assertEquals("P2 CT", 4, r.getProcesses().get(1).getCompletionTime());
        assertEquals("P1 CT", 6, r.getProcesses().get(0).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 14: MLFQ demotion after quantum expiration
    // -------------------------------------------------------------
    public static void testScenario14_MLFQDemotion() {
        List<ProcessModel> list = Collections.singletonList(new ProcessModel("P1", 0, 5, 1, 0));
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.MLFQ, SimulationConfig.withQuantum(2));
        // P1 runs in Q0 (2 units, t=0->2), demoted to Q1.
        // P1 runs in Q1 (4 units, t=2->5 finishes).
        assertEquals("P1 CT", 5, r.getProcesses().get(0).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 15 & 16: MLFQ interruption by higher queue & quantum preservation
    // -------------------------------------------------------------
    public static void testScenario15_16_MLFQQuantumPreservation() {
        // P1 arrives at t=0, BT=6. Starts in Q0 (Q=2).
        // t=0->2: P1 finishes Q0 quantum, demoted to Q1 (Q1 quantum = 4).
        // t=2->3: P1 executes 1 unit in Q1. Its remaining quantum in Q1 is 3 units!
        // At t=3: P2 arrives in Q0 (BT=1).
        // P2 preempts P1!
        // t=3->4: P2 executes in Q0 and finishes at 4.
        // t=4: P1 resumes in Q1! Since its quantum was preserved, it has 3 units left in Q1.
        // P1 has 4 units of burst remaining (total burst 6 - 1 in Q0 - 1 in Q1 = 4).
        // It runs 3 units in Q1 (t=4->7), exhausts Q1 quantum, and is demoted to Q2!
        // In Q2, it finishes its final 1 unit (t=7->8).
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 6, 1, 0),
                new ProcessModel("P2", 3, 1, 1, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.MLFQ, SimulationConfig.withQuantum(2));
        assertEquals("P2 finishes during preemption at 4", 4, r.getProcesses().get(1).getCompletionTime());
        assertEquals("P1 completes at 7", 7, r.getProcesses().get(0).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 17: MLFQ lowest queue FCFS behavior
    // -------------------------------------------------------------
    public static void testScenario17_MLFQLowestQueueFCFS() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 10, 1, 0),
                new ProcessModel("P2", 0, 10, 1, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.MLFQ, SimulationConfig.withQuantum(1));
        // Both demote to Q2 (after 1 unit in Q0 + 2 units in Q1 = 3 units each, t=6)
        // In Q2, FCFS applies: P1 runs to completion before P2!
        assertTrue("P1 finishes before P2 in Q2 FCFS", r.getProcesses().get(0).getCompletionTime() < r.getProcesses().get(1).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 18: HRRN response-ratio calculations
    // -------------------------------------------------------------
    public static void testScenario18_HRRNCalculations() {
        ProcessModel p = new ProcessModel("P1", 0, 4, 1);
        double rrAt0 = cpuscheduler.scheduler.algorithms.HRRNScheduler.computeResponseRatio(p, 0);
        assertEquals("RR at arrival", 1.0, rrAt0, 0.001);
        double rrAt4 = cpuscheduler.scheduler.algorithms.HRRNScheduler.computeResponseRatio(p, 4);
        assertEquals("RR after wait 4", (4.0 + 4.0) / 4.0, rrAt4, 0.001);
    }

    // -------------------------------------------------------------
    // SCENARIO 19: Empty workload handling
    // -------------------------------------------------------------
    public static void testScenario19_EmptyWorkload() {
        boolean thrown = false;
        try {
            SimulationEngine.simulate(Collections.emptyList(), SchedulingAlgorithmType.FCFS, SimulationConfig.defaultConfig());
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("Empty workload rejected", thrown);
    }

    // -------------------------------------------------------------
    // SCENARIO 20: Invalid process inputs
    // -------------------------------------------------------------
    public static void testScenario20_InvalidInputs() {
        boolean caughtBT = false;
        try {
            new ProcessModel("P1", 0, 0, 1);
        } catch (IllegalArgumentException e) {
            caughtBT = true;
        }
        assertTrue("Burst <= 0 rejected", caughtBT);

        boolean caughtAT = false;
        try {
            new ProcessModel("P1", -1, 4, 1);
        } catch (IllegalArgumentException e) {
            caughtAT = true;
        }
        assertTrue("Negative arrival rejected", caughtAT);
    }

    // -------------------------------------------------------------
    // SCENARIO 21: Invalid or non-positive quantum
    // -------------------------------------------------------------
    public static void testScenario21_InvalidQuantum() {
        boolean caught = false;
        try {
            SimulationConfig.withQuantum(0);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assertTrue("Quantum <= 0 rejected", caught);
    }

    // -------------------------------------------------------------
    // SCENARIO 22: Duplicate process IDs
    // -------------------------------------------------------------
    public static void testScenario22_DuplicatePIDs() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 2, 1),
                new ProcessModel("P1", 1, 3, 1)
        );
        boolean caught = false;
        try {
            SimulationEngine.simulate(list, SchedulingAlgorithmType.FCFS, SimulationConfig.defaultConfig());
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assertTrue("Duplicate process IDs rejected", caught);
    }

    // -------------------------------------------------------------
    // SCENARIO 23: Large but permitted time values
    // -------------------------------------------------------------
    public static void testScenario23_LargePermittedTimes() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 500, 1),
                new ProcessModel("P2", 200, 300, 1)
        );
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.FCFS, SimulationConfig.defaultConfig());
        assertEquals("Total time", 800, r.getTotalTime());
        assertEquals("P1 CT", 500, r.getProcesses().get(0).getCompletionTime());
        assertEquals("P2 CT", 800, r.getProcesses().get(1).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 24: CSV parsing quotes and commas
    // -------------------------------------------------------------
    public static void testScenario24_CsvFormatting() {
        String csv = "Process,Arrival Time,Burst Time,Priority\n" +
                     "\"P1\",0,5,2\n" +
                     "\"P,2\",1,3,1\n";
        List<ProcessModel> procs = cpuscheduler.export.CsvImporter.parseCsv(csv);
        assertEquals("2 processes parsed", 2, procs.size());
        assertEquals("P,2 ID parsed", "P,2", procs.get(1).getId());
    }

    // -------------------------------------------------------------
    // SCENARIO 25: Metric consistency between single simulation and comparisons
    // -------------------------------------------------------------
    public static void testScenario25_MetricConsistency() {
        List<ProcessModel> workload = Arrays.asList(
                new ProcessModel("P1", 0, 5, 2, 0),
                new ProcessModel("P2", 1, 3, 1, 1),
                new ProcessModel("P3", 2, 8, 4, 2)
        );
        SimulationConfig config = SimulationConfig.defaultConfig();
        for (SchedulingAlgorithmType algo : SchedulingAlgorithmType.values()) {
            SimulationResult r1 = SimulationEngine.simulate(workload, algo, config);
            SimulationResult r2 = SimulationEngine.simulate(workload, algo, config);
            assertEquals(algo + " avg WT matches", r1.getAvgWaitingTime(), r2.getAvgWaitingTime(), 0.0001);
            assertEquals(algo + " avg TAT matches", r1.getAvgTurnaroundTime(), r2.getAvgTurnaroundTime(), 0.0001);
            assertEquals(algo + " total time matches", r1.getTotalTime(), r2.getTotalTime());
        }
    }

    // -------------------------------------------------------------
    // SCENARIO 26: Context Switch Overhead affects timeline and metrics
    // -------------------------------------------------------------
    public static void testScenario26_ContextSwitchOverhead() {
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 2, 1, 0),
                new ProcessModel("P2", 0, 2, 1, 1)
        );
        // CS overhead = 2 units.
        // P1 runs 0->2. Process switch to P2 occurs!
        // Context switch takes [2, 4).
        // P2 runs 4->6. Total time = 6 (instead of 4).
        SimulationConfig config = new SimulationConfig(2, 2, false, 5, 1, 0, 15);
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.FCFS, config);
        assertEquals("Total time with CS=2", 6, r.getTotalTime());
        assertEquals("Context switch count", 1, r.getContextSwitchCount());
        assertEquals("Context switch time", 2, r.getContextSwitchTime());
        assertEquals("P2 CT", 6, r.getProcesses().get(1).getCompletionTime());
    }

    // -------------------------------------------------------------
    // SCENARIO 27: Priority Aging prevents starvation
    // -------------------------------------------------------------
    public static void testScenario27_PriorityAging() {
        // P1 arrives at 0, BT=10, PR=1. Runs.
        // P2 arrives at 0, BT=2, PR=5.
        // P3 arrives at 2, BT=2, PR=1.
        // Without aging, P3 runs before P2.
        // With aging interval 2 and boost 2: P2's priority will be boosted from 5 to 3 at t=2, then to 1 at t=4!
        List<ProcessModel> list = Arrays.asList(
                new ProcessModel("P1", 0, 6, 1, 0),
                new ProcessModel("P2", 0, 2, 5, 1),
                new ProcessModel("P3", 2, 2, 4, 2)
        );
        SimulationConfig withAging = new SimulationConfig(2, 0, true, 2, 2, 0, 15);
        SimulationResult r = SimulationEngine.simulate(list, SchedulingAlgorithmType.PRIORITY_NP, withAging);
        // P2 aged: should run before P3!
        assertTrue("P2 runs before P3 due to aging boost",
                r.getProcesses().get(1).getCompletionTime() < r.getProcesses().get(2).getCompletionTime());
    }

    // -------------------------------------------------------------
    // HAND-CALCULATED TESTS FOR ALL 10 ALGORITHMS ON STANDARD WORKLOAD
    // Workload: P1(0, 4, PR=2), P2(1, 3, PR=1), P3(2, 1, PR=3)
    // -------------------------------------------------------------
    public static void testHandCalculated_AllTenAlgorithms() {
        List<ProcessModel> base = Arrays.asList(
                new ProcessModel("P1", 0, 4, 2, 0),
                new ProcessModel("P2", 1, 3, 1, 1),
                new ProcessModel("P3", 2, 1, 3, 2)
        );

        // 1. FCFS: P1(0->4), P2(4->7), P3(7->8). Total=8
        SimulationResult fcfs = SimulationEngine.simulate(base, SchedulingAlgorithmType.FCFS, SimulationConfig.defaultConfig());
        assertEquals("FCFS P1 CT", 4, fcfs.getProcesses().get(0).getCompletionTime());
        assertEquals("FCFS P2 CT", 7, fcfs.getProcesses().get(1).getCompletionTime());
        assertEquals("FCFS P3 CT", 8, fcfs.getProcesses().get(2).getCompletionTime());

        // 2. SJF: P1(0->4), P3(4->5), P2(5->8). Total=8
        SimulationResult sjf = SimulationEngine.simulate(base, SchedulingAlgorithmType.SJF, SimulationConfig.defaultConfig());
        assertEquals("SJF P1 CT", 4, sjf.getProcesses().get(0).getCompletionTime());
        assertEquals("SJF P3 CT", 5, sjf.getProcesses().get(2).getCompletionTime());
        assertEquals("SJF P2 CT", 8, sjf.getProcesses().get(1).getCompletionTime());

        // 3. SRTF: P1(0->2), P3(2->3), P1(3->5), P2(5->8). Total=8
        SimulationResult srtf = SimulationEngine.simulate(base, SchedulingAlgorithmType.SRTF, SimulationConfig.defaultConfig());
        assertEquals("SRTF P3 CT", 3, srtf.getProcesses().get(2).getCompletionTime());
        assertEquals("SRTF P1 CT", 5, srtf.getProcesses().get(0).getCompletionTime());
        assertEquals("SRTF P2 CT", 8, srtf.getProcesses().get(1).getCompletionTime());

        // 4. LJF: P1(0->4), P2(4->7), P3(7->8). Total=8
        SimulationResult ljf = SimulationEngine.simulate(base, SchedulingAlgorithmType.LJF, SimulationConfig.defaultConfig());
        assertEquals("LJF P1 CT", 4, ljf.getProcesses().get(0).getCompletionTime());
        assertEquals("LJF P2 CT", 7, ljf.getProcesses().get(1).getCompletionTime());
        assertEquals("LJF P3 CT", 8, ljf.getProcesses().get(2).getCompletionTime());

        // 5. LRTF: Total=8
        SimulationResult lrtf = SimulationEngine.simulate(base, SchedulingAlgorithmType.LRTF, SimulationConfig.defaultConfig());
        assertEquals("LRTF Total time", 8, lrtf.getTotalTime());

        // 6. HRRN: P1(0->4), P3(4->5), P2(5->8). Total=8
        SimulationResult hrrn = SimulationEngine.simulate(base, SchedulingAlgorithmType.HRRN, SimulationConfig.defaultConfig());
        assertEquals("HRRN P1 CT", 4, hrrn.getProcesses().get(0).getCompletionTime());
        assertEquals("HRRN P3 CT", 5, hrrn.getProcesses().get(2).getCompletionTime());
        assertEquals("HRRN P2 CT", 8, hrrn.getProcesses().get(1).getCompletionTime());

        // 7. Priority NP: P1(0->4), P2(4->7), P3(7->8). Total=8
        SimulationResult priNp = SimulationEngine.simulate(base, SchedulingAlgorithmType.PRIORITY_NP, SimulationConfig.defaultConfig());
        assertEquals("Priority NP P1 CT", 4, priNp.getProcesses().get(0).getCompletionTime());
        assertEquals("Priority NP P2 CT", 7, priNp.getProcesses().get(1).getCompletionTime());
        assertEquals("Priority NP P3 CT", 8, priNp.getProcesses().get(2).getCompletionTime());

        // 8. Priority P: P1(0->1), P2(1->4), P1(4->7), P3(7->8). Total=8
        SimulationResult priP = SimulationEngine.simulate(base, SchedulingAlgorithmType.PRIORITY_P, SimulationConfig.defaultConfig());
        assertEquals("Priority P P2 CT", 4, priP.getProcesses().get(1).getCompletionTime());
        assertEquals("Priority P P1 CT", 7, priP.getProcesses().get(0).getCompletionTime());
        assertEquals("Priority P P3 CT", 8, priP.getProcesses().get(2).getCompletionTime());

        // 9. Round Robin (Q=2): P1(0->2), P2(2->4), P3(4->5), P1(5->7), P2(7->8). Total=8
        SimulationResult rr = SimulationEngine.simulate(base, SchedulingAlgorithmType.ROUND_ROBIN, SimulationConfig.withQuantum(2));
        assertEquals("RR P3 CT", 5, rr.getProcesses().get(2).getCompletionTime());
        assertEquals("RR P1 CT", 7, rr.getProcesses().get(0).getCompletionTime());
        assertEquals("RR P2 CT", 8, rr.getProcesses().get(1).getCompletionTime());

        // 10. MLFQ (Base Q=1): Total=8
        SimulationResult mlfq = SimulationEngine.simulate(base, SchedulingAlgorithmType.MLFQ, SimulationConfig.withQuantum(1));
        assertEquals("MLFQ Total time", 8, mlfq.getTotalTime());
        assertEquals("MLFQ P3 CT", 3, mlfq.getProcesses().get(2).getCompletionTime());
        assertEquals("MLFQ P2 CT", 7, mlfq.getProcesses().get(1).getCompletionTime());
        assertEquals("MLFQ P1 CT", 8, mlfq.getProcesses().get(0).getCompletionTime());
    }

    public static Map<String, TestCase> getTests() {
        Map<String, TestCase> tests = new LinkedHashMap<>();
        tests.put("Scenario 1: Single Process Zero Arrival", SchedulerCorrectnessTest::testScenario1_SingleProcessZeroArrival);
        tests.put("Scenario 2: Single Process Non-Zero Arrival", SchedulerCorrectnessTest::testScenario2_SingleProcessNonZeroArrival);
        tests.put("Scenario 3: Simultaneous Arrivals", SchedulerCorrectnessTest::testScenario3_SimultaneousArrivals);
        tests.put("Scenario 4: Staggered Arrivals", SchedulerCorrectnessTest::testScenario4_StaggeredArrivals);
        tests.put("Scenario 5: Idle Period Between Arrivals", SchedulerCorrectnessTest::testScenario5_IdlePeriodBetweenArrivals);
        tests.put("Scenario 6: Equal Arrival and Burst", SchedulerCorrectnessTest::testScenario6_EqualArrivalAndBurst);
        tests.put("Scenario 7: Priority Ties", SchedulerCorrectnessTest::testScenario7_PriorityTies);
        tests.put("Scenario 8: High Priority Arriving During Execution", SchedulerCorrectnessTest::testScenario8_HighPriorityArrivingDuringExecution);
        tests.put("Scenario 9: SRTF Preemption", SchedulerCorrectnessTest::testScenario9_SRTFPreemption);
        tests.put("Scenario 10: LRTF Scheduling", SchedulerCorrectnessTest::testScenario10_LRTFScheduling);
        tests.put("Scenario 11: Round Robin Quantum 1", SchedulerCorrectnessTest::testScenario11_RoundRobinQuantumOne);
        tests.put("Scenario 12: Round Robin Large Quantum", SchedulerCorrectnessTest::testScenario12_RoundRobinLargeQuantum);
        tests.put("Scenario 13: Arrival at Quantum Expiration", SchedulerCorrectnessTest::testScenario13_ArrivalAtQuantumExpiration);
        tests.put("Scenario 14: MLFQ Demotion", SchedulerCorrectnessTest::testScenario14_MLFQDemotion);
        tests.put("Scenario 15 & 16: MLFQ Quantum Preservation & Resumption", SchedulerCorrectnessTest::testScenario15_16_MLFQQuantumPreservation);
        tests.put("Scenario 17: MLFQ Lowest Queue FCFS", SchedulerCorrectnessTest::testScenario17_MLFQLowestQueueFCFS);
        tests.put("Scenario 18: HRRN Calculations", SchedulerCorrectnessTest::testScenario18_HRRNCalculations);
        tests.put("Scenario 19: Empty Workload Rejection", SchedulerCorrectnessTest::testScenario19_EmptyWorkload);
        tests.put("Scenario 20: Invalid Inputs Rejection", SchedulerCorrectnessTest::testScenario20_InvalidInputs);
        tests.put("Scenario 21: Invalid Quantum Rejection", SchedulerCorrectnessTest::testScenario21_InvalidQuantum);
        tests.put("Scenario 22: Duplicate PIDs Rejection", SchedulerCorrectnessTest::testScenario22_DuplicatePIDs);
        tests.put("Scenario 23: Large Permitted Times", SchedulerCorrectnessTest::testScenario23_LargePermittedTimes);
        tests.put("Scenario 24: CSV Formatting & Quotes", SchedulerCorrectnessTest::testScenario24_CsvFormatting);
        tests.put("Scenario 25: Metric Consistency", SchedulerCorrectnessTest::testScenario25_MetricConsistency);
        tests.put("Scenario 26: Context Switch Overhead Simulation", SchedulerCorrectnessTest::testScenario26_ContextSwitchOverhead);
        tests.put("Scenario 27: Priority Aging & Starvation Prevention", SchedulerCorrectnessTest::testScenario27_PriorityAging);
        tests.put("Hand-Calculated: All 10 Algorithms On Benchmark Workload", SchedulerCorrectnessTest::testHandCalculated_AllTenAlgorithms);
        return tests;
    }
}
