package cpuscheduler.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Rounded, flat, hover-aware button with vector icons.
 */
public class FlatButton extends JButton {
    public Color base;
    public boolean primary, tab, selected, hover, press;
    public int glyph; // 0 none, 1 play, 2 pause, 3 step, 4 restart, 5 skip-end

    public FlatButton(String text, Color base, int glyph, boolean primary) {
        super(text);
        this.base = base;
        this.glyph = glyph;
        this.primary = primary;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.font(Font.BOLD, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            public void mouseExited(MouseEvent e) { hover = false; press = false; repaint(); }
            public void mousePressed(MouseEvent e) { press = true; repaint(); }
            public void mouseReleased(MouseEvent e) { press = false; repaint(); }
        });
    }

    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int iw = glyph > 0 ? 12 : 0;
        int gap = (glyph > 0 && !getText().isEmpty()) ? 7 : 0;
        return new Dimension(fm.stringWidth(getText()) + iw + gap + (tab ? 44 : 28), 36);
    }

    public static void drawGlyph(Graphics2D g, int type, int x, int cy) {
        switch (type) {
            case 1: g.fill(Theme.poly(x, cy - 6, x, cy + 6, x + 11, cy)); break;
            case 2: g.fillRect(x + 1, cy - 6, 3, 12); g.fillRect(x + 7, cy - 6, 3, 12); break;
            case 3: g.fill(Theme.poly(x, cy - 6, x, cy + 6, x + 8, cy)); g.fillRect(x + 9, cy - 6, 2, 12); break;
            case 4: g.fillRect(x, cy - 6, 2, 12); g.fill(Theme.poly(x + 11, cy - 6, x + 11, cy + 6, x + 3, cy)); break;
            case 5: g.fill(Theme.poly(x, cy - 6, x, cy + 6, x + 6, cy));
                    g.fill(Theme.poly(x + 5, cy - 6, x + 5, cy + 6, x + 11, cy)); break;
            default: break;
        }
    }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        int w = getWidth(), h = getHeight();
        boolean en = isEnabled();
        Shape sh = new RoundRectangle2D.Double(0.5, 0.5, w - 1.5, h - 1.5, 12, 12);
        Color fg = Color.WHITE;
        if (tab) {
            if (selected) {
                g.setPaint(new GradientPaint(0, 0, Theme.ACCENT, w, h, Theme.mix(Theme.ACCENT, Theme.CYAN, .55)));
                g.fill(sh);
            } else {
                g.setColor(Theme.alpha(Color.WHITE, hover ? 22 : 0));
                g.fill(sh);
                fg = hover ? Theme.TEXT : Theme.MUTED;
            }
        } else {
            Color c = base;
            if (!en) c = Theme.mix(c, Theme.BG, .55);
            else if (press) c = Theme.mix(c, Color.BLACK, .2);
            else if (hover) c = Theme.mix(c, Color.WHITE, .12);
            if (primary && en) g.setPaint(new GradientPaint(0, 0, c, w, h, Theme.mix(c, Theme.CYAN, .45)));
            else g.setColor(c);
            g.fill(sh);
            g.setColor(Theme.alpha(Color.WHITE, en ? 28 : 10));
            g.draw(sh);
            if (!en) fg = Theme.MUTED;
        }
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        String t = getText();
        int iw = glyph > 0 ? 12 : 0;
        int gap = (glyph > 0 && !t.isEmpty()) ? 7 : 0;
        int x = (w - (iw + gap + fm.stringWidth(t))) / 2;
        int cy = h / 2;
        g.setColor(fg);
        if (glyph > 0) drawGlyph(g, glyph, x, cy);
        g.drawString(t, x + iw + gap, cy + (fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }
}
