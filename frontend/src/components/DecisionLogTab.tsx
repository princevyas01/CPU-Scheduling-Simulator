import React, { useState } from 'react';
import { SchedulingEvent, EventType } from '../types/scheduler';

interface DecisionLogTabProps {
  events: SchedulingEvent[];
  onSelectTime: (t: number) => void;
}

export const DecisionLogTab: React.FC<DecisionLogTabProps> = ({ events, onSelectTime }) => {
  const [filterType, setFilterType] = useState<string>('ALL');
  const [filterPid, setFilterPid] = useState<string>('ALL');

  const pids = Array.from(new Set(events.map(e => e.processId).filter(p => p && p !== '-'))).sort();

  const filtered = events.filter(e => {
    if (filterType !== 'ALL' && e.eventType !== filterType) return false;
    if (filterPid !== 'ALL' && e.processId !== filterPid && e.runningProcessId !== filterPid) return false;
    return true;
  });

  const getBadgeClass = (type: EventType) => {
    switch (type) {
      case 'PROCESS_DISPATCHED':
        return 'bg-[#4ade80]/20 text-[#4ade80] border-[#4ade80]/30';
      case 'PROCESS_PREEMPTED':
        return 'bg-[#ff6880]/20 text-[#ff6880] border-[#ff6880]/30';
      case 'QUANTUM_EXPIRED':
        return 'bg-[#ffb84d]/20 text-[#ffb84d] border-[#ffb84d]/30';
      case 'PROCESS_COMPLETED':
        return 'bg-[#38cfdc]/20 text-[#38cfdc] border-[#38cfdc]/30';
      case 'PROCESS_DEMOTED':
        return 'bg-[#be78ff]/20 text-[#be78ff] border-[#be78ff]/30';
      case 'PRIORITY_AGED':
        return 'bg-[#6096ff]/20 text-[#6096ff] border-[#6096ff]/30';
      case 'CONTEXT_SWITCH_STARTED':
      case 'CONTEXT_SWITCH_COMPLETED':
        return 'bg-pink-500/20 text-pink-400 border-pink-500/30';
      default:
        return 'bg-[#2b3450] text-[#96a0bc] border-[#3b4668]';
    }
  };

  return (
    <div className="card-glow p-5 shadow-sm">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4 pb-4 border-b border-[#2b3450]">
        <div>
          <h2 className="text-base font-extrabold text-white flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#7c5cff]"></span>
            Scheduling Decision Explanation Engine
          </h2>
          <p className="text-xs text-[#96a0bc] mt-1">
            Real-time educational event trace generated directly from the simulation engine. Click any row to jump the Gantt timeline to that event.
          </p>
        </div>

        {/* Filters */}
        <div className="flex items-center gap-3 text-xs">
          <div>
            <label className="text-[10px] text-[#96a0bc] block mb-1 uppercase font-bold">Event Type</label>
            <select
              value={filterType}
              onChange={e => setFilterType(e.target.value)}
              className="bg-[#101426] border border-[#2b3450] rounded-lg px-2.5 py-1.5 text-xs text-white outline-none focus:border-[#7c5cff]"
            >
              <option value="ALL">All Events ({events.length})</option>
              <option value="PROCESS_ARRIVED">Arrivals</option>
              <option value="PROCESS_DISPATCHED">Dispatches</option>
              <option value="PROCESS_PREEMPTED">Preemptions</option>
              <option value="QUANTUM_EXPIRED">Quantum Expirations</option>
              <option value="PROCESS_DEMOTED">MLFQ Demotions</option>
              <option value="PRIORITY_AGED">Priority Aging</option>
              <option value="PROCESS_COMPLETED">Completions</option>
              <option value="CONTEXT_SWITCH_STARTED">Context Switches</option>
            </select>
          </div>

          <div>
            <label className="text-[10px] text-[#96a0bc] block mb-1 uppercase font-bold">Process</label>
            <select
              value={filterPid}
              onChange={e => setFilterPid(e.target.value)}
              className="bg-[#101426] border border-[#2b3450] rounded-lg px-2.5 py-1.5 text-xs text-white outline-none focus:border-[#7c5cff]"
            >
              <option value="ALL">All Processes</option>
              {pids.map(pid => (
                <option key={pid} value={pid}>
                  {pid}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Events Table */}
      <div className="overflow-x-auto max-h-[520px] overflow-y-auto">
        {filtered.length === 0 ? (
          <div className="text-center py-12 text-[#96a0bc] text-xs">
            No events match the selected filters.
          </div>
        ) : (
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-[#2b3450] text-[#38cfdc] font-bold text-[11px] bg-[#101426]/70 sticky top-0 z-10">
                <th className="py-2.5 px-3">Time</th>
                <th className="py-2.5 px-3">Event</th>
                <th className="py-2.5 px-3">Subject Process</th>
                <th className="py-2.5 px-3">Ready Queue</th>
                <th className="py-2.5 px-3">Decision Reason</th>
                <th className="py-2.5 px-3">Relevant Values</th>
                <th className="py-2.5 px-3">Outcome</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#2b3450]/30 font-sans">
              {filtered.map((e, idx) => (
                <tr
                  key={idx}
                  onClick={() => onSelectTime(e.time)}
                  className="hover:bg-[#1c233a] cursor-pointer transition-colors group"
                >
                  <td className="py-2.5 px-3 font-mono font-bold text-[#38cfdc] whitespace-nowrap">
                    t = {e.time}
                  </td>
                  <td className="py-2.5 px-3 whitespace-nowrap">
                    <span
                      className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold border ${getBadgeClass(
                        e.eventType
                      )}`}
                    >
                      {e.eventType.replace(/_/g, ' ')}
                    </span>
                  </td>
                  <td className="py-2.5 px-3 font-bold text-white whitespace-nowrap">
                    {e.processId}
                  </td>
                  <td className="py-2.5 px-3 font-mono text-[11px] text-[#96a0bc]">
                    {e.readyProcesses}
                  </td>
                  <td className="py-2.5 px-3 text-[#ecf0fc] group-hover:text-white leading-relaxed">
                    {e.decisionReason}
                  </td>
                  <td className="py-2.5 px-3 font-mono text-[10px] text-[#96a0bc] whitespace-nowrap">
                    {e.relevantValues}
                  </td>
                  <td className="py-2.5 px-3 text-[#4ade80] font-semibold whitespace-nowrap">
                    {e.outcome}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
