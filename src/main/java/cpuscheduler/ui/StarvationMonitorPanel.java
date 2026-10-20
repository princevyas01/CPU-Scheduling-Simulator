package cpuscheduler.ui;

import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Starvation Monitoring Panel:
 * Tracks waiting time, configured vs. effective priority, aging events,
 * and highlights processes exceeding the configured starvation threshold.
 */
public class StarvationMonitorPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel summaryLabel;

    public StarvationMonitorPanel() {
        super(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(10, 12, 10, 12));

        summaryLabel = Theme.lbl("Aging: Inactive \u2022 Waiting Threshold: 15 units", 11, Theme.MUTED, false);
        add(summaryLabel, BorderLayout.NORTH);

        String[] cols = {"PID", "Waiting Time", "Configured Priority", "Effective Priority", "Aging Events", "Warning State"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        styleTable(table);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(Theme.BORDER, 1, true));
        sp.getViewport().setBackground(Theme.PANEL2);
        sp.setPreferredSize(new Dimension(300, 150));
        add(sp, BorderLayout.CENTER);
    }

    public void updateState(SimulationResult result) {
        tableModel.setRowCount(0);
        if (result == null) {
            summaryLabel.setText("No active simulation results.");
            return;
        }

        int threshold = result.getConfig().getStarvationThreshold();
        boolean aging = result.getConfig().isAgingEnabled();
        summaryLabel.setText("Priority Aging: " + (aging ? "ENABLED (Interval=" + result.getConfig().getAgingInterval() + ")" : "DISABLED")
                + "  \u2022  Wait Threshold: " + threshold + " units");

        List<ProcessModel> procs = result.getProcesses();
        int warningCount = 0;

        for (ProcessModel p : procs) {
            boolean warning = p.getWaitingTime() >= threshold;
            if (warning) warningCount++;
            String state = warning ? "LONG WAIT DETECTED" : "NORMAL";

            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getWaitingTime() + " units",
                    p.getConfiguredPriority(),
                    p.getEffectivePriority() + (p.getEffectivePriority() != p.getConfiguredPriority() ? " (boosted)" : ""),
                    p.getAgingEventsCount(),
                    state
            });
        }

        if (warningCount > 0) {
            summaryLabel.setText(summaryLabel.getText() + "  \u2022  \u26A0 " + warningCount + " process(es) exceeded wait threshold!");
            summaryLabel.setForeground(Theme.ROSE);
        } else {
            summaryLabel.setForeground(Theme.GREEN);
        }
    }

    private static void styleTable(JTable t) {
        t.setRowHeight(26);
        t.setShowGrid(false);
        t.setBackground(Theme.PANEL2);
        t.setForeground(Theme.TEXT);
        t.setFont(Theme.font(Font.PLAIN, 11));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(100, 28));
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
                setBackground(isSelected ? new Color(64, 58, 120) : (row % 2 == 0 ? Theme.PANEL2 : Theme.mix(Theme.PANEL2, Theme.BG, .22)));
                setHorizontalAlignment(CENTER);
                if (column == 5) {
                    if ("LONG WAIT DETECTED".equals(value)) {
                        setForeground(Theme.ROSE);
                        setFont(Theme.font(Font.BOLD, 11));
                    } else {
                        setForeground(Theme.GREEN);
                        setFont(Theme.font(Font.PLAIN, 11));
                    }
                } else if (column == 0) {
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
