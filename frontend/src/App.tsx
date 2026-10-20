import { useState, useEffect } from 'react';
import {
  ProcessInput,
  SimulationConfig,
  SimulationResult,
  AlgorithmId,
} from './types/scheduler';
import { runSimulation } from './engine/simulationEngine';
import { Navbar } from './components/Navbar';
import { MetricCards } from './components/MetricCards';
import { LiveCpuView } from './components/LiveCpuView';
import { GanttChart } from './components/GanttChart';
import { SimulatorTab } from './components/SimulatorTab';
import { DecisionLogTab } from './components/DecisionLogTab';
import { CompareTab } from './components/CompareTab';
import { ExperimentLabTab } from './components/ExperimentLabTab';
import { parseCsvWorkload, exportResultToCsv, downloadFile } from './utils/csvHandler';
import { generateHtmlReport } from './utils/htmlReportGenerator';

const INITIAL_PROCESSES: ProcessInput[] = [
  { id: 'P1', arrivalTime: 0, burstTime: 8, priority: 3 },
  { id: 'P2', arrivalTime: 1, burstTime: 4, priority: 1 },
  { id: 'P3', arrivalTime: 2, burstTime: 9, priority: 4 },
  { id: 'P4', arrivalTime: 3, burstTime: 5, priority: 2 },
  { id: 'P5', arrivalTime: 4, burstTime: 2, priority: 5 },
];

export function App() {
  const [activeTab, setActiveTab] = useState<'sim' | 'decision' | 'compare' | 'experiment'>('sim');
  const [algorithm, setAlgorithm] = useState<AlgorithmId>('FCFS');
  const [processes, setProcesses] = useState<ProcessInput[]>(INITIAL_PROCESSES);
  const [config, setConfig] = useState<SimulationConfig>({
    quantum: 3,
    contextSwitchOverhead: 0,
    agingEnabled: false,
    agingInterval: 5,
    priorityBoost: 1,
    minPriority: 0,
    starvationThreshold: 20,
  });

  const [result, setResult] = useState<SimulationResult | null>(null);
  const [cursor, setCursor] = useState<number>(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const executeSimulation = () => {
    try {
      setErrorMessage(null);
      const res = runSimulation(algorithm, processes, config);
      setResult(res);
      setCursor(0);
    } catch (err: any) {
      setErrorMessage(err.message || 'Simulation execution failed.');
    }
  };

  useEffect(() => {
    executeSimulation();
  }, [algorithm, processes, config]);

  const handleRandomize = () => {
    const count = 4 + Math.floor(Math.random() * 4); // 4 to 7 processes
    const generated: ProcessInput[] = [];
    for (let i = 0; i < count; i++) {
      generated.push({
        id: `P${i + 1}`,
        arrivalTime: Math.floor(Math.random() * 8),
        burstTime: 1 + Math.floor(Math.random() * 10),
        priority: 1 + Math.floor(Math.random() * 5),
      });
    }
    setProcesses(generated);
  };

  const handleImportCsv = (csvText: string) => {
    try {
      setErrorMessage(null);
      const parsed = parseCsvWorkload(csvText);
      setProcesses(parsed);
    } catch (err: any) {
      setErrorMessage(`CSV Import Error: ${err.message}`);
    }
  };

  const handleExportCsv = () => {
    if (!result) return;
    const csv = exportResultToCsv(result);
    downloadFile(`cpu_scheduling_${result.algorithmName.toLowerCase()}_results.csv`, csv, 'text/csv');
  };

  const handleExportHtml = () => {
    if (!result) return;
    const html = generateHtmlReport(result);
    downloadFile(`cpu_scheduling_${result.algorithmName.toLowerCase()}_report.html`, html, 'text/html');
  };

  const handleDrillDown = (algoId: AlgorithmId) => {
    setAlgorithm(algoId);
    setActiveTab('sim');
  };

  const handleLoadWorkload = (workload: ProcessInput[], algo: AlgorithmId) => {
    setProcesses(workload);
    setAlgorithm(algo);
    setActiveTab('sim');
  };

  const handleSelectTime = (t: number) => {
    setCursor(t);
    setActiveTab('sim');
  };

  return (
    <div className="min-h-screen flex flex-col">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onImportCsv={handleImportCsv}
        onExportCsv={handleExportCsv}
        onExportHtml={handleExportHtml}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-6">
        {errorMessage && (
          <div className="bg-[#ff6880]/20 border border-[#ff6880]/50 rounded-xl p-3.5 mb-5 text-xs text-[#ff6880] flex items-center justify-between shadow-lg">
            <div className="flex items-center gap-2 font-medium">
              <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              {errorMessage}
            </div>
            <button
              onClick={() => setErrorMessage(null)}
              className="text-[#96a0bc] hover:text-white text-xs font-bold"
            >
              &times; Dismiss
            </button>
          </div>
        )}

        {/* Top Telemetry & Timeline always visible on Simulator tab */}
        {activeTab === 'sim' && (
          <>
            <MetricCards result={result} />
            <LiveCpuView result={result} cursor={cursor} />
            <GanttChart result={result} cursor={cursor} setCursor={setCursor} />
          </>
        )}

        {/* Tab Views */}
        {activeTab === 'sim' && (
          <SimulatorTab
            algorithm={algorithm}
            setAlgorithm={setAlgorithm}
            processes={processes}
            setProcesses={setProcesses}
            config={config}
            setConfig={setConfig}
            result={result}
            onRun={executeSimulation}
            onRandomize={handleRandomize}
          />
        )}

        {activeTab === 'decision' && (
          <DecisionLogTab
            events={result?.events || []}
            onSelectTime={handleSelectTime}
          />
        )}

        {activeTab === 'compare' && (
          <CompareTab
            processes={processes}
            config={config}
            onDrillDown={handleDrillDown}
          />
        )}

        {activeTab === 'experiment' && (
          <ExperimentLabTab
            onLoadWorkloadToSimulator={handleLoadWorkload}
          />
        )}
      </main>

      <footer className="border-t border-[#2b3450] py-4 px-6 text-center text-xs text-[#96a0bc] bg-[#0a0d19]/80">
        CPU Scheduling Studio &bull; BTech Computer Engineering Project &bull; Fully Interactive React + TypeScript Simulation Suite
      </footer>
    </div>
  );
}

export default App;
