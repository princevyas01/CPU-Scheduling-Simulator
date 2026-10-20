package cpuscheduler.ui;

import cpuscheduler.model.SimulationResult;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal grouped bar chart comparing performance metrics across algorithms.
 */
public class BarChart extends JComponent {
    private List<SimulationResult> rs = new ArrayList<>();

    public void set(List<SimulationResult> r) {
        this.rs = r != null ? r : new ArrayList<>();
        revalidate();
        repaint();
    }

    public Dimension getPreferredSize() { return new Dimension(420, 70 + rs.size() * 36); }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        int W = getWidth(), H = getHeight();
        if (rs.isEmpty()) { g.dispose(); return; }

        Color[] cols = {Theme.AMBER, Theme.CYAN, Theme.GREEN};
        String[] names = {"Waiting", "Turnaround", "Response"};
        int lx = 0;
        g.setFont(Theme.font(Font.PLAIN, 11));
        for (int i = 0; i < 3; i++) {
            g.setColor(cols[i]);
            g.fill(new RoundRectangle2D.Double(lx, 4, 10, 10, 4, 4));
            g.setColor(Theme.MUTED);
            g.drawString(names[i], lx + 15, 13);
            lx += 28 + g.getFontMetrics().stringWidth(names[i]) + 12;
        }

        double[][] v = new double[rs.size()][3];
        double max = 0.0001;
        double[] mn = {1e18, 1e18, 1e18};
        for (int i = 0; i < rs.size(); i++) {
            v[i][0] = rs.get(i).getAvgWaitingTime();
            v[i][1] = rs.get(i).getAvgTurnaroundTime();
            v[i][2] = rs.get(i).getAvgResponseTime();
            for (int k = 0; k < 3; k++) {
                max = Math.max(max, v[i][k]);
                mn[k] = Math.min(mn[k], v[i][k]);
            }
        }

        int labelW = 92, x0 = labelW + 10, x1 = W - 44, top = 30;
        double gh = (H - top - 6) / (double) rs.size();
        int bh = (int) Math.max(4, Math.min(10, (gh - 8) / 3));

        for (int i = 0; i < rs.size(); i++) {
            double gy = top + i * gh;
            g.setFont(Theme.font(Font.BOLD, 11));
            g.setColor(Theme.TEXT);
            String nm = cpuscheduler.scheduler.SchedulingAlgorithmType.fromDisplayName(rs.get(i).getAlgorithmName()).getShortName();
            g.drawString(nm, labelW - g.getFontMetrics().stringWidth(nm), (float) (gy + gh / 2 + 4));

            double by = gy + (gh - (bh * 3 + 4)) / 2;
            for (int k = 0; k < 3; k++) {
                double w = Math.max(2, v[i][k] / max * (x1 - x0));
                boolean best = Math.abs(v[i][k] - mn[k]) < 1e-9;
                g.setColor(Theme.alpha(Theme.PANEL2, 255));
                g.fill(new RoundRectangle2D.Double(x0, by + k * (bh + 2), x1 - x0, bh, bh, bh));
                g.setPaint(new GradientPaint((float) x0, 0, Theme.alpha(cols[k], 150), (float) (x0 + w), 0, cols[k]));
                g.fill(new RoundRectangle2D.Double(x0, by + k * (bh + 2), w, bh, bh, bh));
                if (best) {
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke(1.2f));
                    g.draw(new RoundRectangle2D.Double(x0, by + k * (bh + 2), w, bh, bh, bh));
                    g.setStroke(new BasicStroke(1f));
                }
                g.setFont(Theme.font(Font.PLAIN, 9));
                g.setColor(best ? Theme.TEXT : Theme.MUTED);
                g.drawString(Theme.f2(v[i][k]), (float) (x0 + w + 5), (float) (by + k * (bh + 2) + bh - 0.5));
            }
        }
        g.dispose();
    }
}
