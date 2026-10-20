package cpuscheduler.ui;

import cpuscheduler.model.SchedulingEvent;
import cpuscheduler.model.SimulationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interactive Decision Log tab allowing users to inspect, filter, and search
 * all scheduling decisions and synchronize with the Gantt chart timeline.
 */
public class DecisionLogPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<String> processFilter;
    private final JComboBox<String> eventTypeFilter;
    private final JTextField searchField;
    private final JLabel countLabel;

    private List<SchedulingEvent> allEvents = new ArrayList<>();
    private final List<SchedulingEvent> displayedEvents = new ArrayList<>();
    private Consumer<Integer> timeNavigationListener;

    public DecisionLogPanel() {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(14, 26, 14, 26));

        // Top Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterBar.setOpaque(false);

        filterBar.add(Theme.lbl("Filter Process:", 12, Theme.TEXT, true));
        processFilter = new JComboBox<>(new String[]{"All Processes"});
        styleCombo(processFilter);
        processFilter.addActionListener(e -> applyFilter());
        filterBar.add(processFilter);

        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(Theme.lbl("Event Type:", 12, Theme.TEXT, true));
        eventTypeFilter = new JComboBox<>(new String[]{
                "All Events",
                "DISPATCHED",
                "PREEMPTED",
                "QUANTUM_EXPIRED",
                "PRIORITY_AGED",
                "ARRIVED",
                "COMPLETED",
                "CPU_IDLE"
        });
        styleCombo(eventTypeFilter);
        eventTypeFilter.addActionListener(e -> applyFilter());
        filterBar.add(eventTypeFilter);

        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(Theme.lbl("Search:", 12, Theme.TEXT, true));
        searchField = new JTextField(12);
        searchField.setBackground(Theme.PANEL2);
        searchField.setForeground(Theme.TEXT);
        searchField.setCaretColor(Theme.CYAN);
        searchField.setFont(Theme.font(Font.PLAIN, 12));
        searchField.setBorder(new LineBorder(Theme.BORDER, 1, true));
        searchField.addActionListener(e -> applyFilter());
        filterBar.add(searchField);

        FlatButton applyBtn = new FlatButton("Filter", Theme.ACCENT, 0, true);
        applyBtn.setFont(Theme.font(Font.BOLD, 11));
        applyBtn.addActionListener(e -> applyFilter());
        filterBar.add(applyBtn);

        FlatButton resetBtn = new FlatButton("Reset", Theme.PANEL2, 0, false);
        resetBtn.setFont(Theme.font(Font.BOLD, 11));
        resetBtn.addActionListener(e -> {
            processFilter.setSelectedIndex(0);
            eventTypeFilter.setSelectedIndex(0);
            searchField.setText("");
            applyFilter();
        });
        filterBar.add(resetBtn);

        countLabel = Theme.lbl("Showing 0 events", 11, Theme.MUTED, false);
        filterBar.add(Box.createHorizontalStrut(15));
        filterBar.add(countLabel);

        add(filterBar, BorderLayout.NORTH);

        // Events Table
        String[] columns = {"Time", "Event", "Process", "Running", "Ready Processes", "Decision Reason", "Relevant Values", "Outcome"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(65);
        table.getColumnModel().getColumn(3).setPreferredWidth(65);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(320);
        table.getColumnModel().getColumn(6).setPreferredWidth(150);
        table.getColumnModel().getColumn(7).setPreferredWidth(110);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < displayedEvents.size()) {
                    SchedulingEvent ev = displayedEvents.get(row);
                    if (timeNavigationListener != null) {
                        timeNavigationListener.accept(ev.getTime());
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(Theme.BORDER, 1, true));
        scrollPane.getViewport().setBackground(Theme.PANEL2);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setTimeNavigationListener(Consumer<Integer> listener) {
        this.timeNavigationListener = listener;
    }

    public void setResult(SimulationResult result) {
        if (result == null) {
            allEvents = new ArrayList<>();
            processFilter.setModel(new DefaultComboBoxModel<>(new String[]{"All Processes"}));
            applyFilter();
            return;
        }

        this.allEvents = result.getEvents();

        // Populate process filter
        DefaultComboBoxModel<String> pModel = new DefaultComboBoxModel<>();
        pModel.addElement("All Processes");
        for (cpuscheduler.model.ProcessModel p : result.getProcesses()) {
            pModel.addElement(p.getId());
        }
        processFilter.setModel(pModel);

        applyFilter();
    }

    private void applyFilter() {
        displayedEvents.clear();
        tableModel.setRowCount(0);

        String selProc = (String) processFilter.getSelectedItem();
        String selType = (String) eventTypeFilter.getSelectedItem();
        String search = searchField.getText().trim().toLowerCase();

        for (SchedulingEvent ev : allEvents) {
            // Process filter
            if (selProc != null && !selProc.equals("All Processes")) {
                if (!selProc.equals(ev.getProcessId()) && !selProc.equals(ev.getRunningProcessId())) {
                    continue;
                }
            }

            // Event type filter
            if (selType != null && !selType.equals("All Events")) {
                if (!ev.getEventType().name().contains(selType)) {
                    continue;
                }
            }

            // Search text
            if (!search.isEmpty()) {
                String full = (ev.getDecisionReason() + " " + ev.getRelevantValues() + " " + ev.getProcessId()).toLowerCase();
                if (!full.contains(search)) {
                    continue;
                }
            }

            displayedEvents.add(ev);
            tableModel.addRow(new Object[]{
                    "t = " + ev.getTime(),
                    ev.getEventType().name(),
                    ev.getProcessId(),
                    ev.getRunningProcessId(),
                    ev.getReadyProcesses(),
                    ev.getDecisionReason(),
                    ev.getRelevantValues(),
                    ev.getOutcome()
            });
        }

        countLabel.setText("Showing " + displayedEvents.size() + " of " + allEvents.size() + " events (click any row to jump on timeline)");
    }

    private static void styleCombo(JComboBox<String> combo) {
        combo.setPreferredSize(new Dimension(140, 32));
        combo.setBackground(Theme.PANEL2);
        combo.setForeground(Theme.TEXT);
        combo.setFont(Theme.font(Font.PLAIN, 12));
    }

    private static void styleTable(JTable t) {
        t.setRowHeight(28);
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
                setBackground(isSelected ? new Color(64, 58, 120) : (row % 2 == 0 ? Theme.PANEL2 : Theme.mix(Theme.PANEL2, Theme.BG, .22)));
                if (column == 0) {
                    setHorizontalAlignment(CENTER);
                    setForeground(Theme.CYAN);
                    setFont(Theme.font(Font.BOLD, 11));
                } else if (column == 1) {
                    setHorizontalAlignment(CENTER);
                    setForeground(Theme.AMBER);
                    setFont(Theme.font(Font.BOLD, 11));
                } else if (column == 2 || column == 3) {
                    setHorizontalAlignment(CENTER);
                    setForeground(Theme.TEXT);
                    setFont(Theme.font(Font.BOLD, 11));
                } else if (column == 5) {
                    setHorizontalAlignment(LEFT);
                    setForeground(Theme.TEXT);
                    setFont(Theme.font(Font.PLAIN, 11));
                } else {
                    setHorizontalAlignment(CENTER);
                    setForeground(Theme.MUTED);
                    setFont(Theme.font(Font.PLAIN, 11));
                }
                return this;
            }
        });
    }
}
