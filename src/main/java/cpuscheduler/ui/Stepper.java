package cpuscheduler.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Increment/decrement stepper component for numeric configuration values.
 */
public class Stepper extends JPanel {
    public int value, min, max;
    public JLabel lab;
    public FlatButton minus, plus;

    public Stepper(int v, int min, int max) {
        super(new BorderLayout(8, 0));
        this.value = v;
        this.min = min;
        this.max = max;
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        minus = new FlatButton("-", Theme.PANEL2, 0, false);
        plus = new FlatButton("+", Theme.PANEL2, 0, false);
        lab = Theme.lbl("" + v, 15, Theme.TEXT, true);
        lab.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel box = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.aa(g);
                g.setColor(Theme.PANEL2);
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g.setColor(Theme.BORDER);
                g.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 12, 12));
                g.dispose();
            }
        };
        box.setOpaque(false);
        box.add(lab);
        minus.addActionListener(e -> set(value - 1));
        plus.addActionListener(e -> set(value + 1));
        add(minus, BorderLayout.WEST);
        add(box, BorderLayout.CENTER);
        add(plus, BorderLayout.EAST);
    }

    public void set(int v) {
        value = Math.max(min, Math.min(max, v));
        lab.setText("" + value);
    }

    public int getValue() {
        return value;
    }

    public void setEnabled(boolean en) {
        super.setEnabled(en);
        minus.setEnabled(en);
        plus.setEnabled(en);
        lab.setForeground(en ? Theme.TEXT : Theme.MUTED);
    }
}
