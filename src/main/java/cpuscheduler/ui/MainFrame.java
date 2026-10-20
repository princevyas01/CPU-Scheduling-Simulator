package cpuscheduler.ui;

import cpuscheduler.export.CsvExporter;
import cpuscheduler.export.CsvImporter;
import cpuscheduler.export.HtmlReportExporter;
import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationConfig;
import cpuscheduler.model.SimulationResult;
import cpuscheduler.scheduler.SchedulingAlgorithmType;
import cpuscheduler.simulation.SimulationEngine;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Main application window for CPU Scheduling Studio.
 * Preserves the dark navy/slate theme, custom vector controls, and rich animations
 * while modularly integrating Decision Log, Experiment Lab, and Advanced Comparisons.
 */
public class MainFrame extends JFrame {
    private final CardLayout viewsLayout = new CardLayout();
    private final JPanel viewsPanel = new JPanel(viewsLayout);

    private FlatButton tabSim, tabDec, tabCmp, tabExp;
    private JLabel statusLabel, statusHintLabel;

    // Simulator Tab Components
    private final DefaultTableModel inputModel = new DefaultTableModel(
            new Object[]{"PID", "Arrival", "Burst", "Priority"}, 0) {
        public boolean isCellEditable(int r, int c) { return c != 0; }
        public Class<?> getColumnClass(int c) { return c == 0 ? String.class : Integer.class; }
    };
    private final DefaultTableModel outputModel = new DefaultTableModel(
            new Object[]{"PID", "Arrival", "Burst", "Priority", "Start", "CT", "TAT", "WT", "RT"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private JTable inTable, outTable;
    private JComboBox<String> algoCombo;
    private JTextArea algoDescArea;
    private Stepper quantumStepper;
    private Stepper csOverheadStepper;
    private JCheckBox agingCheckBox;
    private Stepper agingIntervalStepper;
    private JCheckBox animateCheckBox;
    private JSlider speedSlider;
    private FlatButton playBtn;

    private MetricCard mWT, mTAT, mRT, mUtil, mThr, mSw;
    private final GanttPanel ganttPanel = new GanttPanel();
    private final LiveView liveView = new LiveView();
    private final StarvationMonitorPanel starvationPanel = new StarvationMonitorPanel();

    // Additional Tabs
    private final DecisionLogPanel decisionLogPanel = new DecisionLogPanel();
    private final ComparisonPanel comparisonPanel = new ComparisonPanel();
    private final ExperimentPanel experimentPanel = new ExperimentPanel();

    // State
    private SimulationResult currentResult;
    private final List<SimulationResult> comparisonResults = new ArrayList<>();
    private int cursor;
    private final javax.swing.Timer playTimer = new javax.swing.Timer(150, e -> onAnimationTick());

    public MainFrame() {
        super("CPU Scheduling Studio");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Rectangle scr = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1360, scr.width), Math.min(900, scr.height));
        setMinimumSize(new Dimension(Math.min(1050, scr.width), Math.min(680, scr.height)));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                g.setPaint(new GradientPaint(0, 0, Theme.BG2, getWidth(), getHeight(), Theme.BG));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(new RadialGradientPaint(new Point2D.Float(getWidth() * 0.88f, 0), 520f,
                        new float[]{0f, 1f}, new Color[]{Theme.alpha(Theme.ACCENT, 70), Theme.alpha(Theme.ACCENT, 0)}));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(new RadialGradientPaint(new Point2D.Float(0, getHeight()), 480f,
                        new float[]{0f, 1f}, new Color[]{Theme.alpha(Theme.CYAN, 34), Theme.alpha(Theme.CYAN, 0)}));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        setContentPane(root);

        viewsPanel.setOpaque(false);
        viewsPanel.add(buildSimulatorView(), "sim");
        viewsPanel.add(decisionLogPanel, "dec");
        viewsPanel.add(comparisonPanel, "cmp");
        viewsPanel.add(experimentPanel, "exp");

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(viewsPanel, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        // Timeline navigation from decision log
        decisionLogPanel.setTimeNavigationListener(time -> {
            pausePlay();
            cursor = time;
            syncView();
            showView("sim");
        });

        // Drill-down from comparison leaderboard
        comparisonPanel.setDrillDownListener(shortName -> {
            for (SchedulingAlgorithmType t : SchedulingAlgorithmType.values()) {
                if (t.getShortName().equalsIgnoreCase(shortName)) {
                    algoCombo.setSelectedItem(t.getDisplayName());
                    showView("sim");
                    runSimulation();
                    break;
                }
            }
        });

        // Ctrl+Enter shortcut
        getRootPane().registerKeyboardAction(e -> runSimulation(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        loadDemoWorkload();
        runSimulation();
        showView("sim");
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 26, 12, 26));

        // Logo & Titles
        JPanel west = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        west.setOpaque(false);
        west.add(new Logo());
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Theme.lbl("CPU SCHEDULING STUDIO", 22, Theme.TEXT, true));
        titles.add(Box.createVerticalStrut(3));
        titles.add(Theme.lbl("Interactive Operating System Scheduling Laboratory & Simulation Engine", 12, Theme.MUTED, false));
        west.add(titles);
        p.add(west, BorderLayout.WEST);

        // Tab Navigation
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        nav.setOpaque(false);
        tabSim = new FlatButton("Simulator", Theme.ACCENT, 0, false);
        tabDec = new FlatButton("Decision Log", Theme.ACCENT, 0, false);
        tabCmp = new FlatButton("Compare All", Theme.ACCENT, 0, false);
        tabExp = new FlatButton("Experiment Lab", Theme.ACCENT, 0, false);

        for (FlatButton b : new FlatButton[]{tabSim, tabDec, tabCmp, tabExp}) b.tab = true;

        tabSim.addActionListener(e -> showView("sim"));
        tabDec.addActionListener(e -> showView("dec"));
        tabCmp.addActionListener(e -> showView("cmp"));
        tabExp.addActionListener(e -> showView("exp"));

        nav.add(tabSim);
        nav.add(tabDec);
        nav.add(tabCmp);
        nav.add(tabExp);
        p.add(nav, BorderLayout.CENTER);

        // Pill
        JLabel pill = new JLabel("JAVA SWING") {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                g.setColor(Theme.PANEL2);
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
                g.setColor(Theme.CYAN);
                g.fill(new Ellipse2D.Double(12, getHeight() / 2.0 - 3, 6, 6));
                g.dispose();
                super.paintComponent(g0);
            }
        };
        pill.setFont(Theme.font(Font.BOLD, 11));
        pill.setForeground(Theme.CYAN);
        pill.setBorder(new EmptyBorder(9, 28, 9, 14));
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        east.setOpaque(false);
        east.add(pill);
        p.add(east, BorderLayout.EAST);

        return p;
    }

    private void showView(String name) {
        viewsLayout.show(viewsPanel, name);
        tabSim.selected = name.equals("sim");
        tabDec.selected = name.equals("dec");
        tabCmp.selected = name.equals("cmp");
        tabExp.selected = name.equals("exp");
        tabSim.repaint();
        tabDec.repaint();
        tabCmp.repaint();
        tabExp.repaint();
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(4, 28, 12, 28));
        statusLabel = Theme.lbl("\u25CF Ready", 12, Theme.CYAN, true);
        statusHintLabel = Theme.lbl("Ctrl+Enter to run  \u2022  click cell to edit  \u2022  hover Gantt blocks for details", 11, Theme.MUTED, false);
        p.add(statusLabel, BorderLayout.WEST);
        p.add(statusHintLabel, BorderLayout.EAST);
        return p;
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText("\u25CF " + text);
        statusLabel.setForeground(color);
    }

    // -------------------------------------------------------------
    // Simulator Tab View
    // -------------------------------------------------------------
    private JScrollPane buildSimulatorView() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(4, 26, 4, 26));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;

        // KPI Summary Cards
        JPanel metrics = new JPanel(new GridLayout(1, 6, 12, 0));
        metrics.setOpaque(false);
        mWT = new MetricCard("AVG WAITING", Theme.AMBER, 2, "", "WT = TAT \u2212 BT");
        mTAT = new MetricCard("AVG TURNAROUND", Theme.CYAN, 2, "", "TAT = CT \u2212 AT");
        mRT = new MetricCard("AVG RESPONSE", Theme.GREEN, 2, "", "RT = start \u2212 AT");
        mUtil = new MetricCard("CPU UTILISATION", Theme.ACCENT, 1, "%", "busy \u00F7 total");
        mThr = new MetricCard("THROUGHPUT", Theme.ROSE, 3, "", "processes / time");
        mSw = new MetricCard("CONTEXT SWITCHES", Theme.BLUE, 0, "", "direct process switches");
        for (MetricCard m : new MetricCard[]{mWT, mTAT, mRT, mUtil, mThr, mSw}) metrics.add(m);

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.weightx = 1; c.weighty = 0;
        c.insets = new Insets(0, 0, 14, 0);
        root.add(metrics, c);

        // Left Column (Config & Processes)
        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        GridBagConstraints lc = new GridBagConstraints();
        lc.fill = GridBagConstraints.BOTH;
        lc.gridx = 0; lc.weightx = 1;

        lc.gridy = 0; lc.weighty = 0;
        lc.insets = new Insets(0, 0, 14, 0);
        left.add(buildConfigCard(), lc);

        lc.gridy = 1; lc.weighty = 1;
        lc.insets = new Insets(0, 0, 0, 0);
        left.add(buildProcessCard(), lc);

        // Right Column (Gantt, Results, Starvation)
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        GridBagConstraints rc = new GridBagConstraints();
        rc.fill = GridBagConstraints.BOTH;
        rc.gridx = 0; rc.weightx = 1;

        rc.gridy = 0; rc.weighty = 0;
        rc.insets = new Insets(0, 0, 14, 0);
        right.add(buildGanttCard(), rc);

        rc.gridy = 1; rc.weighty = 1;
        rc.insets = new Insets(0, 0, 0, 0);
        right.add(buildResultsCard(), rc);

        c.gridy = 1; c.gridwidth = 1; c.weighty = 1; c.insets = new Insets(0, 0, 0, 14);
        c.gridx = 0; c.weightx = 0.36;
        root.add(left, c);

        c.gridx = 1; c.weightx = 0.64; c.insets = new Insets(0, 0, 0, 0);
        root.add(right, c);

        JScrollPane sp = new JScrollPane(root);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        return sp;
    }

    private JPanel buildConfigCard() {
        Card card = new Card("CONFIGURATION", "Select scheduling policy and simulation parameters", null);
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(Theme.lbl("Scheduling Algorithm", 12, Theme.TEXT, true));
        form.add(Box.createVerticalStrut(6));

        algoCombo = new JComboBox<>(SchedulingAlgorithmType.getDisplayNames());
        algoCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        algoCombo.setPreferredSize(new Dimension(200, 36));
        algoCombo.setBackground(Theme.PANEL2);
        algoCombo.setForeground(Theme.TEXT);
        algoCombo.setFont(Theme.font(Font.BOLD, 12));
        form.add(algoCombo);
        form.add(Box.createVerticalStrut(8));

        algoDescArea = new JTextArea(4, 20);
        algoDescArea.setEditable(false);
        algoDescArea.setLineWrap(true);
        algoDescArea.setWrapStyleWord(true);
        algoDescArea.setOpaque(false);
        algoDescArea.setForeground(Theme.MUTED);
        algoDescArea.setFont(Theme.font(Font.PLAIN, 11));
        algoDescArea.setBorder(new EmptyBorder(0, 2, 0, 2));
        form.add(algoDescArea);
        form.add(Box.createVerticalStrut(8));

        // Time Quantum
        form.add(Theme.lbl("Time Quantum (RR / MLFQ Base)", 11, Theme.TEXT, true));
        form.add(Box.createVerticalStrut(4));
        quantumStepper = new Stepper(2, 1, 50);
        form.add(quantumStepper);
        form.add(Box.createVerticalStrut(8));

        // Context Switch Overhead
        form.add(Theme.lbl("Context-Switch Overhead (time units)", 11, Theme.TEXT, true));
        form.add(Box.createVerticalStrut(4));
        csOverheadStepper = new Stepper(0, 0, 5);
        form.add(csOverheadStepper);
        form.add(Box.createVerticalStrut(8));

        // Priority Aging
        JPanel agingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        agingRow.setOpaque(false);
        agingCheckBox = new JCheckBox("Enable Priority Aging", false);
        agingCheckBox.setOpaque(false);
        agingCheckBox.setForeground(Theme.TEXT);
        agingCheckBox.setFont(Theme.font(Font.PLAIN, 11));
        agingRow.add(agingCheckBox);
        agingRow.add(Theme.lbl("Interval:", 11, Theme.MUTED, false));
        agingIntervalStepper = new Stepper(5, 1, 20);
        agingIntervalStepper.setPreferredSize(new Dimension(100, 30));
        agingRow.add(agingIntervalStepper);
        form.add(agingRow);
        form.add(Box.createVerticalStrut(8));

        animateCheckBox = new JCheckBox("Animate Gantt chart upon running", true);
        animateCheckBox.setOpaque(false);
        animateCheckBox.setForeground(Theme.TEXT);
        animateCheckBox.setFont(Theme.font(Font.PLAIN, 11));
        form.add(animateCheckBox);

        algoCombo.addActionListener(e -> {
            String sel = (String) algoCombo.getSelectedItem();
            SchedulingAlgorithmType type = SchedulingAlgorithmType.fromDisplayName(sel);
            algoDescArea.setText(type.getDescription());
            quantumStepper.setEnabled(type.usesQuantum() || type == SchedulingAlgorithmType.MLFQ);
            agingCheckBox.setEnabled(type.supportsAging());
            agingIntervalStepper.setEnabled(type.supportsAging());
        });
        algoCombo.setSelectedIndex(0);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        FlatButton runBtn = new FlatButton("Run Simulation", Theme.ACCENT, 1, true);
        FlatButton resetBtn = new FlatButton("Reset Results", Theme.PANEL2, 4, false);
        runBtn.addActionListener(e -> runSimulation());
        resetBtn.addActionListener(e -> resetResults());
        buttons.add(runBtn);
        buttons.add(resetBtn);

        card.add(form, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildProcessCard() {
        Card card = new Card("PROCESSES", "Add, edit, or import process workloads", null);

        inTable = new JTable(inputModel);
        styleTable(inTable);
        inTable.getColumnModel().getColumn(1).setCellEditor(new IntEditor(0));
        inTable.getColumnModel().getColumn(2).setCellEditor(new IntEditor(1));
        inTable.getColumnModel().getColumn(3).setCellEditor(new IntEditor(0));

        JScrollPane sc = new JScrollPane(inTable);
        sc.setBorder(new LineBorder(Theme.BORDER, 1, true));
        sc.getViewport().setBackground(Theme.PANEL2);
        sc.setPreferredSize(new Dimension(300, 220));
        card.add(sc, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 6, 4, 0));
        buttons.setOpaque(false);
        FlatButton add = new FlatButton("+ Add", Theme.ACCENT, 0, true);
        FlatButton rem = new FlatButton("Remove", Theme.PANEL2, 0, false);
        FlatButton rnd = new FlatButton("Random", Theme.PANEL2, 0, false);
        FlatButton demo = new FlatButton("Demo", Theme.PANEL2, 0, false);
        FlatButton imp = new FlatButton("Import", Theme.PANEL2, 0, false);
        FlatButton clr = new FlatButton("Clear", Theme.PANEL2, 0, false);

        for (FlatButton b : new FlatButton[]{add, rem, rnd, demo, imp, clr}) {
            b.setFont(Theme.font(Font.BOLD, 10));
            buttons.add(b);
        }

        add.addActionListener(e -> addProcess());
        rem.addActionListener(e -> removeProcess());
        rnd.addActionListener(e -> { randomProcesses(); runSimulation(); });
        demo.addActionListener(e -> { loadDemoWorkload(); runSimulation(); });
        imp.addActionListener(e -> importCsvWorkload());
        clr.addActionListener(e -> clearProcesses());

        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildGanttCard() {
        Card card = new Card("GANTT CHART", "Execution timeline with arrival markers and context switches", null);
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(liveView, BorderLayout.NORTH);

        JScrollPane sc = new JScrollPane(ganttPanel);
        sc.setBorder(new LineBorder(Theme.BORDER, 1, true));
        sc.getViewport().setBackground(new Color(17, 21, 37));
        sc.setPreferredSize(new Dimension(300, 190));
        center.add(sc, BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        // Controls Bar
        JPanel ctl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ctl.setOpaque(false);
        playBtn = new FlatButton("Play", Theme.ACCENT, 1, true);
        FlatButton stop = new FlatButton("Step", Theme.PANEL2, 3, false);
        FlatButton restart = new FlatButton("Restart", Theme.PANEL2, 4, false);
        FlatButton end = new FlatButton("Skip to end", Theme.PANEL2, 5, false);

        playBtn.addActionListener(e -> togglePlay());
        stop.addActionListener(e -> {
            if (currentResult == null) return;
            pausePlay();
            cursor = Math.min(currentResult.getTotalTime(), cursor + 1);
            syncView();
        });
        restart.addActionListener(e -> {
            if (currentResult == null) return;
            pausePlay();
            cursor = 0;
            syncView();
        });
        end.addActionListener(e -> {
            if (currentResult == null) return;
            pausePlay();
            cursor = currentResult.getTotalTime();
            syncView();
        });

        ctl.add(playBtn);
        ctl.add(stop);
        ctl.add(restart);
        ctl.add(end);
        ctl.add(Box.createHorizontalStrut(10));
        ctl.add(Theme.lbl("Speed", 11, Theme.MUTED, false));
        speedSlider = new JSlider(1, 10, 4);
        speedSlider.setOpaque(false);
        speedSlider.setPreferredSize(new Dimension(110, 26));
        speedSlider.addChangeListener(e -> playTimer.setDelay(Math.max(25, 660 / speedSlider.getValue())));
        ctl.add(speedSlider);

        card.add(ctl, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildResultsCard() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        FlatButton expCsv = new FlatButton("Export CSV", Theme.PANEL2, 0, false);
        expCsv.setFont(Theme.font(Font.BOLD, 10));
        expCsv.addActionListener(e -> exportCsv());
        FlatButton expHtml = new FlatButton("HTML Report", Theme.ACCENT, 0, false);
        expHtml.setFont(Theme.font(Font.BOLD, 10));
        expHtml.addActionListener(e -> exportHtmlReport());
        actions.add(expCsv);
        actions.add(expHtml);

        Card card = new Card("PROCESS RESULTS & STARVATION MONITOR",
                "CT = completion \u2022 TAT = CT \u2212 AT \u2022 WT = TAT \u2212 BT \u2022 RT = start \u2212 AT", actions);

        outTable = new JTable(outputModel);
        styleTable(outTable);

        JScrollPane sc = new JScrollPane(outTable);
        sc.setBorder(new LineBorder(Theme.BORDER, 1, true));
        sc.getViewport().setBackground(Theme.PANEL2);
        sc.setPreferredSize(new Dimension(300, 160));

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(sc);
        centerPanel.add(starvationPanel);

        card.add(centerPanel, BorderLayout.CENTER);
        return card;
    }

    // -------------------------------------------------------------
    // Core Actions
    // -------------------------------------------------------------
    public void runSimulation() {
        try {
            List<ProcessModel> workload = readProcessInputs();
            String algoName = (String) algoCombo.getSelectedItem();
            SchedulingAlgorithmType algoType = SchedulingAlgorithmType.fromDisplayName(algoName);

            int q = quantumStepper.getValue();
            int cs = csOverheadStepper.getValue();
            boolean aging = agingCheckBox.isSelected();
            int agingInterval = agingIntervalStepper.getValue();

            SimulationConfig config = new SimulationConfig(q, cs, aging, agingInterval, 1, 0, 15);
            SimulationResult result = SimulationEngine.simulate(workload, algoType, config);

            displaySimulationResult(result);

            // Run comparison across all 10 algorithms for comparison tab and HTML export
            runAllComparisons(workload, config);

            setStatus("Simulation complete: " + algoType.getDisplayName()
                    + (algoType.usesQuantum() ? " (Q=" + q + ")" : "")
                    + (cs > 0 ? " (CS=" + cs + ")" : ""), Theme.GREEN);
        } catch (IllegalArgumentException ex) {
            setStatus("Input Error: " + ex.getMessage(), Theme.ROSE);
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception ex) {
            setStatus("Error: " + ex.getMessage(), Theme.ROSE);
            ex.printStackTrace();
        }
    }

    private void displaySimulationResult(SimulationResult r) {
        pausePlay();
        this.currentResult = r;

        outputModel.setRowCount(0);
        for (ProcessModel p : r.getProcesses()) {
            outputModel.addRow(new Object[]{
                    p.getId(), p.getArrivalTime(), p.getBurstTime(), p.getConfiguredPriority(),
                    p.getFirstStartTime(), p.getCompletionTime(), p.getTurnaroundTime(), p.getWaitingTime(), p.getResponseTime()
            });
        }

        mWT.set(r.getAvgWaitingTime());
        mTAT.set(r.getAvgTurnaroundTime());
        mRT.set(r.getAvgResponseTime());
        mUtil.set(r.getCpuUtilization());
        mThr.set(r.getThroughput());
        mSw.set(r.getContextSwitchCount());

        ganttPanel.setResult(r);
        starvationPanel.updateState(r);
        decisionLogPanel.setResult(r);

        cursor = animateCheckBox.isSelected() ? 0 : r.getTotalTime();
        syncView();
        if (animateCheckBox.isSelected()) startPlay();
    }

    private void runAllComparisons(List<ProcessModel> workload, SimulationConfig config) {
        comparisonResults.clear();
        for (SchedulingAlgorithmType algo : SchedulingAlgorithmType.values()) {
            comparisonResults.add(SimulationEngine.simulate(workload, algo, config));
        }
        comparisonPanel.setResults(comparisonResults);
    }

    private List<ProcessModel> readProcessInputs() {
        if (inTable.isEditing() && !inTable.getCellEditor().stopCellEditing()) {
            throw new IllegalArgumentException("Please fix the highlighted input cell first.");
        }
        if (inputModel.getRowCount() == 0) {
            throw new IllegalArgumentException("Add at least one process first.");
        }

        List<ProcessModel> list = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        for (int r = 0; r < inputModel.getRowCount(); r++) {
            String id = String.valueOf(inputModel.getValueAt(r, 0)).trim();
            int at = ((Number) inputModel.getValueAt(r, 1)).intValue();
            int bt = ((Number) inputModel.getValueAt(r, 2)).intValue();
            int pr = ((Number) inputModel.getValueAt(r, 3)).intValue();

            if (id.isEmpty()) throw new IllegalArgumentException("Process ID in row " + (r + 1) + " cannot be empty.");
            if (!ids.add(id)) throw new IllegalArgumentException("Duplicate process ID '" + id + "' in row " + (r + 1) + ".");
            if (at < 0) throw new IllegalArgumentException("Arrival time for " + id + " must be non-negative.");
            if (bt <= 0) throw new IllegalArgumentException("Burst time for " + id + " must be positive.");
            if (pr < 0) throw new IllegalArgumentException("Priority for " + id + " must be non-negative.");

            list.add(new ProcessModel(id, at, bt, pr, r));
        }
        return list;
    }

    private void addProcess() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < inputModel.getRowCount(); i++) ids.add(String.valueOf(inputModel.getValueAt(i, 0)));
        int n = 1;
        while (ids.contains("P" + n)) n++;
        inputModel.addRow(new Object[]{"P" + n, 0, 1, 1});
        int last = inputModel.getRowCount() - 1;
        inTable.setRowSelectionInterval(last, last);
        inTable.scrollRectToVisible(inTable.getCellRect(last, 0, true));
        setStatus("Process P" + n + " added", Theme.CYAN);
    }

    private void removeProcess() {
        int r = inTable.getSelectedRow();
        if (r < 0) {
            setStatus("Select a row to remove", Theme.ROSE);
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        if (inTable.isEditing()) inTable.getCellEditor().cancelCellEditing();
        inputModel.removeRow(r);
        setStatus("Process removed", Theme.CYAN);
    }

    public void loadDemoWorkload() {
        if (inTable != null && inTable.isEditing()) inTable.getCellEditor().cancelCellEditing();
        inputModel.setRowCount(0);
        int[][] d = {{0, 5, 2}, {1, 3, 1}, {2, 8, 4}, {3, 6, 3}, {4, 2, 5}, {6, 4, 2}};
        for (int i = 0; i < d.length; i++) {
            inputModel.addRow(new Object[]{"P" + (i + 1), d[i][0], d[i][1], d[i][2]});
        }
    }

    private void randomProcesses() {
        if (inTable.isEditing()) inTable.getCellEditor().cancelCellEditing();
        Random rnd = new Random();
        int n = 4 + rnd.nextInt(4);
        inputModel.setRowCount(0);
        for (int i = 0; i < n; i++) {
            inputModel.addRow(new Object[]{"P" + (i + 1), rnd.nextInt(8), 1 + rnd.nextInt(9), 1 + rnd.nextInt(5)});
        }
    }

    private void clearProcesses() {
        if (inTable.isEditing()) inTable.getCellEditor().cancelCellEditing();
        inputModel.setRowCount(0);
        resetResults();
        setStatus("All processes cleared", Theme.CYAN);
    }

    private void resetResults() {
        pausePlay();
        currentResult = null;
        cursor = 0;
        outputModel.setRowCount(0);
        ganttPanel.setResult(null);
        liveView.setState(null, 0);
        starvationPanel.updateState(null);
        decisionLogPanel.setResult(null);
        mWT.set(0); mTAT.set(0); mRT.set(0); mUtil.set(0); mThr.set(0); mSw.set(0);
        setStatus("Results reset", Theme.CYAN);
    }

    // -------------------------------------------------------------
    // Animation Playback
    // -------------------------------------------------------------
    private void startPlay() {
        playTimer.setDelay(Math.max(25, 660 / speedSlider.getValue()));
        playTimer.start();
        playBtn.setText("Pause");
        playBtn.glyph = 2;
        playBtn.repaint();
    }

    private void pausePlay() {
        playTimer.stop();
        if (playBtn != null) {
            playBtn.setText("Play");
            playBtn.glyph = 1;
            playBtn.repaint();
        }
    }

    private void togglePlay() {
        if (currentResult == null) return;
        if (playTimer.isRunning()) {
            pausePlay();
        } else {
            if (cursor >= currentResult.getTotalTime()) cursor = 0;
            syncView();
            startPlay();
        }
    }

    private void onAnimationTick() {
        if (currentResult == null) {
            pausePlay();
            return;
        }
        if (cursor < currentResult.getTotalTime()) {
            cursor++;
        }
        syncView();
        if (cursor >= currentResult.getTotalTime()) {
            pausePlay();
        }
    }

    private void syncView() {
        ganttPanel.setCursor(cursor);
        liveView.setState(currentResult, cursor);
        if (playTimer.isRunning()) ganttPanel.ensureCursorVisible();
    }

    // -------------------------------------------------------------
    // Import & Export
    // -------------------------------------------------------------
    private void importCsvWorkload() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Workload CSV");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        try {
            List<ProcessModel> imported = CsvImporter.importFromFile(file);
            if (inTable.isEditing()) inTable.getCellEditor().cancelCellEditing();
            inputModel.setRowCount(0);
            for (ProcessModel p : imported) {
                inputModel.addRow(new Object[]{p.getId(), p.getArrivalTime(), p.getBurstTime(), p.getConfiguredPriority()});
            }
            setStatus("Successfully imported " + imported.size() + " processes from " + file.getName(), Theme.GREEN);
            runSimulation();
        } catch (Exception ex) {
            setStatus("CSV Import Failed: " + ex.getMessage(), Theme.ROSE);
            JOptionPane.showMessageDialog(this, "Failed to import CSV:\n" + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        if (currentResult == null) {
            setStatus("Nothing to export — run simulation first", Theme.ROSE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("simulation_results.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            CsvExporter.exportToFile(currentResult, fc.getSelectedFile());
            setStatus("CSV exported to " + fc.getSelectedFile().getName(), Theme.GREEN);
        } catch (Exception ex) {
            setStatus("CSV Export Failed: " + ex.getMessage(), Theme.ROSE);
        }
    }

    private void exportHtmlReport() {
        if (currentResult == null) {
            setStatus("Nothing to export — run simulation first", Theme.ROSE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("scheduling_report.html"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            HtmlReportExporter.exportToFile(currentResult, comparisonResults, fc.getSelectedFile());
            setStatus("HTML Report exported to " + fc.getSelectedFile().getName(), Theme.GREEN);
        } catch (Exception ex) {
            setStatus("HTML Export Failed: " + ex.getMessage(), Theme.ROSE);
        }
    }

    private static void styleTable(JTable t) {
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setBackground(Theme.PANEL2);
        t.setForeground(Theme.TEXT);
        t.setSelectionBackground(new Color(64, 58, 120));
        t.setSelectionForeground(Color.WHITE);
        t.setFont(Theme.font(Font.PLAIN, 12));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(100, 32));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, false, false, row, column);
                setHorizontalAlignment(CENTER);
                setBackground(new Color(34, 42, 68));
                setForeground(Theme.CYAN);
                setFont(Theme.font(Font.BOLD, 11));
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
                    setForeground(Theme.PAL[row % Theme.PAL.length]);
                    setFont(Theme.font(Font.BOLD, 12));
                } else {
                    setForeground(Theme.TEXT);
                    setFont(Theme.font(Font.PLAIN, 12));
                }
                return this;
            }
        });
    }

    // -------------------------------------------------------------
    // Helper Classes
    // -------------------------------------------------------------
    static class IntEditor extends DefaultCellEditor {
        final int min;
        final JTextField f;

        IntEditor(int min) {
            super(new JTextField());
            this.min = min;
            f = (JTextField) getComponent();
            f.setHorizontalAlignment(JTextField.CENTER);
            f.setBackground(new Color(40, 48, 78));
            f.setForeground(Theme.TEXT);
            f.setCaretColor(Theme.CYAN);
            f.setFont(Theme.font(Font.BOLD, 12));
            f.setBorder(new LineBorder(Theme.CYAN));
            setClickCountToStart(1);
        }

        public boolean stopCellEditing() {
            try {
                int v = Integer.parseInt(f.getText().trim());
                if (v < min || v > 9999) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                f.setBorder(new LineBorder(Theme.ROSE, 2));
                return false;
            }
            f.setBorder(new LineBorder(Theme.CYAN));
            return super.stopCellEditing();
        }

        public Object getCellEditorValue() {
            return Integer.parseInt(f.getText().trim());
        }
    }

    static class Logo extends JComponent {
        public Dimension getPreferredSize() { return new Dimension(46, 46); }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            g.setPaint(new GradientPaint(0, 0, Theme.ACCENT, 46, 46, Theme.CYAN));
            g.fill(new RoundRectangle2D.Double(0, 0, 45, 45, 14, 14));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(13, 13, 19, 19, 5, 5));
            for (int i = 0; i < 3; i++) {
                int o = 17 + i * 6;
                g.drawLine(o, 8, o, 13);
                g.drawLine(o, 32, o, 37);
                g.drawLine(8, o, 13, o);
                g.drawLine(32, o, 37, o);
            }
            g.fill(new RoundRectangle2D.Double(19, 19, 7, 7, 2, 2));
            g.dispose();
        }
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            UIManager.put("ToolTip.background", new Color(36, 44, 72));
            UIManager.put("ToolTip.foreground", Theme.TEXT);
            UIManager.put("ToolTip.border", new LineBorder(Theme.BORDER));
            UIManager.put("ToolTip.font", Theme.font(Font.PLAIN, 12));
            new MainFrame().setVisible(true);
        });
    }
}
