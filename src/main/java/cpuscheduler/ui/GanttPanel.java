package cpuscheduler.ui;

import cpuscheduler.model.GanttSegment;
import cpuscheduler.model.ProcessModel;
import cpuscheduler.model.SimulationResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Animated Gantt chart component supporting process execution, CPU idle time,
 * context-switch overhead blocks, arrival markers, and playhead cursor.
 */
public class GanttPanel extends JComponent implements Scrollable {
    public static final int PADL = 22, PADR = 22, MINU = 28;
    private SimulationResult res;
    private int cursor;
    private final List<Rectangle2D> rects = new ArrayList<>();
    private final List<GanttSegment> rectSeg = new ArrayList<>();

    public GanttPanel() {
        setToolTipText("");
    }

    public void setResult(SimulationResult r) {
        this.res = r;
        revalidate();
        repaint();
        scrollRectToVisible(new Rectangle(0, 0, 1, 1));
    }

    public void setCursor(int c) {
        this.cursor = c;
        repaint();
    }

    public void ensureCursorVisible() {
        if (res == null || res.getTotalTime() == 0) return;
        double u = (getWidth() - PADL - PADR) / (double) res.getTotalTime();
        int x = (int) (PADL + cursor * u);
        scrollRectToVisible(new Rectangle(x - 80, 0, 220, getHeight()));
    }

    public String getToolTipText(MouseEvent e) {
        for (int i = 0; i < rects.size(); i++) {
            if (rects.get(i).contains(e.getPoint())) {
                GanttSegment s = rectSeg.get(i);
                String n;
                if (s.isIdle()) n = "CPU Idle";
                else if (s.isContextSwitch()) n = "Context Switch Overhead";
                else n = s.getProcessId();
                return n + ":  " + s.getStartTime() + " \u2192 " + s.getEndTime() + "   (" + s.getDuration() + " units)";
            }
        }
        return null;
    }

    public Dimension getPreferredSize() {
        int n = res == null ? 1 : Math.max(1, res.getTotalTime());
        return new Dimension(PADL + PADR + n * MINU, 170);
    }

    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 28; }
    public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 160; }
    public boolean getScrollableTracksViewportHeight() { return true; }
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport
            && getParent().getWidth() >= getPreferredSize().width;
    }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        int W = getWidth(), H = getHeight();
        rects.clear();
        rectSeg.clear();

        if (res == null) {
            g.setColor(Theme.MUTED);
            g.setFont(Theme.font(Font.PLAIN, 13));
            String m = "Run a simulation to view the Gantt chart";
            g.drawString(m, (W - g.getFontMetrics().stringWidth(m)) / 2, H / 2);
            g.dispose();
            return;
        }

        int total = Math.max(1, res.getTotalTime());
        double u = (W - PADL - PADR) / (double) total;
        int top = 50, bh = 62, axisY = top + bh + 12;

        // 1. Arrival markers (colored triangles)
        Map<Integer, Integer> stack = new HashMap<>();
        for (ProcessModel p : res.getProcesses()) {
            int k = stack.merge(p.getArrivalTime(), 1, Integer::sum) - 1;
            double x = PADL + p.getArrivalTime() * u;
            int y = Math.max(8, top - 14 - k * 9);
            g.setColor(Theme.PAL[Math.abs(p.getOrder()) % Theme.PAL.length]);
            g.fill(Theme.poly((int) x - 4, y - 6, (int) x + 4, y - 6, (int) x, y + 2));
        }
        g.setFont(Theme.font(Font.PLAIN, 9));
        g.setColor(Theme.MUTED);
        g.drawString("arrivals", 4, 10);

        // 2. Timeline blocks
        for (GanttSegment s : res.getSegments()) {
            double x = PADL + s.getStartTime() * u;
            double w = s.getDuration() * u;
            Color c;
            if (s.isIdle()) {
                c = new Color(64, 70, 94);
            } else if (s.isContextSwitch()) {
                c = Theme.PURPLE;
            } else {
                c = Theme.PAL[Math.abs(s.getProcessIndex()) % Theme.PAL.length];
            }

            RoundRectangle2D box = new RoundRectangle2D.Double(x + 1, top, w - 2, bh, 12, 12);
            rects.add(box.getBounds2D());
            rectSeg.add(s);

            double vis = cursor >= s.getEndTime() ? 1.0 : cursor <= s.getStartTime() ? 0.0 : (cursor - s.getStartTime()) / (double) s.getDuration();

            // Background ghost outline if not yet reached by playhead
            if (vis < 1.0) {
                g.setColor(Theme.alpha(c, 38));
                g.fill(box);
                g.setColor(Theme.alpha(c, 110));
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
                g.draw(box);
                g.setStroke(new BasicStroke(1f));
            }

            // Visible active fill
            if (vis > 0.0) {
                Shape oldClip = g.getClip();
                g.clip(new Rectangle2D.Double(x, 0, w * vis + 1, H));
                g.setPaint(new GradientPaint(0, top, Theme.mix(c, Color.WHITE, .18), 0, top + bh, Theme.mix(c, Theme.BG, .22)));
                g.fill(box);

                // Idle hatching
                if (s.isIdle()) {
                    g.clip(box);
                    g.setColor(Theme.alpha(Color.WHITE, 22));
                    for (double sx = x - bh; sx < x + w; sx += 10) {
                        g.draw(new Line2D.Double(sx, top + bh, sx + bh, top));
                    }
                } else if (s.isContextSwitch()) {
                    g.clip(box);
                    g.setColor(Theme.alpha(Color.BLACK, 40));
                    for (double sx = x - bh; sx < x + w; sx += 8) {
                        g.draw(new Line2D.Double(sx, top, sx + bh, top + bh));
                    }
                }

                g.setClip(oldClip);
                oldClip = g.getClip();
                g.clip(new Rectangle2D.Double(x, 0, w * vis + 1, H));

                // Block Label
                if (w >= 26) {
                    String name = s.isIdle() ? "idle" : s.isContextSwitch() ? "CS" : s.getProcessId();
                    g.setColor(Color.WHITE);
                    g.setFont(Theme.font(Font.BOLD, s.isIdle() || s.isContextSwitch() ? 11 : 14));
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(name, (float) (x + (w - fm.stringWidth(name)) / 2), top + bh / 2f);
                    g.setFont(Theme.font(Font.PLAIN, 10));
                    g.setColor(Theme.alpha(Color.WHITE, 190));
                    String d = s.getDuration() + "u";
                    fm = g.getFontMetrics();
                    g.drawString(d, (float) (x + (w - fm.stringWidth(d)) / 2), top + bh / 2f + 17);
                }
                g.setClip(oldClip);
            }
        }

        // 3. Time axis & ticks
        g.setColor(Theme.BORDER);
        g.drawLine(PADL, axisY, W - PADR, axisY);
        g.setFont(Theme.font(Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        double lastX = -100;
        List<Integer> times = new ArrayList<>();
        for (GanttSegment s : res.getSegments()) times.add(s.getStartTime());
        times.add(res.getTotalTime());
        for (int tt : times) {
            double x = PADL + tt * u;
            g.setColor(Theme.BORDER);
            g.drawLine((int) x, axisY, (int) x, axisY + 5);
            if (x - lastX >= 26 || tt == res.getTotalTime()) {
                g.setColor(Theme.MUTED);
                String s = "" + tt;
                g.drawString(s, (float) (x - fm.stringWidth(s) / 2.0), axisY + 20);
                lastX = x;
            }
        }

        // 4. Playhead cursor
        if (cursor >= 0 && cursor <= res.getTotalTime()) {
            double x = PADL + cursor * u;
            g.setColor(Theme.alpha(Theme.CYAN, 40));
            g.setStroke(new BasicStroke(8f));
            g.draw(new Line2D.Double(x, top - 6, x, axisY));
            g.setColor(Theme.CYAN);
            g.setStroke(new BasicStroke(2f));
            g.draw(new Line2D.Double(x, top - 6, x, axisY + 4));
            g.fill(Theme.poly((int) x - 5, top - 14, (int) x + 5, top - 14, (int) x, top - 5));
        }

        g.dispose();
    }
}
