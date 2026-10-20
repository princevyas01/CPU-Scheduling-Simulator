package cpuscheduler.ui;

import cpuscheduler.model.SimulationResult;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Live CPU and ready-queue view component shown above the Gantt chart.
 */
public class LiveView extends JComponent {
    private SimulationResult res;
    private int cursor;

    public void setState(SimulationResult r, int c) {
        this.res = r;
        this.cursor = c;
        repaint();
    }

    public Dimension getPreferredSize() { return new Dimension(300, 82); }

    public static void chip(Graphics2D g, int x, int y, int w, int h, Color c, String s) {
        g.setPaint(new GradientPaint(0, y, Theme.mix(c, Color.WHITE, .15), 0, y + h, Theme.mix(c, Theme.BG, .2)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 10, 10));
        g.setFont(Theme.font(Font.BOLD, 12));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        int W = getWidth(), H = getHeight();
        g.setColor(Theme.alpha(Color.BLACK, 60));
        g.fill(new RoundRectangle2D.Double(0, 0, W - 1, H - 1, 16, 16));
        g.setColor(Theme.BORDER);
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, W - 2, H - 2, 16, 16));

        if (res == null) {
            g.setColor(Theme.MUTED);
            g.setFont(Theme.font(Font.PLAIN, 12));
            g.drawString("Idle \u2014 configure processes and run simulation", 18, H / 2 + 4);
            g.dispose();
            return;
        }

        boolean done = cursor >= res.getTotalTime();
        g.setFont(Theme.font(Font.BOLD, 10));
        g.setColor(Theme.MUTED);
        g.drawString("CLOCK", 18, 24);
        g.drawString("CPU", 128, 24);
        g.drawString("READY QUEUE", 232, 24);

        g.setFont(Theme.font(Font.BOLD, 24));
        g.setColor(Theme.CYAN);
        g.drawString("t = " + Math.min(cursor, res.getTotalTime()), 18, 58);

        String cs;
        Color cc;
        int[] tl = res.getTimeline();
        if (done) {
            cs = "DONE";
            cc = new Color(52, 160, 110);
        } else if (cursor < tl.length && tl[cursor] == -2) {
            cs = "CS";
            cc = Theme.PURPLE;
        } else if (cursor < tl.length && tl[cursor] < 0) {
            cs = "IDLE";
            cc = new Color(74, 80, 106);
        } else if (cursor < tl.length && tl[cursor] < res.getProcesses().size()) {
            int pIdx = tl[cursor];
            cs = res.getProcesses().get(pIdx).getId();
            cc = Theme.PAL[Math.abs(pIdx) % Theme.PAL.length];
        } else {
            cs = "IDLE";
            cc = new Color(74, 80, 106);
        }
        chip(g, 128, 34, 78, 32, cc, cs);

        int x = 232;
        int[][] snaps = res.getQueueSnapshots();
        int[] q = (done || cursor >= snaps.length) ? new int[0] : snaps[cursor];
        if (q.length == 0) {
            g.setFont(Theme.font(Font.ITALIC, 12));
            g.setColor(Theme.MUTED);
            g.drawString("empty", x, 56);
        }
        for (int i = 0; i < q.length; i++) {
            if (x + 46 > W - 50 && i < q.length - 1) {
                g.setFont(Theme.font(Font.BOLD, 12));
                g.setColor(Theme.MUTED);
                g.drawString("+" + (q.length - i), x + 4, 56);
                break;
            }
            if (q[i] >= 0 && q[i] < res.getProcesses().size()) {
                chip(g, x, 34, 46, 32, Theme.PAL[Math.abs(q[i]) % Theme.PAL.length], res.getProcesses().get(q[i]).getId());
                x += 52;
            }
        }
        g.dispose();
    }
}
