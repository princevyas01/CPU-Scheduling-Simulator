package cpuscheduler.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;

/**
 * KPI card with animated numeric values, vertical accent badge, and subtitle hint.
 */
public class MetricCard extends JComponent {
    public String title, hint, text;
    public Color color;
    public int dec;
    public String suffix;
    public double cur, target;
    public javax.swing.Timer tm;

    public MetricCard(String title, Color color, int dec, String suffix, String hint) {
        this.title = title;
        this.color = color;
        this.dec = dec;
        this.suffix = suffix;
        this.hint = hint;
    }

    public void set(double v) {
        text = null;
        target = v;
        if (tm != null) tm.stop();
        tm = new javax.swing.Timer(16, e -> {
            cur += (target - cur) * 0.22;
            if (Math.abs(target - cur) < Math.pow(10, -dec) / 2) {
                cur = target;
                tm.stop();
            }
            repaint();
        });
        tm.start();
    }

    public void setText(String t, String hint) {
        if (tm != null) tm.stop();
        this.text = t;
        this.hint = hint;
        repaint();
    }

    public Dimension getPreferredSize() { return new Dimension(150, 96); }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        int w = getWidth(), h = getHeight();
        Shape s = new RoundRectangle2D.Double(0.5, 0.5, w - 1.5, h - 1.5, 18, 18);
        g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.PANEL, Theme.PANEL2, .5), 0, h, Theme.PANEL));
        g.fill(s);
        g.setColor(Theme.BORDER);
        g.draw(s);
        g.setColor(Theme.alpha(color, 220));
        g.fill(new RoundRectangle2D.Double(14, 18, 3, h - 36, 3, 3));

        g.setFont(Theme.font(Font.BOLD, 10));
        g.setColor(Theme.MUTED);
        g.drawString(title, 26, 26);

        String v = text != null ? text
            : String.format(Locale.US, "%." + dec + "f%s", cur, suffix);
        int size = text != null ? 22 : 27;
        g.setFont(Theme.font(Font.BOLD, size));
        while (g.getFontMetrics().stringWidth(v) > w - 40 && size > 11)
            g.setFont(Theme.font(Font.BOLD, --size));
        g.setColor(color);
        g.drawString(v, 26, 58);

        g.setFont(Theme.font(Font.PLAIN, 11));
        g.setColor(Theme.MUTED);
        g.drawString(hint, 26, h - 14);
        g.dispose();
    }
}
