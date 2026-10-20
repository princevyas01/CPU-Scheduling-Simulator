import React, { useState, useEffect } from 'react';
import { SimulationResult } from '../types/scheduler';

interface GanttChartProps {
  result: SimulationResult | null;
  cursor: number;
  setCursor: (c: number) => void;
  onSelectEventTime?: (t: number) => void;
}

export const GanttChart: React.FC<GanttChartProps> = ({
  result,
  cursor,
  setCursor,
}) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(4); // 1 to 10
  const [tooltip, setTooltip] = useState<{ text: string; x: number; y: number } | null>(null);

  useEffect(() => {
    if (!isPlaying || !result || result.totalTime === 0) return;
    const intervalMs = Math.max(25, Math.floor(600 / speed));
    const timer = setInterval(() => {
      setCursor(prev => {
        if (prev >= result.totalTime) {
          setIsPlaying(false);
          return result.totalTime;
        }
        return prev + 1;
      });
    }, intervalMs);
    return () => clearInterval(timer);
  }, [isPlaying, speed, result, setCursor]);

  if (!result || result.totalTime === 0) {
    return (
      <div className="card-glow p-5 mb-5 text-center text-[#96a0bc] text-xs">
        No execution timeline yet. Click Run to generate the Gantt chart.
      </div>
    );
  }

  const total = Math.max(1, result.totalTime);

  return (
    <div className="card-glow p-4 mb-5 shadow-sm">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-[#ecf0fc] uppercase tracking-wider">
            Gantt Chart Execution Timeline
          </span>
          <span className="text-[11px] text-[#96a0bc]">
            ({result.segments.length} segments, {result.totalTime} units)
          </span>
        </div>
        <div className="flex items-center gap-4 text-[11px] text-[#96a0bc]">
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-[#7c5cff]"></span> Process
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-[#ff6880]"></span> Context Switch
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-[#2b3450]"></span> Idle
          </span>
        </div>
      </div>

      {/* Gantt Timeline Bar */}
      <div className="relative w-full bg-[#101426] border border-[#2b3450] rounded-xl h-14 overflow-hidden mb-3 select-none flex">
        {result.segments.map((seg, i) => {
          const duration = seg.endTime - seg.startTime;
          const pct = (duration / total) * 100;
          const isCs = seg.type === 'CONTEXT_SWITCH';
          const isIdle = seg.type === 'IDLE';

          return (
            <div
              key={i}
              onClick={() => setCursor(seg.startTime)}
              onMouseEnter={e => {
                const rect = e.currentTarget.getBoundingClientRect();
                setTooltip({
                  text: `${seg.processId}: ${seg.startTime} → ${seg.endTime} (${duration} units)`,
                  x: rect.left + rect.width / 2,
                  y: rect.top - 30,
                });
              }}
              onMouseLeave={() => setTooltip(null)}
              className={`h-full flex items-center justify-center relative cursor-pointer border-r border-[#151a2c]/60 transition-opacity hover:opacity-90 ${
                isCs ? 'gantt-cs' : isIdle ? 'gantt-idle' : ''
              }`}
              style={{
                width: `${pct}%`,
                backgroundColor: isCs ? '#ff6880' : isIdle ? '#1c233a' : seg.color,
              }}
            >
              <span className="text-[11px] font-extrabold text-white truncate px-1 drop-shadow-sm">
                {pct > 3 ? seg.processId : ''}
              </span>
            </div>
          );
        })}

        {/* Cursor Indicator */}
        <div
          className="absolute top-0 bottom-0 w-0.5 bg-[#38cfdc] pointer-events-none shadow-[0_0_8px_#38cfdc] transition-all"
          style={{ left: `${(cursor / total) * 100}%` }}
        >
          <div className="w-2.5 h-2.5 -ml-1 bg-[#38cfdc] rotate-45 transform -translate-y-1 shadow"></div>
        </div>
      </div>

      {/* Floating Tooltip */}
      {tooltip && (
        <div
          className="fixed z-50 px-2.5 py-1 rounded-md text-[11px] font-bold text-white bg-[#242c48] border border-[#3b4668] shadow-lg pointer-events-none transform -translate-x-1/2"
          style={{ left: tooltip.x, top: tooltip.y }}
        >
          {tooltip.text}
        </div>
      )}

      {/* Timeline ruler & ticks */}
      <div className="flex justify-between text-[10px] font-mono text-[#96a0bc] px-1 mb-3">
        <span>0</span>
        {result.totalTime > 20 && <span>{Math.floor(result.totalTime / 2)}</span>}
        <span>{result.totalTime} units</span>
      </div>

      {/* Controls */}
      <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-[#2b3450]/60">
        <div className="flex items-center space-x-1.5">
          <button
            onClick={() => setIsPlaying(!isPlaying)}
            className="px-3 py-1.5 rounded-lg bg-[#7c5cff] hover:bg-[#6a4ce5] text-white text-xs font-bold transition-all shadow-sm flex items-center gap-1.5"
          >
            {isPlaying ? (
              <>
                <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                  <path d="M6 4h4v16H6zm8 0h4v16h-4z" />
                </svg>
                Pause
              </>
            ) : (
              <>
                <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                  <path d="M8 5v14l11-7z" />
                </svg>
                Play
              </>
            )}
          </button>

          <button
            onClick={() => {
              setIsPlaying(false);
              setCursor(Math.min(total, cursor + 1));
            }}
            className="px-3 py-1.5 rounded-lg bg-[#1c233a] hover:bg-[#242c48] border border-[#2b3450] text-[#ecf0fc] text-xs font-semibold transition-all"
            title="Step 1 unit forward"
          >
            Step
          </button>

          <button
            onClick={() => {
              setIsPlaying(false);
              setCursor(0);
            }}
            className="px-3 py-1.5 rounded-lg bg-[#1c233a] hover:bg-[#242c48] border border-[#2b3450] text-[#ecf0fc] text-xs font-semibold transition-all"
            title="Restart playback"
          >
            Restart
          </button>

          <button
            onClick={() => {
              setIsPlaying(false);
              setCursor(total);
            }}
            className="px-3 py-1.5 rounded-lg bg-[#1c233a] hover:bg-[#242c48] border border-[#2b3450] text-[#ecf0fc] text-xs font-semibold transition-all"
            title="Skip to timeline end"
          >
            End
          </button>
        </div>

        {/* Speed Slider */}
        <div className="flex items-center space-x-3 text-xs text-[#96a0bc]">
          <span>Speed</span>
          <input
            type="range"
            min={1}
            max={10}
            value={speed}
            onChange={e => setSpeed(parseInt(e.target.value, 10))}
            className="w-28 accent-[#7c5cff] cursor-pointer"
          />
          <span className="font-mono text-white text-xs w-4">{speed}x</span>
        </div>
      </div>
    </div>
  );
};
