import React from 'react';
import { SimulationResult } from '../types/scheduler';

interface MetricCardsProps {
  result: SimulationResult | null;
}

export const MetricCards: React.FC<MetricCardsProps> = ({ result }) => {
  const cards = [
    {
      label: 'AVG WAITING',
      value: result ? result.avgWaitingTime.toFixed(2) : '—',
      color: '#ffb84d',
      formula: 'WT = TAT − BT',
    },
    {
      label: 'AVG TURNAROUND',
      value: result ? result.avgTurnaroundTime.toFixed(2) : '—',
      color: '#38cfdc',
      formula: 'TAT = CT − AT',
    },
    {
      label: 'AVG RESPONSE',
      value: result ? result.avgResponseTime.toFixed(2) : '—',
      color: '#4ade80',
      formula: 'RT = start − AT',
    },
    {
      label: 'CPU UTILISATION',
      value: result ? `${result.cpuUtilization.toFixed(1)}%` : '—',
      color: '#7c5cff',
      formula: 'busy time ÷ total',
    },
    {
      label: 'THROUGHPUT',
      value: result ? result.throughput.toFixed(3) : '—',
      color: '#ff6880',
      formula: 'processes / tick',
    },
    {
      label: 'CONTEXT SWITCHES',
      value: result ? result.contextSwitchCount.toString() : '—',
      color: '#6096ff',
      formula: 'process → process',
    },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 mb-5">
      {cards.map((c, i) => (
        <div
          key={i}
          className="metric-card-bg p-3.5 flex flex-col justify-between shadow-sm hover:border-[#3b4668] transition-all"
        >
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold tracking-wider text-[#96a0bc] uppercase">
              {c.label}
            </span>
            <span
              className="w-2 h-2 rounded-full"
              style={{ backgroundColor: c.color }}
            />
          </div>
          <div
            className="text-2xl font-extrabold tracking-tight my-1.5"
            style={{ color: c.color }}
          >
            {c.value}
          </div>
          <div className="text-[10px] text-[#96a0bc] font-mono">
            {c.formula}
          </div>
        </div>
      ))}
    </div>
  );
};
