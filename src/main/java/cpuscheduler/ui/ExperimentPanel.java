package cpuscheduler.ui;

import cpuscheduler.experiment.ExperimentLab;
import cpuscheduler.experiment.ExperimentResult;
import cpuscheduler.experiment.WorkloadPreset;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.scheduler.SchedulingAlgorithmType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Locale;

/**
 * Experiment Lab UI:
 * Allows configuring workload presets, parameters, running multi-workload batch experiments
 * in a SwingWorker background thread, visualizing aggregated metrics, and inspecting
 * data-driven conclusions based on selected optimization objectives.
 */
public class ExperimentPanel extends JPanel {
    private final JComboBox<String> presetCombo;
    private final JSpinner processCountSpinner;
    private final JSpinner workloadCountSpinner;
    private final JTextField seedField;
    private final JSpinner quantumSpinner;
    private final JSpinner csSpinner;
    private final JCheckBox agingCheckBox;
    private final JComboBox<String> objectiveCombo;

    private final FlatButton runBtn;
    private final FlatButton cancelBtn;
    private final JProgressBar progressBar;
    private final JLabel progressLabel;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextArea conclusionArea;
    private final MetricCard bestWTCard, bestTATCard, bestRTCard, bestFairCard;

    private SwingWorker<ExperimentResult, Void> activeWorker;
    private ExperimentResult lastResult;

    public ExperimentPanel() {
        super(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(new EmptyBorder(14, 26, 14, 26));

        // Top Configuration Form
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);

        Card cfgCard = new Card("EXPERIMENT CONFIGURATION", "Configure workload presets, parameters, and batch evaluation", null);
        JPanel form = new JPanel(new GridLayout(2, 4, 12, 10));
        form.setOpaque(false);

        // Preset
        JPanel pPreset = boxCol("Workload Preset:");
        presetCombo = new JComboBox<>(WorkloadPreset.getDisplayNames());
        styleCombo(presetCombo);
        pPreset.add(presetCombo);
        form.add(pPreset);

        // Process Count
        JPanel pCount = boxCol("Process Count (N):");
        processCountSpinner = new JSpinner(new SpinnerNumberModel(6, 2, 30, 1));
        styleSpinner(processCountSpinner);
        pCount.add(processCountSpinner);
        form.add(pCount);

        // Workload Count
        JPanel pWCount = boxCol("Workload Iterations:");
        workloadCountSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        styleSpinner(workloadCountSpinner);
        pWCount.add(workloadCountSpinner);
        form.add(pWCount);

        // Random Seed
        JPanel pSeed = boxCol("Random Seed (optional):");
        seedField = new JTextField("42");
        seedField.setBackground(Theme.PANEL2);
        seedField.setForeground(Theme.TEXT);
        seedField.setCaretColor(Theme.CYAN);
        seedField.setFont(Theme.font(Font.PLAIN, 12));
        seedField.setBorder(new LineBorder(Theme.BORDER, 1, true));
        pSeed.add(seedField);
        form.add(pSeed);

        // Quantum
        JPanel pQ = boxCol("Time Quantum (RR/MLFQ):");
        quantumSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        styleSpinner(quantumSpinner);
        pQ.add(quantumSpinner);
        form.add(pQ);

        // CS Overhead
        JPanel pCS = boxCol("Context Switch Overhead:");
        csSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 5, 1));
        styleSpinner(csSpinner);
        pCS.add(csSpinner);
        form.add(pCS);

        // Priority Aging
        JPanel pAging = boxCol("Priority Aging:");
        agingCheckBox = new JCheckBox("Enable Aging", false);
        agingCheckBox.setOpaque(false);
        agingCheckBox.setForeground(Theme.TEXT);
        agingCheckBox.setFont(Theme.font(Font.PLAIN, 12));
        pAging.add(agingCheckBox);
        form.add(pAging);

        // Objective Selector
        JPanel pObj = boxCol("Optimization Objective:");
        objectiveCombo = new JComboBox<>(new String[]{
                "Min Waiting Time",
                "Min Turnaround Time",
                "Min Response Time",
                "Max Throughput",
                "Min Context Switches",
                "Max Fairness"
        });
        styleCombo(objectiveCombo);
        objectiveCombo.addActionListener(e -> updateWinnerHighlight());
        pObj.add(objectiveCombo);
        form.add(pObj);

        cfgCard.add(form, BorderLayout.CENTER);

        // Action & Progress Bar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        actions.setOpaque(false);
        runBtn = new FlatButton("Run Experiment", Theme.ACCENT, 1, true);
        runBtn.addActionListener(e -> startExperiment());
        cancelBtn = new FlatButton("Cancel", Theme.PANEL2, 0, false);
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(e -> cancelExperiment());
        actions.add(runBtn);
        actions.add(cancelBtn);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(220, 20));
        progressBar.setStringPainted(true);
        progressBar.setForeground(Theme.CYAN);
        progressBar.setBackground(Theme.PANEL2);
        actions.add(progressBar);

        progressLabel = Theme.lbl("Ready to run experiment", 11, Theme.MUTED, false);
        actions.add(progressLabel);

        cfgCard.add(actions, BorderLayout.SOUTH);
        top.add(cfgCard, BorderLayout.CENTER);

        // Metric Summary Cards
        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        bestWTCard = new MetricCard("OPTIMAL WAITING", Theme.AMBER, 2, "", "Lowest avg WT");
        bestTATCard = new MetricCard("OPTIMAL TURNAROUND", Theme.CYAN, 2, "", "Lowest avg TAT");
        bestRTCard = new MetricCard("OPTIMAL RESPONSE", Theme.GREEN, 2, "", "Lowest avg RT");
        bestFairCard = new MetricCard("MAX FAIRNESS", Theme.PURPLE, 3, "", "Highest Jain's Index");
        for (MetricCard m : new MetricCard[]{bestWTCard, bestTATCard, bestRTCard, bestFairCard}) {
            m.setText("—", "Run experiment to analyze");
            metrics.add(m);
        }
        top.add(metrics, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // Center: Results Table and Conclusions
        JPanel center = new JPanel(new GridLayout(1, 2, 14, 0));
        center.setOpaque(false);

        Card tableCard = new Card("EXPERIMENT BENCHMARK", "Average metric values aggregated across evaluated workloads", null);
        String[] cols = {"Algorithm", "Avg WT", "Avg TAT", "Avg RT", "CPU Util %", "Throughput", "Ctx Sw", "Max WT", "Jain's Fair"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);
        JScrollPane tsp = new JScrollPane(table);
        tsp.setBorder(new LineBorder(Theme.BORDER, 1, true));
        tsp.getViewport().setBackground(Theme.PANEL2);
        tableCard.add(tsp, BorderLayout.CENTER);
        center.add(tableCard);

        Card concCard = new Card("DATA-DRIVEN CONCLUSIONS", "Automated comparative analysis and scheduling trade-offs", null);
        conclusionArea = new JTextArea();
        conclusionArea.setEditable(false);
        conclusionArea.setLineWrap(true);
        conclusionArea.setWrapStyleWord(true);
        conclusionArea.setBackground(Theme.PANEL2);
        conclusionArea.setForeground(Theme.TEXT);
        conclusionArea.setFont(Theme.font(Font.PLAIN, 12));
        conclusionArea.setBorder(new EmptyBorder(12, 14, 12, 14));
        conclusionArea.setText("Run an experiment to generate comprehensive data-driven conclusions.");
        JScrollPane csp = new JScrollPane(conclusionArea);
        csp.setBorder(new LineBorder(Theme.BORDER, 1, true));
        concCard.add(csp, BorderLayout.CENTER);
        center.add(concCard);

        add(center, BorderLayout.CENTER);
    }

    private void startExperiment() {
        if (activeWorker != null && !activeWorker.isDone()) return;

        WorkloadPreset preset = WorkloadPreset.fromDisplayName((String) presetCombo.getSelectedItem());
        int procCount = (Integer) processCountSpinner.getValue();
        int workCount = (Integer) workloadCountSpinner.getValue();
        int quantum = (Integer) quantumSpinner.getValue();
        int cs = (Integer) csSpinner.getValue();
        boolean aging = agingCheckBox.isSelected();

        Long tempSeed = null;
        String seedTxt = seedField.getText().trim();
        if (!seedTxt.isEmpty()) {
            try {
                tempSeed = Long.parseLong(seedTxt);
            } catch (NumberFormatException ignored) {}
        }
        final Long seed = tempSeed;

        SimulationConfig config = new SimulationConfig(quantum, cs, aging, 5, 1, 0, 15);

        runBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
        progressBar.setValue(0);
        progressLabel.setText("Running experiment...");

        activeWorker = new SwingWorker<ExperimentResult, Void>() {
            protected ExperimentResult doInBackground() {
                return ExperimentLab.runExperiment(preset, procCount, workCount, config, seed, (cur, total, msg) -> {
                    if (isCancelled()) return;
                    int pct = (int) ((cur / (double) total) * 100);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(pct);
                        progressLabel.setText(msg);
                    });
                });
            }

            protected void done() {
                runBtn.setEnabled(true);
                cancelBtn.setEnabled(false);
                try {
                    if (!isCancelled()) {
                        lastResult = get();
                        displayResults(lastResult);
                        progressLabel.setText("Completed " + workCount + " workload(s) across 10 algorithms.");
                        progressBar.setValue(100);
                    } else {
                        progressLabel.setText("Experiment cancelled.");
                    }
                } catch (Exception ex) {
                    progressLabel.setText("Error: " + ex.getMessage());
                }
            }
        };

        activeWorker.execute();
    }

    private void cancelExperiment() {
        if (activeWorker != null) {
            activeWorker.cancel(true);
        }
    }

    private void displayResults(ExperimentResult res) {
        tableModel.setRowCount(0);
        if (res == null) return;

        for (SchedulingAlgorithmType algo : SchedulingAlgorithmType.values()) {
            ExperimentResult.AlgoStats s = res.getStats(algo);
            if (s != null) {
                tableModel.addRow(new Object[]{
                        algo.getShortName(),
                        Theme.f2(s.avgWaitingTime),
                        Theme.f2(s.avgTurnaroundTime),
                        Theme.f2(s.avgResponseTime),
                        String.format(Locale.US, "%.1f%%", s.cpuUtilization),
                        String.format(Locale.US, "%.3f", s.throughput),
                        String.format(Locale.US, "%.1f", s.contextSwitches),
                        String.format(Locale.US, "%.0f", s.maxWaitingTime),
                        String.format(Locale.US, "%.3f", s.jainsFairnessIndex)
                });
            }
        }

        SchedulingAlgorithmType wWT = res.getWinner("Min Waiting Time");
        SchedulingAlgorithmType wTAT = res.getWinner("Min Turnaround Time");
        SchedulingAlgorithmType wRT = res.getWinner("Min Response Time");
        SchedulingAlgorithmType wFair = res.getWinner("Max Fairness");

        if (wWT != null) bestWTCard.setText(wWT.getShortName(), Theme.f2(res.getStats(wWT).avgWaitingTime) + " units");
        if (wTAT != null) bestTATCard.setText(wTAT.getShortName(), Theme.f2(res.getStats(wTAT).avgTurnaroundTime) + " units");
        if (wRT != null) bestRTCard.setText(wRT.getShortName(), Theme.f2(res.getStats(wRT).avgResponseTime) + " units");
        if (wFair != null) bestFairCard.setText(wFair.getShortName(), String.format(Locale.US, "%.3f", res.getStats(wFair).jainsFairnessIndex));

        conclusionArea.setText(res.getConclusionText());
        updateWinnerHighlight();
    }

    private void updateWinnerHighlight() {
        if (lastResult == null) return;
        String objective = (String) objectiveCombo.getSelectedItem();
        SchedulingAlgorithmType winner = lastResult.getWinner(objective);
        if (winner != null) {
            for (int r = 0; r < table.getRowCount(); r++) {
                String name = (String) table.getValueAt(r, 0);
                if (winner.getShortName().equals(name)) {
                    table.setRowSelectionInterval(r, r);
                    break;
                }
            }
        }
    }

    private static JPanel boxCol(String label) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(Theme.lbl(label, 11, Theme.TEXT, true));
        p.add(Box.createVerticalStrut(4));
        return p;
    }

    private static void styleCombo(JComboBox<?> c) {
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        c.setPreferredSize(new Dimension(140, 32));
        c.setBackground(Theme.PANEL2);
        c.setForeground(Theme.TEXT);
        c.setFont(Theme.font(Font.PLAIN, 12));
    }

    private static void styleSpinner(JSpinner s) {
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        s.setPreferredSize(new Dimension(100, 32));
        s.setFont(Theme.font(Font.BOLD, 12));
    }

    private static void styleTable(JTable t) {
        t.setRowHeight(28);
        t.setShowGrid(false);
        t.setBackground(Theme.PANEL2);
        t.setForeground(Theme.TEXT);
        t.setSelectionBackground(new Color(64, 58, 120));
        t.setSelectionForeground(Color.WHITE);
        t.setFont(Theme.font(Font.PLAIN, 11));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(100, 30));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, false, false, row, column);
                setHorizontalAlignment(CENTER);
                setBackground(new Color(34, 42, 68));
                setForeground(Theme.CYAN);
                setFont(Theme.font(Font.BOLD, 10));
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
                return this;
            }
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                setHorizontalAlignment(CENTER);
                setBackground(isSelected ? new Color(64, 58, 120) : (row % 2 == 0 ? Theme.PANEL2 : Theme.mix(Theme.PANEL2, Theme.BG, .22)));
                if (column == 0) {
                    setForeground(Theme.CYAN);
                    setFont(Theme.font(Font.BOLD, 11));
                } else {
                    setForeground(Theme.TEXT);
                    setFont(Theme.font(Font.PLAIN, 11));
                }
                return this;
            }
        });
    }
}
