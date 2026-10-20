import React from 'react';
import {
  ProcessInput,
  SimulationConfig,
  SimulationResult,
  AlgorithmId,
  ALGORITHMS,
} from '../types/scheduler';
import { ProcessTable } from './ProcessTable';
import { StarvationMonitor } from './StarvationMonitor';

interface SimulatorTabProps {
  algorithm: AlgorithmId;
  setAlgorithm: (a: AlgorithmId) => void;
  processes: ProcessInput[];
  setProcesses: React.Dispatch<React.SetStateAction<ProcessInput[]>>;
  config: SimulationConfig;
  setConfig: React.Dispatch<React.SetStateAction<SimulationConfig>>;
  result: SimulationResult | null;
  onRun: () => void;
  onRandomize: () => void;
}

export const SimulatorTab: React.FC<SimulatorTabProps> = ({
  algorithm,
  setAlgorithm,
  processes,
  setProcesses,
  config,
  setConfig,
  result,
  onRun,
  onRandomize,
}) => {
  const currentAlgoMeta = ALGORITHMS.find(a => a.id === algorithm) || ALGORITHMS[0];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 mb-5">
      {/* Left Column: Algorithm Config & Workload */}
      <div className="lg:col-span-5 space-y-4">
        {/* Algorithm Configuration Box */}
        <div className="card-glow p-4 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-[#ecf0fc] uppercase tracking-wider">
              Algorithm & Parameters
            </span>
            <button
              onClick={onRun}
              className="px-4 py-1.5 rounded-lg bg-gradient-to-r from-[#7c5cff] to-[#38cfdc] hover:from-[#6c4be8] hover:to-[#2ec0ce] text-white text-xs font-bold shadow-md transition-all flex items-center gap-1.5"
            >
              <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z" />
              </svg>
              Run Simulation
            </button>
          </div>

          <div className="space-y-3">
            <div>
              <label className="text-[10px] text-[#96a0bc] block mb-1 uppercase font-bold">
                Scheduling Policy
              </label>
              <select
                value={algorithm}
                onChange={e => setAlgorithm(e.target.value as AlgorithmId)}
                className="w-full bg-[#101426] border border-[#2b3450] rounded-lg px-3 py-2 text-xs text-white font-semibold outline-none focus:border-[#7c5cff]"
              >
                {ALGORITHMS.map(a => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </select>
              <div className="text-[11px] text-[#96a0bc] mt-1.5 leading-relaxed bg-[#101426]/70 p-2 rounded border border-[#2b3450]/40">
                {currentAlgoMeta.description}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-1">
              <div>
                <label className="text-[10px] text-[#96a0bc] block mb-1 uppercase font-bold">
                  Time Quantum (q)
                </label>
                <input
                  type="number"
                  min={1}
                  max={50}
                  disabled={!currentAlgoMeta.requiresQuantum}
                  value={config.quantum}
                  onChange={e =>
                    setConfig(prev => ({
                      ...prev,
                      quantum: Math.max(1, parseInt(e.target.value, 10) || 1),
                    }))
                  }
                  className="w-full bg-[#101426] border border-[#2b3450] rounded-lg px-3 py-1.5 text-xs text-white font-mono disabled:opacity-30 outline-none focus:border-[#7c5cff]"
                />
              </div>

              <div>
                <label className="text-[10px] text-[#96a0bc] block mb-1 uppercase font-bold">
                  Context Switch (CS)
                </label>
                <input
                  type="number"
                  min={0}
                  max={10}
                  value={config.contextSwitchOverhead}
                  onChange={e =>
                    setConfig(prev => ({
                      ...prev,
                      contextSwitchOverhead: Math.max(0, parseInt(e.target.value, 10) || 0),
                    }))
                  }
                  className="w-full bg-[#101426] border border-[#2b3450] rounded-lg px-3 py-1.5 text-xs text-white font-mono outline-none focus:border-[#7c5cff]"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Priority Aging & Starvation Monitor */}
        <StarvationMonitor config={config} setConfig={setConfig} result={result} />

        {/* Process Table Input */}
        <ProcessTable
          processes={processes}
          setProcesses={setProcesses}
          onRandomize={onRandomize}
        />
      </div>

      {/* Right Column: Execution Results Table */}
      <div className="lg:col-span-7 flex flex-col">
        <div className="card-glow p-4 shadow-sm flex-1 flex flex-col">
          <div className="flex items-center justify-between mb-3 border-b border-[#2b3450]/60 pb-2">
            <div>
              <span className="text-xs font-bold text-[#ecf0fc] uppercase tracking-wider">
                Process Completion & Execution Results
              </span>
              <span className="text-[11px] text-[#96a0bc] ml-2">
                CT = Completion &bull; TAT = CT − AT &bull; WT = TAT − BT &bull; RT = First Start − AT
              </span>
            </div>
          </div>

          <div className="overflow-x-auto flex-1 max-h-[640px] overflow-y-auto">
            {result ? (
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-[#2b3450] text-[#38cfdc] font-bold text-[11px] bg-[#101426]/70 sticky top-0">
                    <th className="py-2.5 px-3">Process</th>
                    <th className="py-2.5 px-3">Arrival</th>
                    <th className="py-2.5 px-3">Burst</th>
                    <th className="py-2.5 px-3">Config PR</th>
                    <th className="py-2.5 px-3">Effective PR</th>
                    <th className="py-2.5 px-3">Completion</th>
                    <th className="py-2.5 px-3 text-[#38cfdc]">Turnaround</th>
                    <th className="py-2.5 px-3 text-[#ffb84d]">Waiting</th>
                    <th className="py-2.5 px-3 text-[#4ade80]">Response</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#2b3450]/40 font-mono text-[12px]">
                  {result.processes.map((p, idx) => (
                    <tr key={idx} className="hover:bg-[#1c233a] transition-colors">
                      <td className="py-2.5 px-3 font-sans font-bold flex items-center gap-2" style={{ color: p.color }}>
                        <span className="w-2 h-2 rounded-full" style={{ backgroundColor: p.color }}></span>
                        {p.id}
                      </td>
                      <td className="py-2.5 px-3 text-[#ecf0fc]">{p.arrivalTime}</td>
                      <td className="py-2.5 px-3 text-[#ecf0fc]">{p.burstTime}</td>
                      <td className="py-2.5 px-3 text-[#96a0bc]">{p.configuredPriority}</td>
                      <td className="py-2.5 px-3 text-[#ecf0fc]">
                        {p.effectivePriority}
                        {p.agingEventsCount > 0 && (
                          <span className="text-[10px] text-[#4ade80] ml-1">(-{p.agingEventsCount})</span>
                        )}
                      </td>
                      <td className="py-2.5 px-3 text-[#ecf0fc]">{p.completionTime}</td>
                      <td className="py-2.5 px-3 text-[#38cfdc] font-bold">{p.turnaroundTime}</td>
                      <td className="py-2.5 px-3 text-[#ffb84d] font-bold">{p.waitingTime}</td>
                      <td className="py-2.5 px-3 text-[#4ade80] font-bold">{p.responseTime}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="text-center py-20 text-[#96a0bc] text-xs">
                No simulation results yet. Configure the processes on the left and click <strong>Run Simulation</strong>.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
