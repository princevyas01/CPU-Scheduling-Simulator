package cpuscheduler.ui;

import cpuscheduler.model.GanttSegment;
import cpuscheduler.model.SimulationResult;
import cpuscheduler.scheduler.SchedulingAlgorithmType;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Stacked mini Gantt chart strips displaying timelines of all algorithms on a shared time scale.
 */
public class MiniGantts extends JComponent {
    private List<SimulationResult> rs = new ArrayList<>();

    public void set(List<SimulationResult> r) {
        this.rs = r != null ? r : new ArrayList<>();
        revalidate();
        repaint();
    }

    public Dimension getPreferredSize() { return new Dimension(600, rs.size() * 32 + 6); }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        int W = getWidth(), labelW = 110, rowH = 32;
        int maxT = 1;
        for (SimulationResult r : rs) maxT = Math.max(maxT, r.getTotalTime());
        double u = (W - labelW - 24) / (double) maxT;

        for (int i = 0; i < rs.size(); i++) {
            SimulationResult r = rs.get(i);
            int y = i * rowH + 4;
            g.setFont(Theme.font(Font.BOLD, 11));
            g.setColor(Theme.TEXT);
            String shortName = SchedulingAlgorithmType.fromDisplayName(r.getAlgorithmName()).getShortName();
            g.drawString(shortName, 0, y + 18);

            for (GanttSegment s : r.getSegments()) {
                double x = labelW + s.getStartTime() * u;
                double w = s.getDuration() * u;
                Color c = s.isIdle() ? new Color(64, 70, 94) : s.isContextSwitch() ? Theme.PURPLE : Theme.PAL[Math.abs(s.getProcessIndex()) % Theme.PAL.length];
                g.setColor(c);
                g.fill(new RoundRectangle2D.Double(x + 0.5, y + 2, Math.max(1, w - 1), 22, 6, 6));
                if (w >= 22 && !s.isIdle() && !s.isContextSwitch()) {
                    g.setFont(Theme.font(Font.BOLD, 10));
                    g.setColor(Color.WHITE);
                    String n = s.getProcessId();
                    g.drawString(n, (float) (x + (w - g.getFontMetrics().stringWidth(n)) / 2), y + 17);
                }
            }
            g.setColor(Theme.MUTED);
            g.setFont(Theme.font(Font.PLAIN, 10));
            g.drawString("" + r.getTotalTime(), (float) (labelW + r.getTotalTime() * u + 5), y + 17);
        }
        g.dispose();
    }
}
