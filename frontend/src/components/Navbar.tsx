import React, { useRef } from 'react';

interface NavbarProps {
  activeTab: 'sim' | 'decision' | 'compare' | 'experiment';
  setActiveTab: (tab: 'sim' | 'decision' | 'compare' | 'experiment') => void;
  onImportCsv: (content: string) => void;
  onExportCsv: () => void;
  onExportHtml: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  onImportCsv,
  onExportCsv,
  onExportHtml,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = evt => {
      const text = evt.target?.result as string;
      if (text) onImportCsv(text);
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  return (
    <header className="border-b border-[#2b3450] bg-[#101426]/90 backdrop-blur sticky top-0 z-50 px-6 py-3 flex items-center justify-between">
      <div className="flex items-center space-x-6">
        <div className="flex items-center space-x-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-[#7c5cff] to-[#38cfdc] flex items-center justify-center shadow-lg shadow-[#7c5cff]/20">
            <svg className="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.2} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
            </svg>
          </div>
          <div>
            <div className="font-extrabold text-[16px] tracking-tight bg-gradient-to-r from-white via-[#ecf0fc] to-[#96a0bc] bg-clip-text text-transparent">
              CPU Scheduling Studio
            </div>
            <div className="text-[11px] text-[#38cfdc] font-medium flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-[#4ade80] animate-pulse"></span>
              OS Simulation Laboratory
            </div>
          </div>
        </div>

        {/* Tab Buttons */}
        <nav className="flex space-x-1 bg-[#151a2c] p-1 rounded-xl border border-[#2b3450]">
          <button
            onClick={() => setActiveTab('sim')}
            className={`px-4 py-1.5 text-xs font-semibold rounded-lg transition-all ${
              activeTab === 'sim'
                ? 'bg-gradient-to-r from-[#7c5cff] to-[#6096ff] text-white shadow-md'
                : 'text-[#96a0bc] hover:text-white hover:bg-white/5'
            }`}
          >
            Simulator
          </button>
          <button
            onClick={() => setActiveTab('decision')}
            className={`px-4 py-1.5 text-xs font-semibold rounded-lg transition-all ${
              activeTab === 'decision'
                ? 'bg-gradient-to-r from-[#7c5cff] to-[#6096ff] text-white shadow-md'
                : 'text-[#96a0bc] hover:text-white hover:bg-white/5'
            }`}
          >
            Decision Log
          </button>
          <button
            onClick={() => setActiveTab('compare')}
            className={`px-4 py-1.5 text-xs font-semibold rounded-lg transition-all ${
              activeTab === 'compare'
                ? 'bg-gradient-to-r from-[#7c5cff] to-[#6096ff] text-white shadow-md'
                : 'text-[#96a0bc] hover:text-white hover:bg-white/5'
            }`}
          >
            Compare All
          </button>
          <button
            onClick={() => setActiveTab('experiment')}
            className={`px-4 py-1.5 text-xs font-semibold rounded-lg transition-all ${
              activeTab === 'experiment'
                ? 'bg-gradient-to-r from-[#7c5cff] to-[#6096ff] text-white shadow-md'
                : 'text-[#96a0bc] hover:text-white hover:bg-white/5'
            }`}
          >
            Experiment Lab
          </button>
        </nav>
      </div>

      {/* Action Buttons */}
      <div className="flex items-center space-x-2.5">
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileChange}
          accept=".csv"
          className="hidden"
        />
        <button
          onClick={() => fileInputRef.current?.click()}
          className="px-3 py-1.5 rounded-lg border border-[#2b3450] bg-[#1c233a] hover:bg-[#242c48] text-xs font-semibold text-[#ecf0fc] transition-all flex items-center gap-1.5"
          title="Import CSV workload with columns Process, Arrival Time, Burst Time, Priority"
        >
          <svg className="w-3.5 h-3.5 text-[#38cfdc]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
          </svg>
          Import CSV
        </button>
        <button
          onClick={onExportCsv}
          className="px-3 py-1.5 rounded-lg border border-[#2b3450] bg-[#1c233a] hover:bg-[#242c48] text-xs font-semibold text-[#ecf0fc] transition-all flex items-center gap-1.5"
          title="Export process metrics and summary statistics to CSV"
        >
          <svg className="w-3.5 h-3.5 text-[#ffb84d]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
          Export CSV
        </button>
        <button
          onClick={onExportHtml}
          className="px-3 py-1.5 rounded-lg border border-[#7c5cff]/40 bg-[#7c5cff]/15 hover:bg-[#7c5cff]/25 text-xs font-semibold text-[#a38bff] transition-all flex items-center gap-1.5"
          title="Generate self-contained, offline HTML report with SVG Gantt chart"
        >
          <svg className="w-3.5 h-3.5 text-[#7c5cff]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          HTML Report
        </button>
      </div>
    </header>
  );
};
