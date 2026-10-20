import { SimulationResult } from '../types/scheduler';

export function generateHtmlReport(result: SimulationResult): string {
  const now = new Date().toLocaleString();

  const processRows = result.processes
    .map(
      p => `
      <tr>
        <td style="font-weight: bold; color: ${p.color};">${escapeHtml(p.id)}</td>
        <td>${p.arrivalTime}</td>
        <td>${p.burstTime}</td>
        <td>${p.configuredPriority}</td>
        <td>${p.effectivePriority}</td>
        <td>${p.completionTime}</td>
        <td style="color: #38cfdc; font-weight: 600;">${p.turnaroundTime}</td>
        <td style="color: #ffb84d; font-weight: 600;">${p.waitingTime}</td>
        <td style="color: #4ade80; font-weight: 600;">${p.responseTime}</td>
      </tr>`
    )
    .join('');

  // Gantt SVG
  const totalT = Math.max(1, result.totalTime);
  const svgWidth = 900;
  const svgHeight = 60;
  const rects = result.segments
    .map(seg => {
      const x = (seg.startTime / totalT) * svgWidth;
      const w = Math.max(2, ((seg.endTime - seg.startTime) / totalT) * svgWidth);
      const fill = seg.type === 'PROCESS' ? seg.color : seg.type === 'CONTEXT_SWITCH' ? '#ff6880' : '#2b3450';
      return `<rect x="${x}" y="10" width="${w}" height="40" rx="4" fill="${fill}" stroke="#151a2c" stroke-width="1.5">
        <title>${escapeHtml(seg.processId)}: ${seg.startTime} -> ${seg.endTime}</title>
      </rect>
      <text x="${x + w / 2}" y="34" font-size="11" font-weight="bold" fill="#ffffff" text-anchor="middle">${w > 20 ? escapeHtml(seg.processId) : ''}</text>`;
    })
    .join('');

  // Decision log top events
  const eventRows = result.events
    .slice(0, 25)
    .map(
      e => `
      <tr>
        <td style="color: #38cfdc; font-weight: bold;">t = ${e.time}</td>
        <td><span class="badge badge-event">${escapeHtml(e.eventType)}</span></td>
        <td style="font-weight: 600;">${escapeHtml(e.processId)}</td>
        <td style="font-family: monospace; font-size: 11px;">${escapeHtml(e.readyProcesses)}</td>
        <td>${escapeHtml(e.decisionReason)}</td>
        <td style="color: #4ade80;">${escapeHtml(e.outcome)}</td>
      </tr>`
    )
    .join('');

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>CPU Scheduling Studio — ${escapeHtml(result.algorithmName)} Report</title>
  <style>
    body {
      font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
      background: #0a0d19;
      color: #ecf0fc;
      margin: 0;
      padding: 36px 48px;
    }
    .header {
      border-bottom: 2px solid #2b3450;
      padding-bottom: 18px;
      margin-bottom: 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    h1 { margin: 0; color: #7c5cff; font-size: 26px; }
    .subtitle { color: #96a0bc; font-size: 13px; margin-top: 4px; }
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(6, 1fr);
      gap: 14px;
      margin-bottom: 30px;
    }
    .kpi-card {
      background: #151a2c;
      border: 1px solid #2b3450;
      border-radius: 10px;
      padding: 14px;
      text-align: center;
    }
    .kpi-title { font-size: 11px; font-weight: 700; color: #96a0bc; text-transform: uppercase; }
    .kpi-value { font-size: 24px; font-weight: 800; margin: 6px 0; }
    .section-title {
      font-size: 16px;
      font-weight: 700;
      color: #38cfdc;
      margin: 28px 0 12px 0;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      background: #151a2c;
      border-radius: 8px;
      overflow: hidden;
      margin-bottom: 24px;
      font-size: 13px;
    }
    th {
      background: #242c48;
      color: #38cfdc;
      text-align: left;
      padding: 10px 14px;
      font-weight: 600;
    }
    td {
      padding: 9px 14px;
      border-bottom: 1px solid #2b3450;
    }
    .gantt-wrap {
      background: #151a2c;
      border: 1px solid #2b3450;
      border-radius: 10px;
      padding: 16px;
      margin-bottom: 24px;
    }
    .badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
      background: rgba(124, 92, 255, 0.2);
      color: #7c5cff;
    }
    .footer {
      border-top: 1px solid #2b3450;
      padding-top: 16px;
      margin-top: 36px;
      font-size: 12px;
      color: #96a0bc;
      text-align: center;
    }
    @media print {
      body { background: white; color: black; padding: 20px; }
      .kpi-card, table, .gantt-wrap { background: #f8f9fa; border-color: #dee2e6; color: black; }
      th { background: #e9ecef; color: black; }
      td { border-bottom-color: #dee2e6; color: black; }
    }
  </style>
</head>
<body>

  <div class="header">
    <div>
      <h1>CPU Scheduling Studio — Simulation Report</h1>
      <div class="subtitle">Algorithm: <strong>${escapeHtml(result.algorithmName)}</strong> &bull; Generated on: ${now}</div>
    </div>
    <div style="text-align: right;">
      <span class="badge">BTech Mini-Project Report</span>
    </div>
  </div>

  <div class="kpi-grid">
    <div class="kpi-card">
      <div class="kpi-title">Avg Waiting Time</div>
      <div class="kpi-value" style="color: #ffb84d;">${result.avgWaitingTime.toFixed(2)}</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-title">Avg Turnaround</div>
      <div class="kpi-value" style="color: #38cfdc;">${result.avgTurnaroundTime.toFixed(2)}</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-title">Avg Response Time</div>
      <div class="kpi-value" style="color: #4ade80;">${result.avgResponseTime.toFixed(2)}</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-title">CPU Utilization</div>
      <div class="kpi-value" style="color: #7c5cff;">${result.cpuUtilization.toFixed(1)}%</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-title">Throughput</div>
      <div class="kpi-value" style="color: #ff6880;">${result.throughput.toFixed(3)}</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-title">Jain's Fairness</div>
      <div class="kpi-value" style="color: #6096ff;">${result.jainsFairnessIndex.toFixed(3)}</div>
    </div>
  </div>

  <div class="section-title">Execution Timeline (Gantt Chart)</div>
  <div class="gantt-wrap">
    <svg viewBox="0 0 ${svgWidth} ${svgHeight}" width="100%" height="${svgHeight}">
      ${rects}
    </svg>
    <div style="display: flex; justify-content: space-between; font-size: 11px; color: #96a0bc; margin-top: 6px;">
      <span>0</span>
      <span>Total Units: ${result.totalTime}</span>
    </div>
  </div>

  <div class="section-title">Process Results</div>
  <table>
    <thead>
      <tr>
        <th>Process</th>
        <th>Arrival</th>
        <th>Burst</th>
        <th>Config PR</th>
        <th>Effective PR</th>
        <th>Completion</th>
        <th>Turnaround</th>
        <th>Waiting</th>
        <th>Response</th>
      </tr>
    </thead>
    <tbody>
      ${processRows}
    </tbody>
  </table>

  <div class="section-title">Decision Explanation Trace (First 25 Events)</div>
  <table>
    <thead>
      <tr>
        <th>Time</th>
        <th>Event</th>
        <th>Process</th>
        <th>Ready Queue</th>
        <th>Reason</th>
        <th>Outcome</th>
      </tr>
    </thead>
    <tbody>
      ${eventRows}
    </tbody>
  </table>

  <div class="footer">
    Generated by <strong>CPU Scheduling Studio</strong> &bull; Developed by Prince Vyas &bull; Offline Self-Contained Document
  </div>

</body>
</html>`;
}

function escapeHtml(text: string): string {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
