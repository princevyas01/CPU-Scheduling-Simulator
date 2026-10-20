import { ProcessInput, SimulationResult } from '../types/scheduler';

export function parseCsvWorkload(content: string): ProcessInput[] {
  if (!content || !content.trim()) {
    throw new Error('CSV file is empty.');
  }

  const lines = content.split(/\r?\n/).filter(line => line.trim().length > 0);
  if (lines.length < 2) {
    throw new Error('CSV must contain a header line and at least one process row.');
  }

  // Parse header
  const headerCols = parseCsvLine(lines[0]).map(c => c.toLowerCase().trim());
  const pidIdx = headerCols.findIndex(c => c.includes('process') || c === 'id' || c === 'pid');
  const atIdx = headerCols.findIndex(c => c.includes('arrival') || c === 'at');
  const btIdx = headerCols.findIndex(c => c.includes('burst') || c === 'bt');
  const prIdx = headerCols.findIndex(c => c.includes('priority') || c === 'pr');

  if (pidIdx === -1 || atIdx === -1 || btIdx === -1) {
    throw new Error('CSV header must contain Process ID, Arrival Time, and Burst Time columns.');
  }

  const processes: ProcessInput[] = [];
  const seenIds = new Set<string>();

  for (let rowIdx = 1; rowIdx < lines.length; rowIdx++) {
    const cols = parseCsvLine(lines[rowIdx]);
    if (cols.length <= Math.max(pidIdx, atIdx, btIdx)) {
      continue; // Skip malformed trailing lines
    }

    const id = cols[pidIdx].trim();
    if (!id) {
      throw new Error(`Row ${rowIdx + 1}: Process ID cannot be empty.`);
    }
    if (seenIds.has(id)) {
      throw new Error(`Row ${rowIdx + 1}: Duplicate Process ID "${id}" detected.`);
    }
    seenIds.add(id);

    const at = parseInt(cols[atIdx].trim(), 10);
    if (isNaN(at) || at < 0) {
      throw new Error(`Row ${rowIdx + 1} (${id}): Invalid Arrival Time "${cols[atIdx]}". Must be non-negative integer.`);
    }

    const bt = parseInt(cols[btIdx].trim(), 10);
    if (isNaN(bt) || bt <= 0) {
      throw new Error(`Row ${rowIdx + 1} (${id}): Invalid Burst Time "${cols[btIdx]}". Must be strictly positive integer.`);
    }

    let pr = 1;
    if (prIdx !== -1 && cols.length > prIdx && cols[prIdx].trim() !== '') {
      pr = parseInt(cols[prIdx].trim(), 10);
      if (isNaN(pr) || pr < 0) {
        throw new Error(`Row ${rowIdx + 1} (${id}): Invalid Priority "${cols[prIdx]}". Must be non-negative integer.`);
      }
    }

    processes.push({ id, arrivalTime: at, burstTime: bt, priority: pr });
  }

  if (processes.length === 0) {
    throw new Error('No valid process rows found in CSV file.');
  }

  return processes;
}

function parseCsvLine(line: string): string[] {
  const result: string[] = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (c === '"') {
      if (inQuotes && i + 1 < line.length && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (c === ',' && !inQuotes) {
      result.push(current);
      current = '';
    } else {
      current += c;
    }
  }
  result.push(current);
  return result;
}

export function exportResultToCsv(result: SimulationResult): string {
  const lines: string[] = [];
  lines.push('Process,Arrival Time,Burst Time,Configured Priority,Effective Priority,Completion Time,Turnaround Time,Waiting Time,Response Time');

  for (const p of result.processes) {
    lines.push(
      `"${p.id}",${p.arrivalTime},${p.burstTime},${p.configuredPriority},${p.effectivePriority},${p.completionTime},${p.turnaroundTime},${p.waitingTime},${p.responseTime}`
    );
  }

  lines.push('');
  lines.push(`Algorithm,${result.algorithmName}`);
  lines.push(`Total Elapsed Time,${result.totalTime}`);
  lines.push(`CPU Busy Time,${result.busyTime}`);
  lines.push(`CPU Idle Time,${result.idleTime}`);
  lines.push(`Context Switch Overhead Time,${result.contextSwitchTime}`);
  lines.push(`Context Switch Count,${result.contextSwitchCount}`);
  lines.push(`Average Waiting Time,${result.avgWaitingTime.toFixed(2)}`);
  lines.push(`Average Turnaround Time,${result.avgTurnaroundTime.toFixed(2)}`);
  lines.push(`Average Response Time,${result.avgResponseTime.toFixed(2)}`);
  lines.push(`Useful CPU Utilization %,${result.cpuUtilization.toFixed(2)}%`);
  lines.push(`Throughput,${result.throughput.toFixed(4)}`);
  lines.push(`Jain's Fairness Index,${result.jainsFairnessIndex.toFixed(4)}`);

  return lines.join('\n');
}

export function downloadFile(filename: string, content: string, mimeType: string) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
