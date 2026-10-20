# CPU Scheduling Studio

A modern, high-performance, dark-themed Java desktop application for simulating, visualizing, and analyzing CPU scheduling algorithms in real time.

![Java](https://img.shields.io/badge/Java-11%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/UI-Java%20Swing%20%2F%20AWT-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

---

## ⚡ Features

- **Interactive Gantt Chart**: Smoothly rendered and animated execution timeline with process color-coding.
- **Live CPU & Queue Animation**: Watch processes move in real time through the ready queue into the CPU.
- **Metric Cards**: Dynamic KPI displays for:
  - Average Waiting Time (AWT)
  - Average Turnaround Time (ATAT)
  - Average Response Time (ART)
  - CPU Utilization Percentage
  - System Throughput
- **"Compare All" Dashboard**: Run every supported scheduling algorithm against the identical workload to compare relative efficiency side-by-side.
- **Workload Generator**: Generate random workloads or customize arrival times, burst times, and priorities directly in the table.
- **Data Export**: Export process run details and summary metrics to CSV files.

---

## 🧠 Supported Scheduling Algorithms

1. **FCFS** (First-Come, First-Served)
2. **SJF** (Shortest Job First - Non-Preemptive)
3. **SRTF** (Shortest Remaining Time First - Preemptive)
4. **LJF** (Longest Job First - Non-Preemptive)
5. **LRTF** (Longest Remaining Time First - Preemptive)
6. **Priority Scheduling** (Non-Preemptive & Preemptive)
7. **Round Robin (RR)** (Configurable Time Quantum)
8. **HRRN** (Highest Response Ratio Next)
9. **MLFQ** (Multilevel Feedback Queue)

---

## 🚀 Getting Started

### Prerequisites
- **Java SE Development Kit (JDK)**: Version 11 or higher (tested on JDK 25).

### Running on Windows
Simply run the included batch script:
```bat
.\run.bat
```

### Manual Compilation & Execution
Navigate into the simulator directory and run:
```bash
cd CPUSchedulingSimulator
javac -encoding UTF-8 Main.java
java Main
```

---

## 📁 Project Structure

```text
CPU-Scheduling-Simulator/
├── CPUSchedulingSimulator/
│   └── Main.java       # Complete application source code & UI
├── run.bat             # Convenient Windows launcher script
├── .gitignore          # Git ignore rules for bytecode & binaries
└── README.md           # Documentation and usage guide
```

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
