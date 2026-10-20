import React from 'react';
import { ProcessInput } from '../types/scheduler';

interface ProcessTableProps {
  processes: ProcessInput[];
  setProcesses: React.Dispatch<React.SetStateAction<ProcessInput[]>>;
  onRandomize: () => void;
}

export const ProcessTable: React.FC<ProcessTableProps> = ({
  processes,
  setProcesses,
  onRandomize,
}) => {
  const handleUpdate = (index: number, field: keyof ProcessInput, value: any) => {
    setProcesses(prev => {
      const next = [...prev];
      next[index] = { ...next[index], [field]: value };
      return next;
    });
  };

  const handleAdd = () => {
    const nextId = `P${processes.length + 1}`;
    setProcesses(prev => [
      ...prev,
      { id: nextId, arrivalTime: 0, burstTime: 4, priority: 1 },
    ]);
  };

  const handleRemove = (index: number) => {
    if (processes.length <= 1) return;
    setProcesses(prev => prev.filter((_, i) => i !== index));
  };

  return (
    <div className="card-glow p-4 shadow-sm flex flex-col h-full">
      <div className="flex items-center justify-between mb-3">
        <div>
          <span className="text-xs font-bold text-[#ecf0fc] uppercase tracking-wider">
            Process Workload
          </span>
          <span className="text-[11px] text-[#96a0bc] ml-2">
            ({processes.length} jobs)
          </span>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={onRandomize}
            className="px-2.5 py-1 rounded-md bg-[#1c233a] hover:bg-[#242c48] border border-[#2b3450] text-[11px] font-semibold text-[#ecf0fc] transition-all"
            title="Generate random processes"
          >
            Random
          </button>
          <button
            onClick={handleAdd}
            className="px-2.5 py-1 rounded-md bg-[#7c5cff]/20 hover:bg-[#7c5cff]/30 border border-[#7c5cff]/40 text-[11px] font-bold text-[#a38bff] transition-all"
            title="Add new process"
          >
            + Add
          </button>
        </div>
      </div>

      <div className="overflow-x-auto flex-1 max-h-[340px] overflow-y-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-[#2b3450] text-[#38cfdc] font-bold text-[11px] bg-[#101426]/60 sticky top-0">
              <th className="py-2 px-2">ID</th>
              <th className="py-2 px-2">Arrival</th>
              <th className="py-2 px-2">Burst</th>
              <th className="py-2 px-2">Priority</th>
              <th className="py-2 px-1 text-center">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#2b3450]/40">
            {processes.map((p, i) => (
              <tr key={i} className="hover:bg-[#1c233a]/50 transition-colors">
                <td className="py-1.5 px-2">
                  <input
                    type="text"
                    value={p.id}
                    onChange={e => handleUpdate(i, 'id', e.target.value)}
                    className="w-14 bg-[#101426] border border-[#2b3450] rounded px-1.5 py-0.5 text-xs text-white font-mono font-bold focus:border-[#7c5cff] outline-none"
                  />
                </td>
                <td className="py-1.5 px-2">
                  <input
                    type="number"
                    min={0}
                    value={p.arrivalTime}
                    onChange={e => handleUpdate(i, 'arrivalTime', Math.max(0, parseInt(e.target.value, 10) || 0))}
                    className="w-16 bg-[#101426] border border-[#2b3450] rounded px-1.5 py-0.5 text-xs text-white font-mono focus:border-[#7c5cff] outline-none"
                  />
                </td>
                <td className="py-1.5 px-2">
                  <input
                    type="number"
                    min={1}
                    value={p.burstTime}
                    onChange={e => handleUpdate(i, 'burstTime', Math.max(1, parseInt(e.target.value, 10) || 1))}
                    className="w-16 bg-[#101426] border border-[#2b3450] rounded px-1.5 py-0.5 text-xs text-white font-mono focus:border-[#7c5cff] outline-none"
                  />
                </td>
                <td className="py-1.5 px-2">
                  <input
                    type="number"
                    min={0}
                    value={p.priority}
                    onChange={e => handleUpdate(i, 'priority', Math.max(0, parseInt(e.target.value, 10) || 0))}
                    className="w-16 bg-[#101426] border border-[#2b3450] rounded px-1.5 py-0.5 text-xs text-white font-mono focus:border-[#7c5cff] outline-none"
                  />
                </td>
                <td className="py-1.5 px-1 text-center">
                  <button
                    onClick={() => handleRemove(i)}
                    disabled={processes.length <= 1}
                    className="text-[#ff6880] hover:text-red-400 disabled:opacity-30 disabled:pointer-events-none p-1 rounded"
                    title="Remove process"
                  >
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="text-[10px] text-[#96a0bc] mt-2 italic">
        Lower priority number = higher priority &bull; Click any value to edit
      </div>
    </div>
  );
};
