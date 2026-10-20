# CPU Scheduling Studio

> **An Interactive Operating System CPU Scheduling Simulation Laboratory & Comparative Analysis Suite**  
> *BTech Computer Engineering Mini Project*

---

## 1. Project Overview & Problem Statement

CPU scheduling is the core operational mechanism of modern multiprogrammed operating systems. It governs how the CPU allocates finite execution time across competing processes to maximize throughput, minimize waiting and turnaround times, avoid starvation, and maintain system responsiveness.

However, students and practitioners often struggle to conceptualize:
- How scheduling decisions are made at discrete clock transitions.
- Subtle trade-offs between preemptive and non-preemptive policies.
- The real impact of hardware overhead, such as context-switch costs.
- Why algorithms like Round Robin or MLFQ behave differently under varying time quanta.
- How starvation manifests and how mechanisms like Priority Aging remediate it.

**CPU Scheduling Studio** addresses this challenge by providing a professional, mathematically verified, interactive, and experimental desktop simulation platform built in Java Swing.

---

## 2. Key Features

- **10 Scheduling Algorithms**: Full, verified implementations of standard, preemptive, and multi-level scheduling algorithms.
- **Dynamic Gantt Chart**: Smooth animated execution timeline with arrival markers, idle intervals, context switches, and vector playhead cursor.
- **Live CPU & Queue Telemetry**: Real-time inspection of CPU state and ready-queue processes at every clock tick.
- **Decision Explanation Engine**: Dedicated *Decision Log* explaining *why* the scheduler dispatched, preempted, demoted, or completed each process, synchronized with the Gantt timeline.
- **Context-Switch Overhead Simulation**: Configurable overhead (0 to 5 time units) that realistically charges time to direct process-to-process transitions without corrupting burst counts.
- **Priority Aging & Starvation Monitor**: Dynamically boosts effective priorities of waiting tasks and warns of prolonged wait periods (`LONG WAIT DETECTED`).
- **Workload Experiment Lab**: Batch evaluation of workloads across 6 built-in presets (*Short Jobs*, *Long Jobs*, *Mixed*, *Staggered*, *Priority Conflict*, *Heavy Contention*) running asynchronously via `SwingWorker` with multi-criteria objective optimization.
- **Advanced Comparison Dashboard**: Side-by-side Gantt charts, grouped bar charts, leaderboard sorting, starvation flags, and Jain's Fairness Index computation.
- **Data Import & Comprehensive Reports**: Robust CSV workload importer with RFC 4180-compliant quote parsing and validation; structured CSV export; self-contained, offline-ready HTML report with embedded SVG Gantt chart.

---

## 3. Implemented Scheduling Algorithms

| # | Algorithm Name | Policy Type | Quantum / Allotment | Priority Aging | Description |
|---|---|---|---|---|---|
| 1 | **FCFS** | Non-preemptive | N/A | No | First Come First Served: Dispatches processes strictly by arrival order. Prone to the *convoy effect*. |
| 2 | **SJF** | Non-preemptive | N/A | No | Shortest Job First: Selects process with smallest initial CPU burst. Minimizes average waiting time among non-preemptive policies. |
| 3 | **SRTF** | Preemptive | N/A | No | Shortest Remaining Time First (Preemptive SJF): A newly arrived process with shorter remaining burst preempts the running process. |
| 4 | **LJF** | Non-preemptive | N/A | No | Longest Job First: Dispatches largest initial burst first; useful to illustrate high turnaround times. |
| 5 | **LRTF** | Preemptive | N/A | No | Longest Remaining Time First (Preemptive LJF): Keeps long jobs running in balanced lockstep by preempting when remaining burst is tied or exceeded. |
| 6 | **HRRN** | Non-preemptive | N/A | No | Highest Response Ratio Next: Dispatches by $RR = \frac{W + BT}{BT}$. As wait time grows, ratio climbs, preventing starvation. |
| 7 | **Priority (NP)** | Non-preemptive | N/A | Yes | Non-preemptive Priority: Lower numeric value = higher priority. Runs highest priority to completion. |
| 8 | **Priority (P)** | Preemptive | N/A | Yes | Preemptive Priority: Higher-priority arrival immediately preempts a lower-priority running process. |
| 9 | **Round Robin (RR)** | Preemptive | Configurable $Q$ | No | Time-sliced FIFO scheduling. When quantum expires, process is re-enqueued at tail. |
| 10 | **MLFQ** | Preemptive | 3-Level Multi-Queue | No | Queue 0 ($Q$), Queue 1 ($2Q$), Queue 2 (FCFS). Demotes CPU-bound tasks. Preserves remaining quantum on preemption! |

---

## 4. Mathematical Invariants & Metric Definitions

The simulation engine strictly verifies the following relations for every completed process and simulation run:

$$Turnaround\ Time\ (TAT) = Completion\ Time\ (CT) - Arrival\ Time\ (AT)$$
$$Waiting\ Time\ (WT) = Turnaround\ Time\ (TAT) - Burst\ Time\ (BT)$$
$$Response\ Time\ (RT) = First\ Start\ Time - Arrival\ Time\ (AT)$$

### Global Invariants:
1. $Total\ Elapsed\ Time = Useful\ CPU\ Time + Total\ Idle\ Time + Context\ Switch\ Time$
2. $Useful\ CPU\ Time = \sum_{i=1}^N Burst\ Time_i$
3. $Useful\ CPU\ Utilization = \frac{Useful\ CPU\ Time}{Total\ Elapsed\ Time} \times 100\%$
4. $Throughput = \frac{N}{Total\ Elapsed\ Time}$ (processes per time unit)
5. **Jain's Fairness Index**:
   $$J = \frac{\left(\sum_{i=1}^n x_i\right)^2}{n \cdot \sum_{i=1}^n x_i^2}$$
   where $x_i = \frac{BT_i}{TAT_i}$ represents the service progress ratio. $J \in [1/n, 1.0]$, where $1.0$ indicates perfect fairness.

---

## 5. Technology Stack & Architecture

- **Language**: Java 17+ (fully tested on Java 25 LTS).
- **GUI Framework**: Java Swing (`JFrame`, `CardLayout`, `GridBagLayout`, `Custom Components`) with 2D vector graphics rendering (`RenderingHints`, `Graphics2D`).
- **Asynchronous Execution**: `SwingWorker` background threads for compute-intensive batch experiment evaluations.
- **Build System**: Dual-mode support — Standalone zero-dependency build scripts (`run.bat`, `build.bat`) and Apache Maven (`pom.xml`).
- **Test Framework**: Automated test harness (`SchedulerCorrectnessTest`, `TestRunner`) covering 27 test scenarios with 100% pass rate.

### Modular Project Structure
```text
CPUSchedulingStudio/
├── pom.xml                               # Standard Maven project descriptor
├── build.bat                             # Automated build, test, and JAR packaging script
├── run.bat                               # One-click compilation and launcher script
├── README.md                             # Comprehensive project documentation
├── CPUSchedulingSimulator/               # Legacy backward-compatible entry point
│   └── Main.java                         # Delegates to cpuscheduler.ui.MainFrame
├── src/
│   ├── main/
│   │   └── java/
│   │       └── cpuscheduler/
│   │           ├── Main.java             # Modern application entry point
│   │           ├── model/                # Domain models (ProcessModel, SimulationResult, etc.)
│   │           ├── scheduler/            # Scheduler interface, factory, and enum
│   │           │   └── algorithms/       # 10 modular algorithm implementations
│   │           ├── simulation/           # SimulationEngine, invariant verification
│   │           ├── experiment/           # WorkloadGenerator, WorkloadPreset, ExperimentLab
│   │           ├── export/               # CsvImporter, CsvExporter, HtmlReportExporter
│   │           └── ui/                   # Dark-themed custom Swing GUI components
│   └── test/
│       └── java/
│           └── cpuscheduler/
│               ├── SchedulerCorrectnessTest.java # 27 automated test scenarios
│               └── TestRunner.java       # Standalone test runner
├── samples/                              # 8 curated CSV benchmark workloads
│   ├── 01_basic_scheduling.csv
│   ├── 02_preemption_srtf.csv
│   ├── 03_priority_conflicts.csv
│   ├── 04_round_robin_rotation.csv
│   ├── 05_mlfq_transitions.csv
│   ├── 06_long_waiting_starvation.csv
│   ├── 07_context_switch_overhead.csv
│   └── 08_priority_aging.csv
└── docs/                                 # Academic and engineering reports
    ├── TECHNICAL_DOCS.md                 # Detailed architecture and state transition manual
    └── QA_REPORT.md                      # Audit findings, verification record, and test report
```

---

## 6. Installation & Execution Guide

### Prerequisites
- Java Development Kit (JDK) 17 or higher (tested with Oracle JDK 25).
- Ensure `javac` and `java` are available in your system `PATH`.

### Quick Launch (Windows)
Double-click `run.bat` or run in terminal:
```cmd
.\run.bat
```

### Complete Build & Test
To clean, compile, run all 27 automated correctness tests, and package an executable JAR:
```cmd
.\build.bat
```
The executable JAR will be generated at:
```cmd
target\cpu-scheduling-studio.jar
```
Launch directly using:
```cmd
java -jar target\cpu-scheduling-studio.jar
```

### Running Automated Tests
```cmd
java -cp target\classes cpuscheduler.TestRunner
```

---

## 7. Sample Workloads Guide

Curated test workloads are located in `samples/`:
1. `01_basic_scheduling.csv`: Basic 4-process set for initial FCFS/SJF walkthrough.
2. `02_preemption_srtf.csv`: Illustrates preemption when a short process arrives at $t=2$ and $t=3$.
3. `03_priority_conflicts.csv`: Demonstrates static priority inversion and convoy delays.
4. `04_round_robin_rotation.csv`: Demonstrates cyclic time-sliced execution among equal-length processes.
5. `05_mlfq_transitions.csv`: Highlights demotion from Queue 0 to Queue 1 and Queue 2.
6. `06_long_waiting_starvation.csv`: Triggers starvation warning (`LONG WAIT DETECTED`) for long-running lower priority tasks.
7. `07_context_switch_overhead.csv`: Highlights degradation in throughput when context-switch cost $CS > 0$.
8. `08_priority_aging.csv`: Compares priority execution without aging vs. rescue when aging is enabled.

---

## 8. Exporting Results & Reports

- **CSV Export**: Click `Export CSV` on the Process Results card to save a formatted `.csv` file with full per-process and summary metrics.
- **HTML Report**: Click `HTML Report` on the Process Results card to generate a self-contained, offline-compatible report featuring:
  - KPI summary badges.
  - Formatted process outcome tables.
  - Embedded vector SVG Gantt chart.
  - Decision Log event highlights.
  - Comparative benchmark across all 10 scheduling policies.
  - Print-friendly layout for college project documentation.

---

## 9. Academic Limitations & Future Scope

### Limitations
- The discrete simulator currently operates on integer time units.
- Memory management (paging, swapping) is not simulated.

### Future Scope
- Multi-core CPU scheduling (affinity, load balancing across 2–8 cores).
- Multi-burst CPU and I/O alternating wait queues.
- Real-time scheduling policies (Earliest Deadline First - EDF, Rate Monotonic - RMS).
