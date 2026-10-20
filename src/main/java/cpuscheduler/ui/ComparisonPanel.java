package cpuscheduler.ui;

import cpuscheduler.model.SimulationResult;
import cpuscheduler.scheduler.SchedulingAlgorithmType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Advanced algorithm comparison dashboard with fairness analysis, starvation indicators,
 * leaderboard ranking, and drill-down into detailed simulator execution.
 */
public class ComparisonPanel extends JScrollPane {
    private final DefaultTableModel cmpModel;
    private final JTable cmpTable;
    private final BarChart chart = new BarChart();
    private final MiniGantts mini = new MiniGantts();

    private final MetricCard wWT, wTAT, wRT, wSw, wFair;
    private final JComboBox<String> rankByCombo;

    private List<SimulationResult> results = new ArrayList<>();
    private Consumer<String> drillDownListener;

    public ComparisonPanel() {
        setBorder(null);
        setOpaque(false);
        getViewport().setOpaque(false);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(4, 26, 14, 26));

        // Metric Cards
        JPanel win = new JPanel(new GridLayout(1, 5, 12, 0));
        win.setOpaque(false);
        wWT = new MetricCard("BEST WAITING TIME", Theme.AMBER, 2, "", "Lowest avg WT");
        wTAT = new MetricCard("BEST TURNAROUND", Theme.CYAN, 2, "", "Lowest avg TAT");
        wRT = new MetricCard("BEST RESPONSE TIME", Theme.GREEN, 2, "", "Lowest avg RT");
        wSw = new MetricCard("FEWEST SWITCHES", Theme.BLUE, 0, "", "Min process switches");
        wFair = new MetricCard("HIGHEST FAIRNESS", Theme.PURPLE, 3, "", "Jain's index (near 1.0)");
        for (MetricCard m : new MetricCard[]{wWT, wTAT, wRT, wSw, wFair}) {
            m.setText("—", "Run a simulation first");
            win.add(m);
        }
        win.setAlignmentX(Component.LEFT_ALIGNMENT);
        win.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        body.add(win);
        body.add(Box.createVerticalStrut(14));

        // Middle: Chart + Leaderboard
        JPanel mid = new JPanel(new GridLayout(1, 2, 14, 0));
        mid.setOpaque(false);
        mid.setAlignmentX(Component.LEFT_ALIGNMENT);

        Card chartCard = new Card("METRIC COMPARISON",
                "Average waiting, turnaround and response time — lower is better, white outline = winner", null);
        chartCard.add(chart, BorderLayout.CENTER);

        JPanel rankPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rankPanel.setOpaque(false);
        rankPanel.add(Theme.lbl("Rank by:", 11, Theme.MUTED, false));
        rankByCombo = new JComboBox<>(new String[]{
                "Default Order",
                "Lowest Avg Waiting Time",
                "Lowest Avg Turnaround Time",
                "Lowest Avg Response Time",
                "Highest Fairness",
                "Fewest Context Switches"
        });
        rankByCombo.setPreferredSize(new Dimension(170, 26));
        rankByCombo.setBackground(Theme.PANEL2);
        rankByCombo.setForeground(Theme.TEXT);
        rankByCombo.setFont(Theme.font(Font.PLAIN, 11));
        rankByCombo.addActionListener(e -> updateTableData());
        rankPanel.add(rankByCombo);

        Card tableCard = new Card("LEADERBOARD", "Double-click a row to open that algorithm in the simulator", rankPanel);
        String[] cols = {"Algorithm", "Avg WT", "Avg TAT", "Avg RT", "CPU %", "Switches", "Total", "Jain's Fair", "Max WT"};
        cmpModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        cmpTable = new JTable(cmpModel);
        styleTable(cmpTable);
        cmpTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && cmpTable.getSelectedRow() >= 0) {
                    String algoShort = (String) cmpTable.getValueAt(cmpTable.getSelectedRow(), 0);
                    if (drillDownListener != null) {
                        drillDownListener.accept(algoShort);
                    }
                }
            }
        });
        JScrollPane tsc = new JScrollPane(cmpTable);
        tsc.setBorder(new LineBorder(Theme.BORDER, 1, true));
        tsc.getViewport().setBackground(Theme.PANEL2);
        tableCard.add(tsc, BorderLayout.CENTER);

        mid.add(chartCard);
        mid.add(tableCard);
        mid.setPreferredSize(new Dimension(600, 420));
        mid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        body.add(mid);
        body.add(Box.createVerticalStrut(14));

        // Timelines side by side
        Card miniCard = new Card("TIMELINES SIDE BY SIDE",
                "Every algorithm on the same time scale — shorter strip = finishes sooner", null);
        miniCard.add(mini, BorderLayout.CENTER);
        miniCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(miniCard);

        setViewportView(body);
    }

    public void setDrillDownListener(Consumer<String> listener) {
        this.drillDownListener = listener;
    }

    public void setResults(List<SimulationResult> rs) {
        this.results = rs != null ? new ArrayList<>(rs) : new ArrayList<>();
        chart.set(results);
        mini.set(results);

        if (!results.isEmpty()) {
            updateWinners(results);
        }
        updateTableData();
    }

    private void updateWinners(List<SimulationResult> rs) {
        winner(wWT, rs, 0);
        winner(wTAT, rs, 1);
        winner(wRT, rs, 2);
        winner(wSw, rs, 3);
        winnerFairness(wFair, rs);
    }

    private void winner(MetricCard card, List<SimulationResult> rs, int metric) {
        double best = Double.MAX_VALUE;
        for (SimulationResult r : rs) {
            double v = metric == 0 ? r.getAvgWaitingTime() : metric == 1 ? r.getAvgTurnaroundTime() : metric == 2 ? r.getAvgResponseTime() : r.getContextSwitchCount();
            best = Math.min(best, v);
        }
        List<String> names = new ArrayList<>();
        for (SimulationResult r : rs) {
            double v = metric == 0 ? r.getAvgWaitingTime() : metric == 1 ? r.getAvgTurnaroundTime() : metric == 2 ? r.getAvgResponseTime() : r.getContextSwitchCount();
            if (Math.abs(v - best) < 1e-4) {
                names.add(SchedulingAlgorithmType.fromDisplayName(r.getAlgorithmName()).getShortName());
            }
        }
        String title = names.size() == 1 ? names.get(0)
                : names.size() == 2 ? names.get(0) + " / " + names.get(1)
                : names.get(0) + " +" + (names.size() - 1) + " tied";
        card.setText(title, (metric == 3 ? String.valueOf((int) best) : Theme.f2(best))
                + (metric == 3 ? " switches" : " time units"));
    }

    private void winnerFairness(MetricCard card, List<SimulationResult> rs) {
        double best = -1.0;
        for (SimulationResult r : rs) {
            best = Math.max(best, r.getJainsFairnessIndex());
        }
        List<String> names = new ArrayList<>();
        for (SimulationResult r : rs) {
            if (Math.abs(r.getJainsFairnessIndex() - best) < 1e-4) {
                names.add(SchedulingAlgorithmType.fromDisplayName(r.getAlgorithmName()).getShortName());
            }
        }
        String title = names.size() == 1 ? names.get(0)
                : names.size() == 2 ? names.get(0) + " / " + names.get(1)
                : names.get(0) + " +" + (names.size() - 1) + " tied";
        card.setText(title, String.format(Locale.US, "%.3f", best));
    }

    private void updateTableData() {
        cmpModel.setRowCount(0);
        if (results.isEmpty()) return;

        List<SimulationResult> sorted = new ArrayList<>(results);
        int rankOption = rankByCombo.getSelectedIndex();
        if (rankOption == 1) { // Lowest WT
            sorted.sort(Comparator.comparingDouble(SimulationResult::getAvgWaitingTime));
        } else if (rankOption == 2) { // Lowest TAT
            sorted.sort(Comparator.comparingDouble(SimulationResult::getAvgTurnaroundTime));
        } else if (rankOption == 3) { // Lowest RT
            sorted.sort(Comparator.comparingDouble(SimulationResult::getAvgResponseTime));
        } else if (rankOption == 4) { // Highest Fairness
            sorted.sort((a, b) -> Double.compare(b.getJainsFairnessIndex(), a.getJainsFairnessIndex()));
        } else if (rankOption == 5) { // Fewest Switches
            sorted.sort(Comparator.comparingInt(SimulationResult::getContextSwitchCount));
        }

        for (SimulationResult r : sorted) {
            String shortName = SchedulingAlgorithmType.fromDisplayName(r.getAlgorithmName()).getShortName();
            cmpModel.addRow(new Object[]{
                    shortName,
                    Theme.f2(r.getAvgWaitingTime()),
                    Theme.f2(r.getAvgTurnaroundTime()),
                    Theme.f2(r.getAvgResponseTime()),
                    String.format(Locale.US, "%.1f%%", r.getCpuUtilization()),
                    String.valueOf(r.getContextSwitchCount()),
                    String.valueOf(r.getTotalTime()),
                    String.format(Locale.US, "%.3f", r.getJainsFairnessIndex()),
                    String.valueOf(r.getMaxWaitingTime())
            });
        }
    }

    private static void styleTable(JTable t) {
        t.setRowHeight(32);
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
                setHorizontalAlignment(column == 0 ? LEFT : CENTER);
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
                setBackground(isSelected ? new Color(64, 58, 120) : (row % 2 == 0 ? Theme.PANEL2 : Theme.mix(Theme.PANEL2, Theme.BG, .22)));
                if (column == 0) {
                    setHorizontalAlignment(LEFT);
                    setForeground(Theme.TEXT);
                    setFont(Theme.font(Font.BOLD, 12));
                } else {
                    setHorizontalAlignment(CENTER);
                    setForeground(Theme.TEXT);
                    setFont(Theme.font(Font.PLAIN, 12));
                }
                return this;
            }
        });
    }
}
