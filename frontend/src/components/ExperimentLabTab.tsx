import React, { useState } from 'react';
import { ProcessInput, SimulationConfig, SimulationResult, ALGORITHMS, AlgorithmId } from '../types/scheduler';
import { runSimulation } from '../engine/simulationEngine';

interface ExperimentLabTabProps {
  onLoadWorkloadToSimulator: (workload: ProcessInput[], algo: AlgorithmId) => void;
}

type ObjectiveType =
  | 'MIN_WT'
  | 'MIN_TAT'
  | 'MIN_RT'
  | 'MAX_THROUGHPUT'
  | 'MIN_CS'
  | 'MAX_FAIRNESS';

export const ExperimentLabTab: React.FC<ExperimentLabTabProps> = ({
  onLoadWorkloadToSimulator,
}) => {
  const [preset, setPreset] = useState<string>('MIXED');
  const [procCount, setProcCount] = useState<number>(8);
  const [arrivalMax, setArrivalMax] = useState<number>(12);
  const [burstMin, setBurstMin] = useState<number>(2);
  const [burstMax, setBurstMax] = useState<number>(14);
  const [priorityMax, setPriorityMax] = useState<number>(5);
  const [seed, setSeed] = useState<number>(42);
  const [quantum, setQuantum] = useState<number>(3);
  const [csOverhead, setCsOverhead] = useState<number>(1);
  const [objective, setObjective] = useState<ObjectiveType>('MIN_WT');

  const [experimentResults, setExperimentResults] = useState<{
    results: SimulationResult[];
    workload: ProcessInput[];
  } | null>(null);

  // Apply Presets
  const applyPreset = (name: string) => {
    setPreset(name);
    switch (name) {
      case 'SHORT':
        setProcCount(8);
        setArrivalMax(8);
        setBurstMin(1);
        setBurstMax(4);
        break;
      case 'LONG':
        setProcCount(6);
        setArrivalMax(5);
        setBurstMin(12);
        setBurstMax(25);
        break;
      case 'MIXED':
        setProcCount(8);
        setArrivalMax(10);
        setBurstMin(2);
        setBurstMax(16);
        break;
      case 'STAGGERED':
        setProcCount(7);
        setArrivalMax(25);
        setBurstMin(3);
        setBurstMax(8);
        break;
      case 'PRIORITY':
        setProcCount(8);
        setArrivalMax(6);
        setBurstMin(4);
        setBurstMax(10);
        setPriorityMax(10);
        break;
      case 'CONTENTION':
        setProcCount(14);
        setArrivalMax(2);
        setBurstMin(3);
        setBurstMax(9);
        break;
    }
  };

  // Pseudo-random deterministic PRNG
  function pseudoRandom(s: number) {
    let t = s;
    return () => {
      t = (t * 1664525 + 1013904223) % 4294967296;
      return t / 4294967296;
    };
  }

  const runExperiment = () => {
    const rng = pseudoRandom(seed);
    const generated: ProcessInput[] = [];

    for (let i = 0; i < procCount; i++) {
      const at = Math.floor(rng() * (arrivalMax + 1));
      const bt = burstMin + Math.floor(rng() * (burstMax - burstMin + 1));
      const pr = 1 + Math.floor(rng() * priorityMax);
      generated.push({ id: `P${i + 1}`, arrivalTime: at, burstTime: bt, priority: pr });
    }

    const cfg: SimulationConfig = {
      quantum,
      contextSwitchOverhead: csOverhead,
      agingEnabled: true,
      agingInterval: 5,
      priorityBoost: 1,
      minPriority: 0,
      starvationThreshold: 20,
    };

    const res = ALGORITHMS.map(a => runSimulation(a.id, generated, cfg));
    setExperimentResults({ results: res, workload: generated });
  };

  const getWinner = () => {
    if (!experimentResults) return null;
    const { results } = experimentResults;
    let best = results[0];

    for (const r of results) {
      switch (objective) {
        case 'MIN_WT':
          if (r.avgWaitingTime < best.avgWaitingTime) best = r;
          break;
        case 'MIN_TAT':
          if (r.avgTurnaroundTime < best.avgTurnaroundTime) best = r;
          break;
        case 'MIN_RT':
          if (r.avgResponseTime < best.avgResponseTime) best = r;
          break;
        case 'MAX_THROUGHPUT':
          if (r.throughput > best.throughput) best = r;
          break;
        case 'MIN_CS':
          if (r.contextSwitchCount < best.contextSwitchCount) best = r;
          break;
        case 'MAX_FAIRNESS':
          if (r.jainsFairnessIndex > best.jainsFairnessIndex) best = r;
          break;
      }
    }
    return best;
  };

  const winner = getWinner();

  return (
    <div className="space-y-5">
      {/* Configuration Box */}
      <div className="card-glow p-5 shadow-sm">
        <h2 className="text-sm font-extrabold text-white uppercase tracking-wider mb-2 flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-[#7c5cff]"></span>
          Workload Experimentation Lab & Benchmark Suite
        </h2>
        <p className="text-xs text-[#96a0bc] mb-4">
          Select or customize empirical workload profiles to rigorously evaluate algorithms under varying load conditions.
        </p>

        {/* Presets */}
        <div className="flex flex-wrap gap-2 mb-4">
          {[
            { id: 'SHORT', name: 'Short Jobs Dominated' },
            { id: 'LONG', name: 'Long Jobs Dominated' },
            { id: 'MIXED', name: 'Mixed Workload' },
            { id: 'STAGGERED', name: 'Staggered Arrivals' },
            { id: 'PRIORITY', name: 'Priority Conflicts' },
            { id: 'CONTENTION', name: 'Heavy Contention (14 Jobs)' },
          ].map(p => (
            <button
              key={p.id}
              onClick={() => applyPreset(p.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                preset === p.id
                  ? 'bg-[#7c5cff] text-white border-[#7c5cff]'
                  : 'bg-[#151a2c] text-[#96a0bc] border-[#2b3450] hover:text-white'
              }`}
            >
              {p.name}
            </button>
          ))}
        </div>

        {/* Parameters Grid */}
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-3 mb-4 text-xs">
          <div>
            <label className="text-[#96a0bc] block mb-1">Process Count (N)</label>
            <input
              type="number"
              min={3}
              max={25}
              value={procCount}
              onChange={e => setProcCount(parseInt(e.target.value, 10) || 5)}
              className="w-full bg-[#101426] border border-[#2b3450] rounded px-2 py-1 text-white font-mono"
            />
          </div>

          <div>
            <label className="text-[#96a0bc] block mb-1">Max Arrival</label>
            <input
              type="number"
              min={0}
              value={arrivalMax}
              onChange={e => setArrivalMax(parseInt(e.target.value, 10) || 0)}
              className="w-full bg-[#101426] border border-[#2b3450] rounded px-2 py-1 text-white font-mono"
            />
          </div>

          <div>
            <label className="text-[#96a0bc] block mb-1">Burst Range (Min-Max)</label>
            <div className="flex gap-1">
              <input
                type="number"
                min={1}
                value={burstMin}
                onChange={e => setBurstMin(parseInt(e.target.value, 10) || 1)}
                className="w-1/2 bg-[#101426] border border-[#2b3450] rounded px-1.5 py-1 text-white font-mono"
              />
              <input
                type="number"
                min={1}
                value={burstMax}
                onChange={e => setBurstMax(parseInt(e.target.value, 10) || 1)}
                className="w-1/2 bg-[#101426] border border-[#2b3450] rounded px-1.5 py-1 text-white font-mono"
              />
            </div>
          </div>

          <div>
            <label className="text-[#96a0bc] block mb-1">Seed (PRNG)</label>
            <input
              type="number"
              value={seed}
              onChange={e => setSeed(parseInt(e.target.value, 10) || 1)}
              className="w-full bg-[#101426] border border-[#2b3450] rounded px-2 py-1 text-white font-mono"
            />
          </div>

          <div>
            <label className="text-[#96a0bc] block mb-1">Time Quantum</label>
            <input
              type="number"
              min={1}
              value={quantum}
              onChange={e => setQuantum(parseInt(e.target.value, 10) || 1)}
              className="w-full bg-[#101426] border border-[#2b3450] rounded px-2 py-1 text-white font-mono"
            />
          </div>

          <div>
            <label className="text-[#96a0bc] block mb-1">Context Switch</label>
            <input
              type="number"
              min={0}
              value={csOverhead}
              onChange={e => setCsOverhead(parseInt(e.target.value, 10) || 0)}
              className="w-full bg-[#101426] border border-[#2b3450] rounded px-2 py-1 text-white font-mono"
            />
          </div>

          <div>
            <label className="text-[#96a0bc] block mb-1">Optimization Goal</label>
            <select
              value={objective}
              onChange={e => setObjective(e.target.value as ObjectiveType)}
              className="w-full bg-[#101426] border border-[#2b3450] rounded px-2 py-1 text-white font-sans text-xs"
            >
              <option value="MIN_WT">Minimize Waiting Time</option>
              <option value="MIN_TAT">Minimize Turnaround</option>
              <option value="MIN_RT">Minimize Response Time</option>
              <option value="MAX_THROUGHPUT">Maximize Throughput</option>
              <option value="MIN_CS">Minimize Context Switches</option>
              <option value="MAX_FAIRNESS">Maximize Fairness</option>
            </select>
          </div>
        </div>

        <button
          onClick={runExperiment}
          className="px-5 py-2 rounded-lg bg-gradient-to-r from-[#7c5cff] to-[#38cfdc] hover:from-[#6c4be8] hover:to-[#2ec0ce] text-white text-xs font-bold shadow-md transition-all flex items-center gap-2"
        >
          <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z" />
          </svg>
          Run Multi-Algorithm Experiment
        </button>
      </div>

      {/* Results View */}
      {experimentResults && winner && (
        <div className="space-y-4">
          {/* Winner Banner */}
          <div className="card-glow p-4 bg-gradient-to-r from-[#1c233a] to-[#151a2c] border border-[#4ade80]/40 flex flex-col md:flex-row items-center justify-between gap-4">
            <div>
              <div className="text-[11px] font-bold text-[#4ade80] uppercase tracking-wider">
                Optimal Algorithm for Selected Objective
              </div>
              <div className="text-xl font-black text-white mt-0.5">
                {winner.algorithmName}
              </div>
              <div className="text-xs text-[#96a0bc] mt-1 font-mono">
                AWT: {winner.avgWaitingTime.toFixed(2)} &bull; ATAT: {winner.avgTurnaroundTime.toFixed(2)} &bull; Jain's Fairness: {winner.jainsFairnessIndex.toFixed(3)}
              </div>
            </div>

            <button
              onClick={() => onLoadWorkloadToSimulator(experimentResults.workload, winner.algorithmName as AlgorithmId)}
              className="px-4 py-2 rounded-lg bg-[#4ade80] hover:bg-[#3ec470] text-[#0a0d19] font-bold text-xs shadow-md transition-all whitespace-nowrap"
            >
              Load This Scenario in Simulator &rarr;
            </button>
          </div>

          {/* Experiment Comparison Table */}
          <div className="card-glow p-5 shadow-sm overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse font-mono">
              <thead>
                <tr className="border-b border-[#2b3450] text-[#38cfdc] font-bold text-[11px] bg-[#101426]/70">
                  <th className="py-2.5 px-3">Algorithm</th>
                  <th className="py-2.5 px-3">Avg WT</th>
                  <th className="py-2.5 px-3">Avg TAT</th>
                  <th className="py-2.5 px-3">Avg RT</th>
                  <th className="py-2.5 px-3">CPU Util</th>
                  <th className="py-2.5 px-3">Throughput</th>
                  <th className="py-2.5 px-3">Context Sw</th>
                  <th className="py-2.5 px-3">Fairness</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#2b3450]/40">
                {experimentResults.results.map((r, i) => (
                  <tr
                    key={i}
                    className={`hover:bg-[#1c233a] ${
                      r.algorithmName === winner.algorithmName ? 'bg-[#7c5cff]/10 font-bold' : ''
                    }`}
                  >
                    <td className="py-2 px-3 font-sans text-white">
                      {r.algorithmName === winner.algorithmName ? '★ ' : ''}
                      {r.algorithmName}
                    </td>
                    <td className="py-2 px-3 text-[#ffb84d]">{r.avgWaitingTime.toFixed(2)}</td>
                    <td className="py-2 px-3 text-[#38cfdc]">{r.avgTurnaroundTime.toFixed(2)}</td>
                    <td className="py-2 px-3 text-[#4ade80]">{r.avgResponseTime.toFixed(2)}</td>
                    <td className="py-2 px-3">{r.cpuUtilization.toFixed(1)}%</td>
                    <td className="py-2 px-3">{r.throughput.toFixed(3)}</td>
                    <td className="py-2 px-3 text-[#6096ff]">{r.contextSwitchCount}</td>
                    <td className="py-2 px-3 text-[#be78ff]">{r.jainsFairnessIndex.toFixed(3)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
