# CPU Scheduling Studio — React Web Frontend

A modern, responsive, dark-themed web interface for the **CPU Scheduling Studio**, featuring client-side simulation of all 10 CPU scheduling algorithms, real-time Gantt charts, live CPU/queue state machine, decision explanation logging, comparison dashboard, and experiment lab.

---

## ⚡ Features

- **10 Core Scheduling Algorithms**: FCFS, SJF, SRTF, LJF, LRTF, HRRN, Priority (NP/P), Round Robin, MLFQ.
- **Client-Side Simulation Engine**: Pure TypeScript simulation engine with zero network latency or backend dependency.
- **Interactive Gantt Chart**: Color-coded process blocks, context-switch intervals, idle gaps, timeline scrub, play/pause, step-by-step playback, and speed controls.
- **Live CPU & Ready Queue View**: Real-time visualization of the CPU state and ordered queue progression at every clock tick.
- **Decision Log**: Complete educational event trace explaining why each process was selected, preempted, demoted, or aged.
- **Compare All Dashboard**: Evaluates all 10 algorithms against identical process workloads with side-by-side mini-Gantt timelines and Jain's Fairness Index.
- **Workload Experiment Lab**: 6 empirical presets (Short Jobs, Long Jobs, Mixed, Staggered, Priority Conflict, Heavy Contention) with multi-objective goal ranking.
- **RFC 4180 CSV Import/Export**: Import custom workload files and export execution results.
- **Offline HTML Report Generator**: Generates print-ready self-contained HTML reports with vector SVG Gantt charts.

---

## 🚀 Getting Started

### Prerequisites
- Node.js (v18 or higher recommended)
- npm

### Installation & Development
```bash
cd frontend
npm install
npm run dev
```

The application will be live at:
👉 `http://localhost:3000`

### Production Build
```bash
npm run build
npm run preview
```
