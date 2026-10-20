import React from 'react';
import { SimulationResult } from '../types/scheduler';

interface LiveCpuViewProps {
  result: SimulationResult | null;
  cursor: number;
}

export const LiveCpuView: React.FC<LiveCpuViewProps> = ({ result, cursor }) => {
  if (!result || result.totalTime === 0) {
    return (
      <div className="bg-[#151a2c] border border-[#2b3450] rounded-xl p-4 mb-4 flex items-center justify-center text-[#96a0bc] text-xs font-medium">
        Simulation ready — add processes and click Run
      </div>
    );
  }

  const isDone = cursor >= result.totalTime;
  const currentTick = Math.min(cursor, result.totalTime - 1);
  const currentCode = isDone ? -99 : result.timeline[currentTick];
  const queueSnap = isDone ? [] : result.queueSnapshots[currentTick] || [];

  let cpuLabel = 'IDLE';
  let cpuColor = '#4a506a';

  if (isDone) {
    cpuLabel = 'DONE';
    cpuColor = '#34a06e';
  } else if (currentCode === -2) {
    cpuLabel = 'CS';
    cpuColor = '#ff6880';
  } else if (currentCode >= 0) {
    const p = result.processes[currentCode];
    cpuLabel = p ? p.id : `P${currentCode + 1}`;
    cpuColor = p ? p.color : '#7c5cff';
  }

  return (
    <div className="bg-[#151a2c] border border-[#2b3450] rounded-xl p-3.5 mb-4 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
      {/* Clock section */}
      <div className="flex items-center gap-6">
        <div>
          <div className="text-[10px] font-bold text-[#96a0bc] uppercase tracking-wider">CLOCK</div>
          <div className="text-2xl font-black text-[#38cfdc] font-mono">
            t = {cursor}
          </div>
        </div>

        <div className="h-8 w-px bg-[#2b3450]"></div>

        {/* CPU Box */}
        <div>
          <div className="text-[10px] font-bold text-[#96a0bc] uppercase tracking-wider mb-1">CPU STATE</div>
          <div
            className="px-4 py-1.5 rounded-lg text-xs font-bold text-white shadow-sm flex items-center gap-2"
            style={{ backgroundColor: cpuColor }}
          >
            <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse"></span>
            {cpuLabel}
          </div>
        </div>
      </div>

      {/* Ready Queue section */}
      <div className="flex-1 w-full md:w-auto overflow-hidden">
        <div className="text-[10px] font-bold text-[#96a0bc] uppercase tracking-wider mb-1">READY QUEUE</div>
        <div className="flex items-center space-x-1.5 overflow-x-auto py-1">
          {queueSnap.length === 0 ? (
            <span className="text-xs text-[#96a0bc] italic">Empty</span>
          ) : (
            queueSnap.map((pid, idx) => {
              const proc = result.processes.find(p => p.id === pid);
              const color = proc ? proc.color : '#7c5cff';
              return (
                <div
                  key={idx}
                  className="px-2.5 py-1 rounded-md text-[11px] font-bold text-white whitespace-nowrap shadow-sm border border-white/10"
                  style={{ backgroundColor: color }}
                >
                  {pid}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
