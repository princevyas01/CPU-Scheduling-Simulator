export interface ProcessInput {
  id: string;
  arrivalTime: number;
  burstTime: number;
  priority: number;
}

export interface ProcessResult {
  id: string;
  arrivalTime: number;
  burstTime: number;
  configuredPriority: number;
  effectivePriority: number;
  firstStartTime: number;
  completionTime: number;
  turnaroundTime: number;
  waitingTime: number;
  responseTime: number;
  agingEventsCount: number;
  color: string;
}

export type GanttType = 'PROCESS' | 'IDLE' | 'CONTEXT_SWITCH';

export interface GanttSegment {
  type: GanttType;
  processIndex: number;
  processId: string;
  startTime: number;
  endTime: number;
  color: string;
}

export type EventType =
  | 'PROCESS_ARRIVED'
  | 'PROCESS_ENQUEUED'
  | 'PROCESS_DISPATCHED'
  | 'PROCESS_PREEMPTED'
  | 'QUANTUM_EXPIRED'
  | 'PROCESS_DEMOTED'
  | 'PRIORITY_AGED'
  | 'CONTEXT_SWITCH_STARTED'
  | 'CONTEXT_SWITCH_COMPLETED'
  | 'PROCESS_COMPLETED'
  | 'CPU_IDLE';

export interface SchedulingEvent {
  time: number;
  eventType: EventType;
  processId: string;
  runningProcessId: string;
  readyProcesses: string;
  decisionReason: string;
  relevantValues: string;
  outcome: string;
}

export interface SimulationConfig {
  quantum: number;
  contextSwitchOverhead: number;
  agingEnabled: boolean;
  agingInterval: number;
  priorityBoost: number;
  minPriority: number;
  starvationThreshold: number;
}

export interface SimulationResult {
  algorithmName: string;
  config: SimulationConfig;
  processes: ProcessResult[];
  segments: GanttSegment[];
  timeline: number[];
  queueSnapshots: string[][];
  events: SchedulingEvent[];
  totalTime: number;
  busyTime: number;
  idleTime: number;
  contextSwitchTime: number;
  contextSwitchCount: number;
  avgWaitingTime: number;
  avgTurnaroundTime: number;
  avgResponseTime: number;
  cpuUtilization: number;
  idlePercentage: number;
  contextSwitchPercentage: number;
  throughput: number;
  minWaitingTime: number;
  maxWaitingTime: number;
  medianWaitingTime: number;
  waitingTimeVariance: number;
  waitingTimeStdDev: number;
  jainsFairnessIndex: number;
  starvationWarnings: string[];
}

export type AlgorithmId =
  | 'FCFS'
  | 'SJF'
  | 'SRTF'
  | 'LJF'
  | 'LRTF'
  | 'HRRN'
  | 'PRIORITY_NP'
  | 'PRIORITY_P'
  | 'RR'
  | 'MLFQ';

export interface AlgorithmMetadata {
  id: AlgorithmId;
  name: string;
  shortName: string;
  isPreemptive: boolean;
  requiresQuantum: boolean;
  supportsAging: boolean;
  description: string;
}

export const ALGORITHMS: AlgorithmMetadata[] = [
  {
    id: 'FCFS',
    name: 'FCFS (First-Come, First-Served)',
    shortName: 'FCFS',
    isPreemptive: false,
    requiresQuantum: false,
    supportsAging: false,
    description: 'First Come First Served. Processes execute strictly in order of arrival, non-preemptively. Suffers from the convoy effect when long processes arrive early.'
  },
  {
    id: 'SJF',
    name: 'SJF (Shortest Job First - Non-preemptive)',
    shortName: 'SJF NP',
    isPreemptive: false,
    requiresQuantum: false,
    supportsAging: false,
    description: 'Shortest Job First. Selects the arrived process with the smallest initial CPU burst. Minimizes average waiting time among non-preemptive policies.'
  },
  {
    id: 'SRTF',
    name: 'SRTF (Shortest Remaining Time First - Preemptive)',
    shortName: 'SRTF P',
    isPreemptive: true,
    requiresQuantum: false,
    supportsAging: false,
    description: 'Preemptive SJF. At each scheduling decision, the process with the least remaining CPU burst executes. Preempts currently executing job if a shorter job arrives.'
  },
  {
    id: 'LJF',
    name: 'LJF (Longest Job First - Non-preemptive)',
    shortName: 'LJF NP',
    isPreemptive: false,
    requiresQuantum: false,
    supportsAging: false,
    description: 'Longest Job First. Selects the arrived process with the largest initial burst. Useful as an educational counterexample illustrating extreme convoy delay.'
  },
  {
    id: 'LRTF',
    name: 'LRTF (Longest Remaining Time First - Preemptive)',
    shortName: 'LRTF P',
    isPreemptive: true,
    requiresQuantum: false,
    supportsAging: false,
    description: 'Preemptive LJF. At every decision tick, runs the job with the longest remaining burst. Interleaves equally long processes to maintain balanced progress.'
  },
  {
    id: 'HRRN',
    name: 'HRRN (Highest Response Ratio Next)',
    shortName: 'HRRN',
    isPreemptive: false,
    requiresQuantum: false,
    supportsAging: false,
    description: 'Highest Response Ratio Next. Prioritizes processes using Response Ratio = (Waiting + Burst) / Burst. Naturally prevents starvation as waiting time grows.'
  },
  {
    id: 'PRIORITY_NP',
    name: 'Priority Scheduling (Non-preemptive)',
    shortName: 'Priority NP',
    isPreemptive: false,
    requiresQuantum: false,
    supportsAging: true,
    description: 'Non-preemptive priority. Lower numeric value represents higher priority. Runs highest-priority arrived process to completion.'
  },
  {
    id: 'PRIORITY_P',
    name: 'Priority Scheduling (Preemptive)',
    shortName: 'Priority P',
    isPreemptive: true,
    requiresQuantum: false,
    supportsAging: true,
    description: 'Preemptive priority. A newly arrived process with a higher priority immediately preempts the currently running job.'
  },
  {
    id: 'RR',
    name: 'Round Robin (RR)',
    shortName: 'Round Robin',
    isPreemptive: true,
    requiresQuantum: true,
    supportsAging: false,
    description: 'Round Robin. Allocates a fixed time quantum per process. When quantum expires, process is placed at the tail of the ready queue.'
  },
  {
    id: 'MLFQ',
    name: 'Multilevel Feedback Queue (MLFQ)',
    shortName: 'MLFQ',
    isPreemptive: true,
    requiresQuantum: true,
    supportsAging: false,
    description: 'Multilevel Feedback Queue with 3 priority tiers: Q0 (quantum q), Q1 (quantum 2q), Q2 (FCFS). Preserves remaining quantum when preempted by higher queues.'
  }
];

export const PROCESS_PALETTE = [
  '#7c5cff', // purple
  '#22b8cf', // teal
  '#ff8a5c', // orange
  '#4ade80', // green
  '#ec62aa', // pink
  '#f0b93c', // gold
  '#5a96ff', // sky
  '#be78ff', // violet
  '#ff6e6e', // red
  '#3cd2b4', // mint
  '#aacc46', // lime
  '#ffa0c8'  // light pink
];
