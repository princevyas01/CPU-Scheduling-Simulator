# CPU Scheduling Studio: Technical Architecture & Specification

## 1. System Architecture Overview

CPU Scheduling Studio is structured following modern object-oriented software engineering principles, enforcing strict separation of concerns across six modular packages:

```
                      +-----------------------------+
                      |     MainFrame / GUI View    |
                      |  (Swing Custom Components)  |
                      +--------------+--------------+
                                     |
                                     v
                      +-----------------------------+
                      |      SimulationEngine       |
                      |  (Event Driver & Invariants)|
                      +-------+--------------+------+
                              |              |
                +-------------+              +-------------+
                |                                          |
                v                                          v
+-------------------------------+          +-------------------------------+
|      Scheduler Interface      |          |         Domain Models         |
|   (10 Algorithm Strategies)   |          | (ProcessModel, SimResult, ev) |
+-------------------------------+          +-------------------------------+
                |                                          |
                +-------------+              +-------------+
                              |              |
                              v              v
                      +-----------------------------+
                      |   Exporters & Experiments   |
                      | (CSV, HTML, ExperimentLab)  |
                      +-----------------------------+
```

### Module Responsibilities:
1. `cpuscheduler.model`: Defines immutable domain models (`ProcessModel`, `SimulationResult`, `SimulationConfig`, `SchedulingEvent`, `GanttSegment`). Every simulation receives independent deep-copied process instances.
2. `cpuscheduler.scheduler`: Defines the `Scheduler` interface, `SchedulingAlgorithmType` enum, and `SchedulerFactory`.
3. `cpuscheduler.scheduler.algorithms`: Implements the 10 distinct scheduling algorithms, extending `BaseScheduler` for common tie-breaking and queue operations.
4. `cpuscheduler.simulation`: Centralizes simulation event progression, discrete clock advancement, context-switch intervals, priority aging, and mathematical invariant verification.
5. `cpuscheduler.experiment`: Provides the `WorkloadGenerator`, preset definitions, and `ExperimentLab` background evaluation engine.
6. `cpuscheduler.export`: Provides RFC 4180-compliant CSV import/export and standalone SVG-embedded HTML reporting.
7. `cpuscheduler.ui`: Implements the high-contrast dark theme, custom vector controls, animated Gantt panel, live CPU telemetry, and tabular dashboards.

---

## 2. Process State Transition Model

Each process progresses through a formal finite state machine during its lifecycle:

```text
       [ NEW / UNARRIVED ]
                |
                | (clock >= arrivalTime)
                v
       +-----------------+
       |      READY      |<----------------+
       +-----------------+                 |
          |           ^                    |
 (dispatch|           | (preemption /      | (context switch complete)
  decision)           |  quantum expired)  |
          v           |                    |
       +-----------------+                 |
       |  CONTEXT_SWITCH |-----------------+
       |    (if CS > 0)  |
       +-----------------+
          |
 (direct  |
  dispatch)
          v
       +-----------------+
       |     RUNNING     |
       +-----------------+
                |
                | (remainingBurst == 0)
                v
       [    TERMINATED   ]
```

### State Definitions:
- **NEW / UNARRIVED**: Initial state before the simulation clock reaches `arrivalTime`.
- **READY**: Process has arrived and is queued in the ready queue (or MLFQ queue levels), awaiting CPU allocation.
- **CONTEXT_SWITCH**: Process is transitioning into or out of the CPU; CPU is occupied by hardware switching overhead ($CS > 0$).
- **RUNNING**: Process is actively executing on the CPU; its `remainingBurst` decrements once per clock unit.
- **TERMINATED**: Process has consumed its entire CPU burst requirement ($rem = 0$); completion metrics are finalized.

---

## 3. Discrete Simulation Event Flow

The `SimulationEngine` advances through discrete integer time ticks $t = 0, 1, 2, \dots$ according to the following deterministic event sequence:

```text
[ TICK START: t ]
       |
       v
1. ADMIT ARRIVALS
   Identify all processes where arrivalTime <= t that have not arrived.
   Enqueue them in ready queue & emit PROCESS_ARRIVED event.
       |
       v
2. PRIORITY AGING
   For ready processes waiting in queue:
   Increment readyWaitTime.
   If readyWaitTime % agingInterval == 0:
       effectivePriority = max(minPriority, effectivePriority - priorityBoost)
       Emit PRIORITY_AGED event.
       |
       v
3. CHECK QUANTUM EXPIRATION (RR / MLFQ)
   If running process exists and remainingQuantum == 0:
       Emit QUANTUM_EXPIRED / PROCESS_DEMOTED event.
       Call scheduler.onQuantumExpired().
       Set running = null.
       |
       v
4. CONTEXT SWITCH IN-PROGRESS?
   If csRemaining > 0:
       Record CONTEXT_SWITCH (-2) in timeline.
       Advance clock: t++, csRemaining--.
       If csRemaining == 0: dispatch target process & emit CONTEXT_SWITCH_COMPLETED.
       Return to TICK START.
       |
       v
5. SCHEDULER SELECTION
   Call scheduler.selectProcess(t, running, config).
   - If preemption requested:
         Preempt running -> re-enqueue -> initiate context switch (or immediate dispatch).
   - If dispatching:
         If transitioning from different process without idle: initiate context switch.
         Else: set running = selected.
   - If idle:
         Record IDLE (-1) in timeline. Advance t++.
         Return to TICK START.
       |
       v
6. EXECUTE BURST
   Record process index in timeline & snapshot ready queue.
   running.decrementRemainingBurst().
   running.decrementRemainingQuantum().
   Advance clock: t++.
   If remainingBurst == 0:
       Process finishes: set CT = t, TAT = CT - AT, WT = TAT - BT.
       Emit PROCESS_COMPLETED event.
       Set running = null.
       |
       v
[ TICK END -> Repeat until all processes completed ]
```

---

## 4. Context-Switch Overhead Specification

Context switching represents the computational cost of saving process register state, updating process control blocks (PCBs), and flushing CPU cache lines.

### Operational Policy:
- Overhead parameter $CS \ge 0$ (configurable integer).
- Charged **only** when execution transitions directly from one process to a **different** process, without intervening CPU idle periods.
- **Zero Overhead Situations**:
  - Initial CPU dispatch from system start.
  - Dispatch following a CPU idle period ($T_{idle} > 0$).
  - Re-dispatch of the same process (e.g. Round Robin sole-process continuation).
- **Timeline Integration**: Context switch intervals are explicitly marked with code `-2` in the execution timeline and visually distinguished with purple/magenta diagonal hatching in the Gantt chart.
- **Arrival Preservation**: New processes arriving during context-switch intervals $[t, t+CS)$ are admitted to the ready queue at their exact arrival times.

---

## 5. Priority Aging & Starvation Mitigation

Fixed priority scheduling suffers from potential starvation: low-priority tasks may wait indefinitely if higher-priority processes arrive continuously.

### Aging Algorithm:
For each process waiting in the ready queue:
$$\Delta W_{ready} \leftarrow \Delta W_{ready} + 1$$
When $\Delta W_{ready} \equiv 0 \pmod{K_{interval}}$:
$$PR_{effective} = \max\left(PR_{min}, PR_{effective} - \text{boost}\right)$$
*(Note: A lower numeric value indicates higher priority).*

The configured priority ($PR_{configured}$) remains immutable for auditability, while $PR_{effective}$ is dynamically used by the scheduler. When $W_i \ge \text{threshold}$, the starvation monitor flags the process with a `LONG WAIT DETECTED` alert.

---

## 6. Experimental Methodology & Metric Verification

All experimental comparisons in the **Experiment Lab** and **Compare All** tabs evaluate identical workload copies under controlled configurations:
- Deterministic random generation using pseudo-random seeds.
- Aggregation across multi-workload iterations avoids ratio averaging fallacies:
  $$\text{Aggregated Throughput} = \frac{\sum_{k=1}^M N_k}{\sum_{k=1}^M T_k}$$
- Automated invariant assertion ensures that:
  $$T_{total} = T_{busy} + T_{idle} + T_{cs}$$
  and every process burst is accounted for with zero leakage.
