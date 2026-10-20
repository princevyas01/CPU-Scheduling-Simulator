import React, { useMemo } from 'react';
import { ProcessInput, SimulationConfig, SimulationResult, ALGORITHMS, AlgorithmId } from '../types/scheduler';
import { runSimulation } from '../engine/simulationEngine';

interface CompareTabProps {
  processes: ProcessInput[];
  config: SimulationConfig;
  onDrillDown: (algoId: AlgorithmId) => void;
}

export const CompareTab: React.FC<CompareTabProps> = ({
  processes,
  config,
  onDrillDown,
}) => {
  const results: SimulationResult[] = useMemo(() => {
    return ALGORITHMS.map(a => runSimulation(a.id, processes, config));
  }, [processes, config]);

  // Find best metrics
  const minWT = Math.min(...results.map(r => r.avgWaitingTime));
  const minTAT = Math.min(...results.map(r => r.avgTurnaroundTime));
  const minRT = Math.min(...results.map(r => r.avgResponseTime));
  const maxUtil = Math.max(...results.map(r => r.cpuUtilization));
  const maxThroughput = Math.max(...results.map(r => r.throughput));
  const minCS = Math.min(...results.map(r => r.contextSwitchCount));
  const maxFairness = Math.max(...results.map(r => r.jainsFairnessIndex));

  return (
    <div className="space-y-5">
      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
        <div className="card-glow p-3.5">
          <div className="text-[10px] font-bold text-[#ffb84d] uppercase tracking-wider">Lowest Waiting Time</div>
          <div className="text-xl font-black text-white mt-1">
            {results.find(r => Math.abs(r.avgWaitingTime - minWT) < 1e-9)?.algorithmName || '—'}
          </div>
          <div className="text-xs font-mono text-[#ffb84d] mt-1">{minWT.toFixed(2)} units</div>
        </div>

        <div className="card-glow p-3.5">
          <div className="text-[10px] font-bold text-[#38cfdc] uppercase tracking-wider">Lowest Turnaround</div>
          <div className="text-xl font-black text-white mt-1">
            {results.find(r => Math.abs(r.avgTurnaroundTime - minTAT) < 1e-9)?.algorithmName || '—'}
          </div>
          <div className="text-xs font-mono text-[#38cfdc] mt-1">{minTAT.toFixed(2)} units</div>
        </div>

        <div className="card-glow p-3.5">
          <div className="text-[10px] font-bold text-[#4ade80] uppercase tracking-wider">Fastest Response</div>
          <div className="text-xl font-black text-white mt-1">
            {results.find(r => Math.abs(r.avgResponseTime - minRT) < 1e-9)?.algorithmName || '—'}
          </div>
          <div className="text-xs font-mono text-[#4ade80] mt-1">{minRT.toFixed(2)} units</div>
        </div>

        <div className="card-glow p-3.5">
          <div className="text-[10px] font-bold text-[#6096ff] uppercase tracking-wider">Highest Fairness</div>
          <div className="text-xl font-black text-white mt-1">
            {results.find(r => Math.abs(r.jainsFairnessIndex - maxFairness) < 1e-9)?.algorithmName || '—'}
          </div>
          <div className="text-xs font-mono text-[#6096ff] mt-1">Jain's {maxFairness.toFixed(3)}</div>
        </div>
      </div>

      {/* Comparison Leaderboard Table */}
      <div className="card-glow p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-sm font-extrabold text-white uppercase tracking-wider flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-[#38cfdc]"></span>
              All Algorithms Comparative Leaderboard
            </h2>
            <p className="text-xs text-[#96a0bc] mt-0.5">
              Identical workload benchmark. Green values indicate top-performing metric. Double-click or click Simulate to open in simulator.
            </p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#2b3450] text-[#38cfdc] font-bold text-[11px] bg-[#101426]/70">
                <th className="py-2.5 px-3">Algorithm</th>
                <th className="py-2.5 px-3">Avg WT</th>
                <th className="py-2.5 px-3">Avg TAT</th>
                <th className="py-2.5 px-3">Avg RT</th>
                <th className="py-2.5 px-3">CPU Util</th>
                <th className="py-2.5 px-3">Throughput</th>
                <th className="py-2.5 px-3">Context Sw</th>
                <th className="py-2.5 px-3">Fairness (Jain)</th>
                <th className="py-2.5 px-3">WT StdDev</th>
                <th className="py-2.5 px-3 text-center">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#2b3450]/40 font-mono text-[12px]">
              {results.map((r, i) => {
                const isBestWT = Math.abs(r.avgWaitingTime - minWT) < 1e-9;
                const isBestTAT = Math.abs(r.avgTurnaroundTime - minTAT) < 1e-9;
                const isBestRT = Math.abs(r.avgResponseTime - minRT) < 1e-9;
                const isBestUtil = Math.abs(r.cpuUtilization - maxUtil) < 1e-9;
                const isBestThr = Math.abs(r.throughput - maxThroughput) < 1e-9;
                const isBestCS = r.contextSwitchCount === minCS;
                const isBestFair = Math.abs(r.jainsFairnessIndex - maxFairness) < 1e-9;

                return (
                  <tr
                    key={i}
                    onDoubleClick={() => onDrillDown(r.algorithmName as AlgorithmId)}
                    className="hover:bg-[#1c233a] transition-colors cursor-pointer"
                  >
                    <td className="py-2.5 px-3 font-sans font-bold text-white flex items-center gap-2">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#7c5cff]"></span>
                      {r.algorithmName}
                    </td>
                    <td className={`py-2.5 px-3 ${isBestWT ? 'text-[#4ade80] font-black' : 'text-[#ecf0fc]'}`}>
                      {r.avgWaitingTime.toFixed(2)}
                    </td>
                    <td className={`py-2.5 px-3 ${isBestTAT ? 'text-[#4ade80] font-black' : 'text-[#ecf0fc]'}`}>
                      {r.avgTurnaroundTime.toFixed(2)}
                    </td>
                    <td className={`py-2.5 px-3 ${isBestRT ? 'text-[#4ade80] font-black' : 'text-[#ecf0fc]'}`}>
                      {r.avgResponseTime.toFixed(2)}
                    </td>
                    <td className={`py-2.5 px-3 ${isBestUtil ? 'text-[#4ade80] font-black' : 'text-[#ecf0fc]'}`}>
                      {r.cpuUtilization.toFixed(1)}%
                    </td>
                    <td className={`py-2.5 px-3 ${isBestThr ? 'text-[#4ade80] font-black' : 'text-[#ecf0fc]'}`}>
                      {r.throughput.toFixed(3)}
                    </td>
                    <td className={`py-2.5 px-3 ${isBestCS ? 'text-[#4ade80] font-black' : 'text-[#ecf0fc]'}`}>
                      {r.contextSwitchCount}
                    </td>
                    <td className={`py-2.5 px-3 ${isBestFair ? 'text-[#4ade80] font-black' : 'text-[#ecf0fc]'}`}>
                      {r.jainsFairnessIndex.toFixed(3)}
                    </td>
                    <td className="py-2.5 px-3 text-[#96a0bc]">
                      {r.waitingTimeStdDev.toFixed(2)}
                    </td>
                    <td className="py-2.5 px-3 text-center">
                      <button
                        onClick={e => {
                          e.stopPropagation();
                          onDrillDown(r.algorithmName as AlgorithmId);
                        }}
                        className="px-2.5 py-1 rounded bg-[#7c5cff]/20 hover:bg-[#7c5cff]/30 text-[#a38bff] text-[11px] font-sans font-bold border border-[#7c5cff]/40 transition-all"
                      >
                        Simulate
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Side-by-side Timeline comparison */}
      <div className="card-glow p-5 shadow-sm">
        <h2 className="text-sm font-extrabold text-white uppercase tracking-wider mb-3">
          Side-by-Side Execution Gantt Timelines
        </h2>
        <div className="space-y-3">
          {results.map((r, idx) => {
            const total = Math.max(1, r.totalTime);
            return (
              <div key={idx} className="bg-[#101426] p-2.5 rounded-lg border border-[#2b3450]/60">
                <div className="flex justify-between items-center text-xs mb-1">
                  <span className="font-bold text-white">{r.algorithmName}</span>
                  <span className="text-[11px] text-[#96a0bc] font-mono">
                    Total: {r.totalTime} units &bull; AWT: {r.avgWaitingTime.toFixed(2)}
                  </span>
                </div>
                <div className="w-full bg-[#151a2c] h-7 rounded overflow-hidden flex border border-[#2b3450]">
                  {r.segments.map((seg, sIdx) => {
                    const duration = seg.endTime - seg.startTime;
                    const pct = (duration / total) * 100;
                    return (
                      <div
                        key={sIdx}
                        title={`${seg.processId}: ${seg.startTime} -> ${seg.endTime}`}
                        className="h-full flex items-center justify-center text-[10px] font-bold text-white truncate px-0.5"
                        style={{
                          width: `${pct}%`,
                          backgroundColor:
                            seg.type === 'PROCESS'
                              ? seg.color
                              : seg.type === 'CONTEXT_SWITCH'
                              ? '#ff6880'
                              : '#2b3450',
                        }}
                      >
                        {pct > 4 ? seg.processId : ''}
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
