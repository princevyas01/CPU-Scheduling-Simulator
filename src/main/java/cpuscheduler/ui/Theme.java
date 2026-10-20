package cpuscheduler.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Unified design system and theme tokens for CPU Scheduling Studio.
 * Preserves the dark navy/slate aesthetic and custom rendering primitives.
 */
public class Theme {
    public static final Color BG = new Color(10, 13, 25);
    public static final Color BG2 = new Color(20, 22, 48);
    public static final Color PANEL = new Color(21, 26, 44);
    public static final Color PANEL2 = new Color(28, 35, 58);
    public static final Color BORDER = new Color(43, 52, 80);
    public static final Color ACCENT = new Color(124, 92, 255);
    public static final Color CYAN = new Color(56, 207, 220);
    public static final Color AMBER = new Color(255, 184, 77);
    public static final Color ROSE = new Color(255, 104, 128);
    public static final Color GREEN = new Color(74, 222, 160);
    public static final Color BLUE = new Color(96, 150, 255);
    public static final Color PURPLE = new Color(192, 132, 252);
    public static final Color TEXT = new Color(236, 240, 252);
    public static final Color MUTED = new Color(150, 160, 188);

    public static final Color[] PAL = {
        new Color(124, 92, 255), new Color(34, 184, 207), new Color(255, 138, 92),
        new Color(74, 200, 130), new Color(236, 98, 170), new Color(240, 185, 60),
        new Color(90, 150, 255), new Color(190, 120, 255), new Color(255, 110, 110),
        new Color(60, 210, 180), new Color(170, 200, 70), new Color(255, 160, 200)
    };

    public static final String FONT_FAMILY = pickFont();

    private static String pickFont() {
        Set<String> have = new HashSet<>(Arrays.asList(GraphicsEnvironment
            .getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Ubuntu", "Noto Sans", "Arial"}) {
            if (have.contains(f)) return f;
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(FONT_FAMILY, style, 12).deriveFont(size);
    }

    public static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static Color mix(Color a, Color b, double t) {
        return new Color(
            (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t)
        );
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public static Polygon poly(int... xy) {
        Polygon p = new Polygon();
        for (int i = 0; i < xy.length; i += 2) p.addPoint(xy[i], xy[i + 1]);
        return p;
    }

    public static JLabel lbl(String text, int size, Color color, boolean bold) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(font(bold ? Font.BOLD : Font.PLAIN, size));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    public static String f2(double v) {
        return String.format(Locale.US, "%.2f", v);
    }
}
