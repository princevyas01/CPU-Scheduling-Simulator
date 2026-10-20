package cpuscheduler.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Rounded panel container with title block, optional subtitle, and optional header action component.
 */
public class Card extends JPanel {
    public Card(String title, String sub, Component east) {
        super(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(Theme.lbl(title, 13, Theme.TEXT, true));
        if (sub != null && !sub.isEmpty()) {
            t.add(Box.createVerticalStrut(3));
            t.add(Theme.lbl(sub, 11, Theme.MUTED, false));
        }
        head.add(t, BorderLayout.WEST);
        if (east != null) head.add(east, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);
    }

    public Dimension getMinimumSize() {
        Dimension d = getPreferredSize();
        return new Dimension(Math.min(d.width, 320), d.height);
    }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        Shape s = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5, 20, 20);
        g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.PANEL, Theme.PANEL2, .35), 0, getHeight(), Theme.PANEL));
        g.fill(s);
        g.setColor(Theme.BORDER);
        g.draw(s);
        g.dispose();
    }
}
