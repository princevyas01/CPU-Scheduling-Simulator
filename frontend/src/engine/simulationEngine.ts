import {
  ProcessInput,
  ProcessResult,
  GanttSegment,
  SchedulingEvent,
  SimulationConfig,
  SimulationResult,
  AlgorithmId,
  PROCESS_PALETTE
} from '../types/scheduler';

interface InternalProcess {
  index: number;
  id: string;
  arrivalTime: number;
  burstTime: number;
  configuredPriority: number;
  effectivePriority: number;
  remainingBurst: number;
  firstStartTime: number;
  completionTime: number;
  turnaroundTime: number;
  waitingTime: number;
  responseTime: number;
  currentQueueLevel: number;
  remainingQuantum: number;
  readyWaitTime: number;
  agingEventsCount: number;
  completed: boolean;
  color: string;
}

export function runSimulation(
  algorithm: AlgorithmId,
  inputs: ProcessInput[],
  config: SimulationConfig
): SimulationResult {
  if (!inputs || inputs.length === 0) {
    throw new Error('Workload cannot be empty.');
  }

  // Validate duplicate IDs
  const seenIds = new Set<string>();
  for (const p of inputs) {
    if (seenIds.has(p.id)) {
      throw new Error(`Duplicate process ID detected: ${p.id}`);
    }
    seenIds.add(p.id);
    if (p.burstTime <= 0) {
      throw new Error(`Process ${p.id} burst time must be strictly positive.`);
    }
    if (p.arrivalTime < 0) {
      throw new Error(`Process ${p.id} arrival time cannot be negative.`);
    }
  }

  const n = inputs.length;
  const procs: InternalProcess[] = inputs.map((p, idx) => ({
    index: idx,
    id: p.id,
    arrivalTime: p.arrivalTime,
    burstTime: p.burstTime,
    configuredPriority: p.priority,
    effectivePriority: p.priority,
    remainingBurst: p.burstTime,
    firstStartTime: -1,
    completionTime: 0,
    turnaroundTime: 0,
    waitingTime: 0,
    responseTime: 0,
    currentQueueLevel: 0,
    remainingQuantum: 0,
    readyWaitTime: 0,
    agingEventsCount: 0,
    completed: false,
    color: PROCESS_PALETTE[idx % PROCESS_PALETTE.length]
  }));

  const byArrival = [...procs].sort((a, b) =>
    a.arrivalTime !== b.arrivalTime ? a.arrivalTime - b.arrivalTime : a.index - b.index
  );

  const readyQueue: InternalProcess[] = [];
  const mlfqQueues: InternalProcess[][] = [[], [], []];

  const timeline: number[] = [];
  const queueSnapshots: string[][] = [];
  const events: SchedulingEvent[] = [];
  const segments: GanttSegment[] = [];

  let currentTime = 0;
  let completedCount = 0;
  let runningProcess: InternalProcess | null = null;
  let lastRanProcess: InternalProcess | null = null;
  let contextSwitchRemaining = 0;
  let pendingProcessToRun: InternalProcess | null = null;

  const arrivedSet = new Set<number>();
  const MAX_TICKS = 50000;

  function admitArrivals(t: number) {
    for (const p of byArrival) {
      if (!arrivedSet.has(p.index) && p.arrivalTime <= t) {
        arrivedSet.add(p.index);
        p.readyWaitTime = 0;

        if (algorithm === 'MLFQ') {
          p.currentQueueLevel = 0;
          p.remainingQuantum = config.quantum;
          mlfqQueues[0].push(p);
        } else if (algorithm === 'RR') {
          p.remainingQuantum = config.quantum;
          readyQueue.push(p);
        } else {
          readyQueue.push(p);
        }

        events.push({
          time: t,
          eventType: 'PROCESS_ARRIVED',
          processId: p.id,
          runningProcessId: runningProcess ? runningProcess.id : 'IDLE',
          readyProcesses: formatReadyQueue(),
          decisionReason: `Process ${p.id} arrived at clock tick ${t} (Burst=${p.burstTime}, Priority=${p.configuredPriority}).`,
          relevantValues: `AT=${p.arrivalTime}, BT=${p.burstTime}, PR=${p.configuredPriority}`,
          outcome: 'Admitted to Ready Queue'
        });
      }
    }
  }

  function formatReadyQueue(): string {
    if (algorithm === 'MLFQ') {
      const q0 = mlfqQueues[0].map(p => p.id).join(', ');
      const q1 = mlfqQueues[1].map(p => p.id).join(', ');
      const q2 = mlfqQueues[2].map(p => p.id).join(', ');
      return `Q0:[${q0}] Q1:[${q1}] Q2:[${q2}]`;
    }
    return `[${readyQueue.map(p => p.id).join(', ')}]`;
  }

  function getSnapshot(): string[] {
    if (algorithm === 'MLFQ') {
      return [...mlfqQueues[0], ...mlfqQueues[1], ...mlfqQueues[2]].map(p => p.id);
    }
    return readyQueue.map(p => p.id);
  }

  function applyPriorityAging(t: number) {
    if (!config.agingEnabled) return;
    for (const p of readyQueue) {
      p.readyWaitTime++;
      if (p.readyWaitTime >= config.agingInterval) {
        if (p.effectivePriority > config.minPriority) {
          const oldPr = p.effectivePriority;
          p.effectivePriority = Math.max(config.minPriority, p.effectivePriority - config.priorityBoost);
          p.agingEventsCount++;
          p.readyWaitTime = 0;

          events.push({
            time: t,
            eventType: 'PRIORITY_AGED',
            processId: p.id,
            runningProcessId: runningProcess ? runningProcess.id : 'IDLE',
            readyProcesses: formatReadyQueue(),
            decisionReason: `Process ${p.id} waited ${config.agingInterval} units in ready queue. Priority boosted from ${oldPr} to ${p.effectivePriority}.`,
            relevantValues: `EffectivePriority=${p.effectivePriority}, ConfiguredPriority=${p.configuredPriority}`,
            outcome: 'Priority Promoted'
          });
        }
      }
    }
  }

  function selectCandidate(t: number): InternalProcess | null {
    if (algorithm === 'RR') {
      return readyQueue.length > 0 ? readyQueue[0] : null;
    }

    if (algorithm === 'MLFQ') {
      for (let l = 0; l < 3; l++) {
        if (mlfqQueues[l].length > 0) return mlfqQueues[l][0];
      }
      return null;
    }

    if (readyQueue.length === 0) return null;

    let best = readyQueue[0];
    for (let i = 1; i < readyQueue.length; i++) {
      const candidate = readyQueue[i];
      if (compareCandidates(algorithm, candidate, best, t) < 0) {
        best = candidate;
      }
    }
    return best;
  }

  function compareCandidates(alg: AlgorithmId, a: InternalProcess, b: InternalProcess, t: number): number {
    switch (alg) {
      case 'FCFS':
        return a.arrivalTime !== b.arrivalTime ? a.arrivalTime - b.arrivalTime : a.index - b.index;
      case 'SJF':
        return a.burstTime !== b.burstTime
          ? a.burstTime - b.burstTime
          : a.arrivalTime !== b.arrivalTime
          ? a.arrivalTime - b.arrivalTime
          : a.index - b.index;
      case 'SRTF':
        return a.remainingBurst !== b.remainingBurst
          ? a.remainingBurst - b.remainingBurst
          : a.arrivalTime !== b.arrivalTime
          ? a.arrivalTime - b.arrivalTime
          : a.index - b.index;
      case 'LJF':
        return a.burstTime !== b.burstTime
          ? b.burstTime - a.burstTime
          : a.arrivalTime !== b.arrivalTime
          ? a.arrivalTime - b.arrivalTime
          : a.index - b.index;
      case 'LRTF':
        return a.remainingBurst !== b.remainingBurst
          ? b.remainingBurst - a.remainingBurst
          : a.arrivalTime !== b.arrivalTime
          ? a.arrivalTime - b.arrivalTime
          : a.index - b.index;
      case 'HRRN': {
        const wtA = t - a.arrivalTime;
        const wtB = t - b.arrivalTime;
        const rrA = (wtA + a.burstTime) / a.burstTime;
        const rrB = (wtB + b.burstTime) / b.burstTime;
        if (Math.abs(rrA - rrB) > 1e-9) return rrB > rrA ? 1 : -1;
        return a.arrivalTime !== b.arrivalTime ? a.arrivalTime - b.arrivalTime : a.index - b.index;
      }
      case 'PRIORITY_NP':
      case 'PRIORITY_P': {
        const prA = config.agingEnabled ? a.effectivePriority : a.configuredPriority;
        const prB = config.agingEnabled ? b.effectivePriority : b.configuredPriority;
        if (prA !== prB) return prA - prB; // lower number = higher priority
        return a.arrivalTime !== b.arrivalTime ? a.arrivalTime - b.arrivalTime : a.index - b.index;
      }
      default:
        return a.index - b.index;
    }
  }

  // Discrete Simulation Loop
  while (completedCount < n && currentTime < MAX_TICKS) {
    admitArrivals(currentTime);
    applyPriorityAging(currentTime);

    // If context switch is active
    if (contextSwitchRemaining > 0) {
      timeline.push(-2); // -2 represents Context Switch
      queueSnapshots.push(getSnapshot());
      contextSwitchRemaining--;

      if (contextSwitchRemaining === 0 && pendingProcessToRun) {
        runningProcess = pendingProcessToRun;
        pendingProcessToRun = null;

        events.push({
          time: currentTime + 1,
          eventType: 'CONTEXT_SWITCH_COMPLETED',
          processId: runningProcess.id,
          runningProcessId: runningProcess.id,
          readyProcesses: formatReadyQueue(),
          decisionReason: `Context switch completed. CPU dispatched to process ${runningProcess.id}.`,
          relevantValues: `CS Overhead=${config.contextSwitchOverhead}`,
          outcome: 'CPU Dispatched'
        });
      }

      currentTime++;
      continue;
    }

    // Select candidate
    const isPreemptive =
      algorithm === 'SRTF' ||
      algorithm === 'LRTF' ||
      algorithm === 'PRIORITY_P' ||
      algorithm === 'RR' ||
      algorithm === 'MLFQ';

    if (algorithm === 'RR') {
      if (runningProcess && runningProcess.remainingQuantum <= 0) {
        // Quantum expired
        readyQueue.push(runningProcess);
        events.push({
          time: currentTime,
          eventType: 'QUANTUM_EXPIRED',
          processId: runningProcess.id,
          runningProcessId: runningProcess.id,
          readyProcesses: formatReadyQueue(),
          decisionReason: `Process ${runningProcess.id} time quantum (${config.quantum}) expired. Re-enqueued at tail.`,
          relevantValues: `Quantum=${config.quantum}, RemainingBurst=${runningProcess.remainingBurst}`,
          outcome: 'Re-enqueued at Tail'
        });
        runningProcess = null;
      }

      if (!runningProcess && readyQueue.length > 0) {
        const next = readyQueue.shift()!;
        next.remainingQuantum = config.quantum;

        if (lastRanProcess && lastRanProcess.index !== next.index && config.contextSwitchOverhead > 0) {
          contextSwitchRemaining = config.contextSwitchOverhead;
          pendingProcessToRun = next;
          lastRanProcess = next;

          events.push({
            time: currentTime,
            eventType: 'CONTEXT_SWITCH_STARTED',
            processId: next.id,
            runningProcessId: 'CS',
            readyProcesses: formatReadyQueue(),
            decisionReason: `Context switch initiated from ${lastRanProcess.id} to ${next.id} (${config.contextSwitchOverhead} ticks).`,
            relevantValues: `Overhead=${config.contextSwitchOverhead}`,
            outcome: 'Context Switching'
          });

          timeline.push(-2);
          queueSnapshots.push(getSnapshot());
          contextSwitchRemaining--;
          currentTime++;
          continue;
        }

        runningProcess = next;
        lastRanProcess = next;
      }
    } else if (algorithm === 'MLFQ') {
      if (runningProcess) {
        // Check preemption by higher level queue
        let higherWork = false;
        for (let l = 0; l < runningProcess.currentQueueLevel; l++) {
          if (mlfqQueues[l].length > 0) {
            higherWork = true;
            break;
          }
        }

        if (runningProcess.remainingQuantum <= 0) {
          // Demote
          const newLevel = Math.min(2, runningProcess.currentQueueLevel + 1);
          runningProcess.currentQueueLevel = newLevel;
          runningProcess.remainingQuantum = newLevel === 2 ? 999999 : config.quantum << newLevel;
          mlfqQueues[newLevel].push(runningProcess);

          events.push({
            time: currentTime,
            eventType: 'PROCESS_DEMOTED',
            processId: runningProcess.id,
            runningProcessId: runningProcess.id,
            readyProcesses: formatReadyQueue(),
            decisionReason: `Process ${runningProcess.id} exhausted quantum allotment. Demoted to Q${newLevel}.`,
            relevantValues: `Level=${newLevel}, Quantum=${runningProcess.remainingQuantum}`,
            outcome: `Demoted to Q${newLevel}`
          });
          runningProcess = null;
        } else if (higherWork) {
          // Preempted by higher queue arrival: PRESERVE remaining quantum!
          mlfqQueues[runningProcess.currentQueueLevel].push(runningProcess);
          events.push({
            time: currentTime,
            eventType: 'PROCESS_PREEMPTED',
            processId: runningProcess.id,
            runningProcessId: runningProcess.id,
            readyProcesses: formatReadyQueue(),
            decisionReason: `Process ${runningProcess.id} preempted by higher priority queue. Remaining quantum ${runningProcess.remainingQuantum} is PRESERVED.`,
            relevantValues: `Level=Q${runningProcess.currentQueueLevel}, SavedQuantum=${runningProcess.remainingQuantum}`,
            outcome: 'Preempted and Saved'
          });
          runningProcess = null;
        }
      }

      if (!runningProcess) {
        for (let l = 0; l < 3; l++) {
          if (mlfqQueues[l].length > 0) {
            const next = mlfqQueues[l].shift()!;
            if (next.remainingQuantum <= 0) {
              next.remainingQuantum = l === 2 ? 999999 : config.quantum << l;
            }

            if (lastRanProcess && lastRanProcess.index !== next.index && config.contextSwitchOverhead > 0) {
              contextSwitchRemaining = config.contextSwitchOverhead;
              pendingProcessToRun = next;
              lastRanProcess = next;
              timeline.push(-2);
              queueSnapshots.push(getSnapshot());
              contextSwitchRemaining--;
              currentTime++;
              continue;
            }

            runningProcess = next;
            lastRanProcess = next;
            break;
          }
        }
      }
    } else {
      // General Ready Queue (FCFS, SJF, SRTF, LJF, LRTF, HRRN, Priority NP, Priority P)
      const best = selectCandidate(currentTime);

      if (isPreemptive && runningProcess && best && compareCandidates(algorithm, best, runningProcess, currentTime) < 0) {
        // Preemption
        const preempted = runningProcess;
        readyQueue.push(preempted);
        const idx = readyQueue.indexOf(best);
        if (idx !== -1) readyQueue.splice(idx, 1);

        events.push({
          time: currentTime,
          eventType: 'PROCESS_PREEMPTED',
          processId: preempted.id,
          runningProcessId: best.id,
          readyProcesses: formatReadyQueue(),
          decisionReason: `Process ${best.id} preempts ${preempted.id} under ${algorithm}.`,
          relevantValues: `NewJob=${best.id}, Preempted=${preempted.id}`,
          outcome: 'Preempted'
        });

        if (config.contextSwitchOverhead > 0) {
          contextSwitchRemaining = config.contextSwitchOverhead;
          pendingProcessToRun = best;
          lastRanProcess = best;
          runningProcess = null;
          timeline.push(-2);
          queueSnapshots.push(getSnapshot());
          contextSwitchRemaining--;
          currentTime++;
          continue;
        }

        runningProcess = best;
        lastRanProcess = best;
      } else if (!runningProcess && best) {
        const idx = readyQueue.indexOf(best);
        if (idx !== -1) readyQueue.splice(idx, 1);

        if (lastRanProcess && lastRanProcess.index !== best.index && config.contextSwitchOverhead > 0) {
          contextSwitchRemaining = config.contextSwitchOverhead;
          pendingProcessToRun = best;
          lastRanProcess = best;
          timeline.push(-2);
          queueSnapshots.push(getSnapshot());
          contextSwitchRemaining--;
          currentTime++;
          continue;
        }

        runningProcess = best;
        lastRanProcess = best;

        events.push({
          time: currentTime,
          eventType: 'PROCESS_DISPATCHED',
          processId: runningProcess.id,
          runningProcessId: runningProcess.id,
          readyProcesses: formatReadyQueue(),
          decisionReason: `Dispatched process ${runningProcess.id} based on ${algorithm} selection policy.`,
          relevantValues: `AT=${runningProcess.arrivalTime}, BT=${runningProcess.burstTime}, Rem=${runningProcess.remainingBurst}`,
          outcome: 'Dispatched to CPU'
        });
      }
    }

    // Execute 1 tick
    if (runningProcess) {
      if (runningProcess.firstStartTime === -1) {
        runningProcess.firstStartTime = currentTime;
      }

      timeline.push(runningProcess.index);
      queueSnapshots.push(getSnapshot());

      runningProcess.remainingBurst--;
      if (algorithm === 'RR' || algorithm === 'MLFQ') {
        runningProcess.remainingQuantum--;
      }

      if (runningProcess.remainingBurst === 0) {
        runningProcess.completed = true;
        runningProcess.completionTime = currentTime + 1;
        runningProcess.turnaroundTime = runningProcess.completionTime - runningProcess.arrivalTime;
        runningProcess.waitingTime = runningProcess.turnaroundTime - runningProcess.burstTime;
        runningProcess.responseTime = runningProcess.firstStartTime - runningProcess.arrivalTime;
        completedCount++;

        events.push({
          time: currentTime + 1,
          eventType: 'PROCESS_COMPLETED',
          processId: runningProcess.id,
          runningProcessId: runningProcess.id,
          readyProcesses: formatReadyQueue(),
          decisionReason: `Process ${runningProcess.id} completed total CPU burst of ${runningProcess.burstTime}.`,
          relevantValues: `CT=${runningProcess.completionTime}, TAT=${runningProcess.turnaroundTime}, WT=${runningProcess.waitingTime}, RT=${runningProcess.responseTime}`,
          outcome: 'Finished Execution'
        });

        runningProcess = null;
      }
    } else {
      // CPU Idle
      timeline.push(-1);
      queueSnapshots.push(getSnapshot());
      lastRanProcess = null;
    }

    currentTime++;
  }

  // Construct Gantt Segments
  let segStart = 0;
  for (let i = 0; i < timeline.length; i++) {
    if (i === timeline.length - 1 || timeline[i] !== timeline[i + 1]) {
      const code = timeline[i];
      const start = segStart;
      const end = i + 1;
      segStart = i + 1;

      if (code >= 0) {
        const p = procs[code];
        segments.push({
          type: 'PROCESS',
          processIndex: code,
          processId: p.id,
          startTime: start,
          endTime: end,
          color: p.color
        });
      } else if (code === -2) {
        segments.push({
          type: 'CONTEXT_SWITCH',
          processIndex: -2,
          processId: 'CS',
          startTime: start,
          endTime: end,
          color: '#ff6880'
        });
      } else {
        segments.push({
          type: 'IDLE',
          processIndex: -1,
          processId: 'IDLE',
          startTime: start,
          endTime: end,
          color: '#2b3450'
        });
      }
    }
  }

  // Calculate Metrics
  const totalTime = timeline.length;
  let busyTime = 0;
  let idleTime = 0;
  let csTime = 0;
  let csCount = 0;

  for (let i = 0; i < timeline.length; i++) {
    if (timeline[i] >= 0) busyTime++;
    else if (timeline[i] === -2) csTime++;
    else idleTime++;

    if (timeline[i] === -2 && (i === 0 || timeline[i - 1] !== -2)) {
      csCount++;
    }
  }

  let sumWT = 0;
  let sumTAT = 0;
  let sumRT = 0;
  const waitingTimes: number[] = [];

  for (const p of procs) {
    sumWT += p.waitingTime;
    sumTAT += p.turnaroundTime;
    sumRT += p.responseTime;
    waitingTimes.push(p.waitingTime);
  }

  waitingTimes.sort((a, b) => a - b);
  const minWT = waitingTimes[0] || 0;
  const maxWT = waitingTimes[waitingTimes.length - 1] || 0;
  const medianWT =
    waitingTimes.length % 2 === 1
      ? waitingTimes[Math.floor(waitingTimes.length / 2)]
      : (waitingTimes[waitingTimes.length / 2 - 1] + waitingTimes[waitingTimes.length / 2]) / 2;

  const avgWT = sumWT / n;
  const avgTAT = sumTAT / n;
  const avgRT = sumRT / n;

  let varSum = 0;
  for (const wt of waitingTimes) {
    varSum += Math.pow(wt - avgWT, 2);
  }
  const wtVariance = varSum / n;
  const wtStdDev = Math.sqrt(wtVariance);

  // Jain's Fairness Index: (sum x_i)^2 / (n * sum (x_i^2)) using throughput metric or (WT + 1)
  let sumX = 0;
  let sumX2 = 0;
  for (const p of procs) {
    const x = p.burstTime / Math.max(1, p.turnaroundTime); // normalized progress rate
    sumX += x;
    sumX2 += x * x;
  }
  const jainsIndex = sumX2 > 0 ? (sumX * sumX) / (n * sumX2) : 1.0;

  const cpuUtil = totalTime > 0 ? (busyTime / totalTime) * 100 : 0;
  const idlePct = totalTime > 0 ? (idleTime / totalTime) * 100 : 0;
  const csPct = totalTime > 0 ? (csTime / totalTime) * 100 : 0;
  const throughput = totalTime > 0 ? n / totalTime : 0;

  // Starvation Warnings
  const starvationWarnings: string[] = [];
  for (const p of procs) {
    if (p.waitingTime >= config.starvationThreshold) {
      starvationWarnings.push(
        `LONG WAIT DETECTED: Process ${p.id} experienced waiting time of ${p.waitingTime} >= threshold ${config.starvationThreshold}.`
      );
    }
  }

  const resultProcesses: ProcessResult[] = procs.map(p => ({
    id: p.id,
    arrivalTime: p.arrivalTime,
    burstTime: p.burstTime,
    configuredPriority: p.configuredPriority,
    effectivePriority: p.effectivePriority,
    firstStartTime: p.firstStartTime,
    completionTime: p.completionTime,
    turnaroundTime: p.turnaroundTime,
    waitingTime: p.waitingTime,
    responseTime: p.responseTime,
    agingEventsCount: p.agingEventsCount,
    color: p.color
  }));

  return {
    algorithmName: algorithm,
    config,
    processes: resultProcesses,
    segments,
    timeline,
    queueSnapshots,
    events,
    totalTime,
    busyTime,
    idleTime,
    contextSwitchTime: csTime,
    contextSwitchCount: csCount,
    avgWaitingTime: avgWT,
    avgTurnaroundTime: avgTAT,
    avgResponseTime: avgRT,
    cpuUtilization: cpuUtil,
    idlePercentage: idlePct,
    contextSwitchPercentage: csPct,
    throughput,
    minWaitingTime: minWT,
    maxWaitingTime: maxWT,
    medianWaitingTime: medianWT,
    waitingTimeVariance: wtVariance,
    waitingTimeStdDev: wtStdDev,
    jainsFairnessIndex: jainsIndex,
    starvationWarnings
  };
}
