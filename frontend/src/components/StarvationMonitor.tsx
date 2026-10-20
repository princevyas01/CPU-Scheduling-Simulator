import React from 'react';
import { SimulationConfig, SimulationResult } from '../types/scheduler';

interface StarvationMonitorProps {
  config: SimulationConfig;
  setConfig: React.Dispatch<React.SetStateAction<SimulationConfig>>;
  result: SimulationResult | null;
}

export const StarvationMonitor: React.FC<StarvationMonitorProps> = ({
  config,
  setConfig,
  result,
}) => {
  return (
    <div className="card-glow p-4 shadow-sm mb-4">
      <div className="flex items-center justify-between mb-3 border-b border-[#2b3450]/60 pb-2">
        <div>
          <span className="text-xs font-bold text-[#ecf0fc] uppercase tracking-wider">
            Priority Aging & Starvation Monitor
          </span>
          <span className="text-[11px] text-[#96a0bc] ml-2">
            Prevents low-priority indefinite starvation
          </span>
        </div>
        <label className="flex items-center cursor-pointer space-x-2">
          <input
            type="checkbox"
            checked={config.agingEnabled}
            onChange={e => setConfig(prev => ({ ...prev, agingEnabled: e.target.checked }))}
            className="w-4 h-4 rounded accent-[#7c5cff] cursor-pointer"
          />
          <span className="text-xs font-semibold text-white">Enable Aging</span>
        </label>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs mb-3">
        <div>
          <label className="text-[10px] text-[#96a0bc] block mb-1">Aging Interval (ticks)</label>
          <input
            type="number"
            min={1}
            max={20}
            disabled={!config.agingEnabled}
            value={config.agingInterval}
            onChange={e =>
              setConfig(prev => ({ ...prev, agingInterval: Math.max(1, parseInt(e.target.value, 10) || 1) }))
            }
            className="w-full bg-[#101426] border border-[#2b3450] rounded px-2 py-1 text-white font-mono disabled:opacity-40"
          />
        </div>

        <div>
          <label className="text-[10px] text-[#96a0bc] block mb-1">Priority Boost Step</label>
          <input
            type="number"
            min={1}
            max={5}
            disabled={!config.agingEnabled}
            value={config.priorityBoost}
            onChange={e =>
              setConfig(prev => ({ ...prev, priorityBoost: Math.max(1, parseInt(e.target.value, 10) || 1) }))
            }
            className="w-full bg-[#101426] border border-[#2b3450] rounded px-2 py-1 text-white font-mono disabled:opacity-40"
          />
        </div>

        <div>
          <label className="text-[10px] text-[#96a0bc] block mb-1">Min Priority Floor</label>
          <input
            type="number"
            min={0}
            max={10}
            disabled={!config.agingEnabled}
            value={config.minPriority}
            onChange={e =>
              setConfig(prev => ({ ...prev, minPriority: Math.max(0, parseInt(e.target.value, 10) || 0) }))
            }
            className="w-full bg-[#101426] border border-[#2b3450] rounded px-2 py-1 text-white font-mono disabled:opacity-40"
          />
        </div>

        <div>
          <label className="text-[10px] text-[#96a0bc] block mb-1">Warning Threshold</label>
          <input
            type="number"
            min={5}
            max={100}
            value={config.starvationThreshold}
            onChange={e =>
              setConfig(prev => ({ ...prev, starvationThreshold: Math.max(1, parseInt(e.target.value, 10) || 10) }))
            }
            className="w-full bg-[#101426] border border-[#2b3450] rounded px-2 py-1 text-white font-mono"
          />
        </div>
      </div>

      {/* Starvation Alerts */}
      {result && result.starvationWarnings.length > 0 && (
        <div className="bg-[#ff6880]/15 border border-[#ff6880]/30 rounded-lg p-2.5 space-y-1">
          {result.starvationWarnings.map((warn, i) => (
            <div key={i} className="text-xs text-[#ff6880] flex items-center gap-1.5 font-medium">
              <span className="w-2 h-2 rounded-full bg-[#ff6880] animate-ping"></span>
              {warn}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
