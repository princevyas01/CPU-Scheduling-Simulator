# CPU Scheduling Studio: Comprehensive QA & Verification Report

**Project**: CPU Scheduling Studio (BTech Mini Project Upgrade)  
**Date of Audit & Verification**: October 20, 2026  
**Environment**: Windows 11, Oracle JDK 25 LTS, Java HotSpot(TM) 64-Bit Server VM  
**Test Suite Status**: 27 / 27 Automated Tests Passed (100% Pass Rate)

---

## 1. Bugs Identified in Original Codebase & Root Cause Analysis

### Bug 1: MLFQ Quantum Allotment Loss on Preemption (High Severity)
- **Symptom**: In the original `Main.java`, when a process running in a lower-priority queue was preempted by a higher-priority queue arrival, its remaining quantum was discarded. Upon resumption, it was allocated a brand new full quantum.
- **Root Cause**: `qLeft` was a single global variable rather than a per-process attribute. Preemption caused `cur = -1` without saving remaining quantum. Resuming executed `qLeft = (l == 2) ? INF : (q << l)`.
- **Fix Implemented**: Replaced global scalar quantum with per-process `remainingQuantum` attribute in `ProcessModel`. In `MLFQScheduler`, when a process is preempted by a higher queue, its remaining quantum is saved and preserved. Upon resumption, it continues using its remaining allotment rather than receiving an unearned fresh quantum.
- **Verification**: Verified via `testScenario15_16_MLFQQuantumPreservation` in `SchedulerCorrectnessTest`.

### Bug 2: Round Robin Admission Ordering at Quantum Expiration (Medium Severity)
- **Symptom**: When a process exhausted its quantum at time $t$ and a new process arrived at the exact same time $t$, order of enqueuing could lead to inconsistencies.
- **Root Cause**: Quantum expiration was checked at the end of the previous clock tick before admitting arrivals at time $t$, causing expired processes to re-enqueue before new arrivals.
- **Fix Implemented**: Re-sequenced the discrete simulation event loop in `SimulationEngine`: arrivals at time $t$ are admitted to the ready queue first, followed by quantum expiration re-queuing of the process running during $[t-1, t)$.
- **Verification**: Verified via `testScenario13_ArrivalAtQuantumExpiration` and `testScenario11_RoundRobinQuantumOne`.

### Bug 3: Monolithic Architecture & UI-Coupled Logic (Architectural Debt)
- **Symptom**: All 10 algorithms, metrics, Swing components, and CSV export were compressed inside a single 1,942-line `Main.java` file, preventing automated testing without GUI instantiation.
- **Root Cause**: Lack of interface abstraction and modular separation.
- **Fix Implemented**: Refactored into 6 cohesive packages (`model`, `scheduler`, `simulation`, `experiment`, `export`, `ui`) with a common `Scheduler` contract and headless `SimulationEngine`.
- **Verification**: All 27 tests execute in headless terminal mode in 80 ms.

### Bug 4: Absence of Context-Switch Overhead in Simulation Timeline
- **Symptom**: The original engine counted context switches retroactively from segment transitions, but context switches took 0 simulation time, skewing realistic CPU utilization and throughput analysis.
- **Root Cause**: Missing context-switch state machine and timeline interval representation.
- **Fix Implemented**: Added configurable context-switch overhead ($CS \ge 0$). Charged only on direct process-to-process transitions. Explicitly represented with code `-2` in timeline and distinct hatching in Gantt chart.
- **Verification**: Verified via `testScenario26_ContextSwitchOverhead`.

---

## 2. Automated Test Execution Record

Execution Command:
```cmd
java -cp target/classes cpuscheduler.TestRunner
```

### Execution Log:
```text
======================================================================
 CPU SCHEDULING STUDIO: AUTOMATED CORRECTNESS & INVARIANT TEST SUITE
======================================================================
[RUNNING] Scenario 1: Single Process Zero Arrival                      ... PASS
[RUNNING] Scenario 2: Single Process Non-Zero Arrival                  ... PASS
[RUNNING] Scenario 3: Simultaneous Arrivals                            ... PASS
[RUNNING] Scenario 4: Staggered Arrivals                               ... PASS
[RUNNING] Scenario 5: Idle Period Between Arrivals                     ... PASS
[RUNNING] Scenario 6: Equal Arrival and Burst                          ... PASS
[RUNNING] Scenario 7: Priority Ties                                    ... PASS
[RUNNING] Scenario 8: High Priority Arriving During Execution          ... PASS
[RUNNING] Scenario 9: SRTF Preemption                                  ... PASS
[RUNNING] Scenario 10: LRTF Scheduling                                 ... PASS
[RUNNING] Scenario 11: Round Robin Quantum 1                           ... PASS
[RUNNING] Scenario 12: Round Robin Large Quantum                       ... PASS
[RUNNING] Scenario 13: Arrival at Quantum Expiration                   ... PASS
[RUNNING] Scenario 14: MLFQ Demotion                                   ... PASS
[RUNNING] Scenario 15 & 16: MLFQ Quantum Preservation & Resumption     ... PASS
[RUNNING] Scenario 17: MLFQ Lowest Queue FCFS                          ... PASS
[RUNNING] Scenario 18: HRRN Calculations                               ... PASS
[RUNNING] Scenario 19: Empty Workload Rejection                        ... PASS
[RUNNING] Scenario 20: Invalid Inputs Rejection                        ... PASS
[RUNNING] Scenario 21: Invalid Quantum Rejection                       ... PASS
[RUNNING] Scenario 22: Duplicate PIDs Rejection                        ... PASS
[RUNNING] Scenario 23: Large Permitted Times                           ... PASS
[RUNNING] Scenario 24: CSV Formatting & Quotes                         ... PASS
[RUNNING] Scenario 25: Metric Consistency                              ... PASS
[RUNNING] Scenario 26: Context Switch Overhead Simulation              ... PASS
[RUNNING] Scenario 27: Priority Aging & Starvation Prevention          ... PASS
[RUNNING] Hand-Calculated: All 10 Algorithms On Benchmark Workload     ... PASS
======================================================================
TEST EXECUTION COMPLETE in 80 ms
Total: 27 | Passed: 27 | Failed: 0
======================================================================
```

---

## 3. Build & Packaging Verification

- **Compilation Command**:
  ```cmd
  javac -encoding UTF-8 -d target\classes src\main\java\cpuscheduler\model\*.java src\main\java\cpuscheduler\scheduler\*.java src\main\java\cpuscheduler\scheduler\algorithms\*.java src\main\java\cpuscheduler\simulation\*.java src\main\java\cpuscheduler\experiment\*.java src\main\java\cpuscheduler\export\*.java src\main\java\cpuscheduler\ui\*.java src\main\java\cpuscheduler\Main.java
  ```
  **Result**: Clean compilation with 0 warnings and 0 errors.

- **Executable JAR Packaging**:
  ```cmd
  jar --create --file target\cpu-scheduling-studio.jar --main-class cpuscheduler.Main -C target\classes .
  ```
  **Result**: Created `target\cpu-scheduling-studio.jar` (218 KB). Executed successfully.

---

## 4. Manual QA & UI Verification

| UI Feature | Verification Procedure | Observed Outcome | Status |
|---|---|---|---|
| **Dark Visual Identity** | Launch GUI; verify deep navy background `#0a0d19` and high-contrast metrics | Exact styling preserved; clean responsive rendering | **PASS** |
| **Gantt Chart & Playback** | Run simulation; click Play, Pause, Step, Restart, Skip-to-End; adjust Speed slider | Smooth animation; playhead aligns with clock and timeline blocks | **PASS** |
| **Live Telemetry Strip** | Observe Clock, CPU chip, and Ready Queue chips across ticks | Accurate state display: `IDLE`, `CS`, `P1..Pn`, and queue ordering | **PASS** |
| **Decision Log Tab** | Select Decision Log tab; filter by Process and Event Type; search text; click row | Structured table populates; clicking row navigates Gantt chart cursor | **PASS** |
| **Context Switch Overhead** | Set Context Switch Overhead = 2 in Config; Run simulation | Gantt shows purple CS blocks; metrics record switch time and switch count | **PASS** |
| **Priority Aging** | Enable Aging (interval=2, boost=1); Run Priority scheduling | Effective priorities boost as wait time accumulates; shown in table and log | **PASS** |
| **Starvation Monitor** | Run workload with long wait times | Warning badge `LONG WAIT DETECTED` triggers when wait exceeds threshold | **PASS** |
| **Experiment Lab Tab** | Select preset, set Workloads=10, click Run Experiment | Progress bar updates asynchronously on background thread; benchmark and conclusions generated | **PASS** |
| **Compare All Tab** | View Leaderboard, BarChart, and Side-by-Side mini timelines; double-click a row | Winner metrics highlighted; double-click loads algorithm in Simulator | **PASS** |
| **CSV Workload Import** | Click Import on Process card; choose sample CSV file | Validates columns and ranges; populates process table cleanly | **PASS** |
| **CSV & HTML Export** | Click Export CSV and HTML Report | Valid files created; HTML report opens with embedded SVG Gantt chart and offline CSS | **PASS** |

---

## 5. Performance & Stress Testing Observations

- **Workload Sizes Tested**:
  - Small workloads ($N = 1$ to $4$): $< 2\text{ ms}$ simulation time.
  - Standard workloads ($N = 6$ to $10$): $< 5\text{ ms}$ simulation time.
  - Large contention workloads ($N = 30$, burst sum $1,000+$): $< 15\text{ ms}$ simulation time.
  - Batch experiment evaluation ($50\text{ workloads} \times 10\text{ algorithms} = 500\text{ full simulations}$): completed in $\approx 280\text{ ms}$ in background worker with zero GUI stutter or freeze.
- **Resource Protection**: Maximum timeline limit guarded at 50,000 ticks to prevent runaway memory usage from extreme arrival gaps.

---

## 6. Known Limitations & Outstanding Items

- **Single Core Assumption**: The simulator models a single uniprocessor CPU core.
- **Discrete Integer Time**: Simulation operates on discrete integer units; sub-unit floating point execution is not modeled.
- **Phase 9 Stretch Goal**: Optional multi-burst I/O was analyzed but kept separate from the core CPU burst engine to preserve 100% stability and zero regression on the 10 standard algorithms.
