import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * CPU Scheduling Studio
 * ---------------------------------------------------------------
 * Algorithms : FCFS, SJF, SRTF, LJF, LRTF, HRRN, Priority (NP / P),
 *              Round Robin, Multilevel Feedback Queue
 * Features   : animated Gantt chart, live CPU / ready-queue view,
 *              animated metric cards, "Compare All" dashboard,
 *              CSV export, random workload generator, input validation.
 *
 * Compile : javac Main.java      Run : java Main
 */
public class Main extends JFrame {

    // ====================================================== THEME
    static final Color BG = new Color(10, 13, 25);
    static final Color BG2 = new Color(20, 22, 48);
    static final Color PANEL = new Color(21, 26, 44);
    static final Color PANEL2 = new Color(28, 35, 58);
    static final Color BORDER = new Color(43, 52, 80);
    static final Color ACCENT = new Color(124, 92, 255);
    static final Color CYAN = new Color(56, 207, 220);
    static final Color AMBER = new Color(255, 184, 77);
    static final Color ROSE = new Color(255, 104, 128);
    static final Color GREEN = new Color(74, 222, 160);
    static final Color BLUE = new Color(96, 150, 255);
    static final Color TEXT = new Color(236, 240, 252);
    static final Color MUTED = new Color(150, 160, 188);
    static final Color[] PAL = {
        new Color(124, 92, 255), new Color(34, 184, 207), new Color(255, 138, 92),
        new Color(74, 200, 130), new Color(236, 98, 170), new Color(240, 185, 60),
        new Color(90, 150, 255), new Color(190, 120, 255), new Color(255, 110, 110),
        new Color(60, 210, 180), new Color(170, 200, 70), new Color(255, 160, 200)
    };

    static final String FONT = pickFont();

    static String pickFont() {
        Set<String> have = new HashSet<>(Arrays.asList(GraphicsEnvironment
            .getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue",
                "Ubuntu", "Noto Sans", "Arial"})
            if (have.contains(f)) return f;
        return Font.SANS_SERIF;
    }

    static Font font(int style, float size) {
        return new Font(FONT, style, 12).deriveFont(size);
    }

    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static Color mix(Color a, Color b, double t) {
        return new Color(
            (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Polygon poly(int... xy) {
        Polygon p = new Polygon();
        for (int i = 0; i < xy.length; i += 2) p.addPoint(xy[i], xy[i + 1]);
        return p;
    }

    static JLabel lbl(String text, int size, Color color, boolean bold) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(font(bold ? Font.BOLD : Font.PLAIN, size));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static String f2(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    // ====================================================== ALGORITHMS
    static final String A_FCFS = "FCFS";
    static final String A_SJF = "SJF (Non-preemptive)";
    static final String A_SRTF = "SRTF (Preemptive)";
    static final String A_LJF = "LJF (Non-preemptive)";
    static final String A_LRTF = "LRTF (Preemptive)";
    static final String A_HRRN = "HRRN";
    static final String A_PRI = "Priority (Non-preemptive)";
    static final String A_PRIP = "Priority (Preemptive)";
    static final String A_RR = "Round Robin";
    static final String A_MLFQ = "Multilevel Feedback Queue";

    static final String[] ALGOS = {A_FCFS, A_SJF, A_SRTF, A_LJF, A_LRTF, A_HRRN,
        A_PRI, A_PRIP, A_RR, A_MLFQ};

    static final Map<String, String> DESC = new HashMap<>();

    static {
        DESC.put(A_FCFS, "First Come First Served. Runs processes strictly in arrival order, "
            + "non-preemptive. Simple and fair, but short jobs can get stuck behind a long one (convoy effect).");
        DESC.put(A_SJF, "Shortest Job First. Among the arrived processes, runs the one with the smallest "
            + "burst time to completion. Minimises average waiting time for non-preemptive policies; long jobs may starve.");
        DESC.put(A_SRTF, "Shortest Remaining Time First (preemptive SJF). Every time unit the process with the "
            + "least remaining time runs, so a short newcomer can preempt the running process.");
        DESC.put(A_LJF, "Longest Job First. Picks the largest burst time among arrived processes and runs it "
            + "to completion. Rarely optimal - useful as a contrast to SJF.");
        DESC.put(A_LRTF, "Longest Remaining Time First (preemptive LJF). The process with the most remaining "
            + "time runs each time unit, so equally long jobs alternate.");
        DESC.put(A_HRRN, "Highest Response Ratio Next. Picks the process with the largest "
            + "(waiting + burst) / burst ratio, non-preemptive. Long waits raise the ratio, preventing starvation.");
        DESC.put(A_PRI, "Non-preemptive priority scheduling. The arrived process with the best priority "
            + "(lower number = higher priority) runs to completion. Ties break by arrival time.");
        DESC.put(A_PRIP, "Preemptive priority scheduling. The most urgent process runs each time unit and "
            + "preempts lower-priority work as soon as a more urgent process arrives.");
        DESC.put(A_RR, "Round Robin. Each process runs for at most one time quantum, then goes to the back "
            + "of the ready queue. Fair and responsive; the quantum sets the trade-off with context switches.");
        DESC.put(A_MLFQ, "Multilevel Feedback Queue with 3 levels. New processes start in queue 0 (quantum Q). "
            + "Using a full quantum demotes a process to queue 1 (2Q) and then queue 2 (FCFS). "
            + "Higher queues preempt lower ones.");
    }

    static String shortName(String a) {
        return a.replace(" (Non-preemptive)", " NP").replace(" (Preemptive)", " P")
            .replace("Multilevel Feedback Queue", "MLFQ").replace("Round Robin", "RR");
    }

    // ====================================================== DATA
    static class Proc {
        String id;
        int at, bt, pr, order;
        int start = -1, ct, tat, wt, rt;

        Proc(String id, int at, int bt, int pr, int order) {
            this.id = id;
            this.at = at;
            this.bt = bt;
            this.pr = pr;
            this.order = order;
        }
    }

    static class Seg {
        int p, s, e; // p = process index, -1 = idle

        Seg(int p, int s, int e) {
            this.p = p;
            this.s = s;
            this.e = e;
        }
    }

    static class Result {
        String algo;
        Proc[] procs;
        List<Seg> segs = new ArrayList<>();
        int[] tl;          // process index running in each time unit (-1 = idle)
        int[][] snap;      // ready queue snapshot for each time unit
        int total, switches;
        double avgWT, avgTAT, avgRT, util, thr;
    }

    // ====================================================== ENGINE
    static double key(String alg, Proc x, int rem, int t) {
        switch (alg) {
            case A_SJF: return x.bt;
            case A_LJF: return -x.bt;
            case A_SRTF: return rem;
            case A_LRTF: return -rem;
            case A_PRI:
            case A_PRIP: return x.pr;
            case A_HRRN: return -(((t - x.at) + (double) x.bt) / x.bt);
            default: return x.at; // FCFS
        }
    }

    static boolean less(String alg, Proc[] p, int[] rem, int a, int b, int t) {
        double ka = key(alg, p[a], rem[a], t), kb = key(alg, p[b], rem[b], t);
        if (ka != kb) return ka < kb;
        if (p[a].at != p[b].at) return p[a].at < p[b].at;
        return a < b;
    }

    static int[] toArr(List<Integer> l) {
        int[] r = new int[l.size()];
        for (int i = 0; i < r.length; i++) r[i] = l.get(i);
        return r;
    }

    static Result simulate(List<Proc> in, String alg, int q) {
        final int INF = Integer.MAX_VALUE;
        int n = in.size();
        Proc[] p = new Proc[n];
        for (int i = 0; i < n; i++) {
            Proc x = in.get(i);
            p[i] = new Proc(x.id, x.at, x.bt, x.pr, i);
        }
        Integer[] byArr = new Integer[n];
        for (int i = 0; i < n; i++) byArr[i] = i;
        Arrays.sort(byArr, (a, b) -> p[a].at != p[b].at
            ? Integer.compare(p[a].at, p[b].at) : Integer.compare(a, b));

        int[] rem = new int[n], lvl = new int[n];
        boolean[] arrived = new boolean[n];
        boolean rr = alg.equals(A_RR), mlfq = alg.equals(A_MLFQ);
        boolean pre = alg.equals(A_SRTF) || alg.equals(A_LRTF) || alg.equals(A_PRIP);
        for (int i = 0; i < n; i++) rem[i] = p[i].bt;

        List<Integer> ready = new ArrayList<>();
        List<ArrayDeque<Integer>> mq = new ArrayList<>();
        for (int i = 0; i < 3; i++) mq.add(new ArrayDeque<>());
        List<Integer> tl = new ArrayList<>();
        List<int[]> snaps = new ArrayList<>();

        int t = 0, done = 0, cur = -1, qLeft = 0;

        while (done < n) {
            // 1) admit arrivals
            for (int k = 0; k < n; k++) {
                int i = byArr[k];
                if (!arrived[i] && p[i].at <= t) {
                    arrived[i] = true;
                    if (rr || mlfq) mq.get(0).addLast(i);
                    else ready.add(i);
                }
            }

            // 2) choose who runs during [t, t+1)
            if (rr) {
                if (cur >= 0 && qLeft == 0) {
                    mq.get(0).addLast(cur);
                    cur = -1;
                }
                if (cur < 0 && !mq.get(0).isEmpty()) {
                    cur = mq.get(0).pollFirst();
                    qLeft = q;
                }
            } else if (mlfq) {
                if (cur >= 0) {
                    boolean higher = false;
                    for (int l = 0; l < lvl[cur]; l++) if (!mq.get(l).isEmpty()) higher = true;
                    if (qLeft == 0) {
                        lvl[cur] = Math.min(lvl[cur] + 1, 2);
                        mq.get(lvl[cur]).addLast(cur);
                        cur = -1;
                    } else if (higher) {
                        mq.get(lvl[cur]).addLast(cur);
                        cur = -1;
                    }
                }
                if (cur < 0) {
                    for (int l = 0; l < 3; l++) {
                        if (!mq.get(l).isEmpty()) {
                            cur = mq.get(l).pollFirst();
                            lvl[cur] = l;
                            qLeft = (l == 2) ? INF : (q << l);
                            break;
                        }
                    }
                }
            } else if (cur < 0 || pre) {
                int best = cur;
                for (int i : ready) if (best < 0 || less(alg, p, rem, i, best, t)) best = i;
                if (best != cur) {
                    if (cur >= 0) ready.add(cur);
                    ready.remove(Integer.valueOf(best));
                    cur = best;
                }
            }

            // 3) snapshot of the waiting queue
            List<Integer> w = new ArrayList<>();
            if (rr || mlfq) {
                for (ArrayDeque<Integer> d : mq) w.addAll(d);
            } else {
                w.addAll(ready);
                w.sort((a, b) -> p[a].at != p[b].at
                    ? Integer.compare(p[a].at, p[b].at) : Integer.compare(a, b));
            }
            snaps.add(toArr(w));
            tl.add(cur);

            // 4) execute one time unit
            if (cur >= 0) {
                if (p[cur].start < 0) p[cur].start = t;
                rem[cur]--;
                if (qLeft != INF && qLeft > 0) qLeft--;
                t++;
                if (rem[cur] == 0) {
                    p[cur].ct = t;
                    done++;
                    cur = -1;
                    qLeft = 0;
                }
            } else {
                t++;
            }
        }

        Result r = new Result();
        r.algo = alg;
        r.procs = p;
        r.total = t;
        r.tl = toArr(tl);
        r.snap = snaps.toArray(new int[0][]);

        double sw = 0, st = 0, sr = 0;
        int busy = 0;
        for (Proc x : p) {
            x.tat = x.ct - x.at;
            x.wt = x.tat - x.bt;
            x.rt = x.start - x.at;
            sw += x.wt;
            st += x.tat;
            sr += x.rt;
            busy += x.bt;
        }
        r.avgWT = sw / n;
        r.avgTAT = st / n;
        r.avgRT = sr / n;
        r.util = busy * 100.0 / t;
        r.thr = n / (double) t;

        int s = 0;
        for (int i = 1; i <= t; i++) {
            if (i == t || r.tl[i] != r.tl[s]) {
                r.segs.add(new Seg(r.tl[s], s, i));
                s = i;
            }
        }
        for (int i = 1; i < r.segs.size(); i++)
            if (r.segs.get(i).p >= 0 && r.segs.get(i - 1).p >= 0) r.switches++;
        return r;
    }

    // ====================================================== CUSTOM COMPONENTS

    /** Rounded, flat, hover-aware button with vector icons. */
    static class FlatButton extends JButton {
        Color base;
        boolean primary, tab, selected, hover, press;
        int glyph; // 0 none, 1 play, 2 pause, 3 step, 4 restart, 5 skip-end

        FlatButton(String text, Color base, int glyph, boolean primary) {
            super(text);
            this.base = base;
            this.glyph = glyph;
            this.primary = primary;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(font(Font.BOLD, 12));
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

        static void drawGlyph(Graphics2D g, int type, int x, int cy) {
            switch (type) {
                case 1: g.fill(poly(x, cy - 6, x, cy + 6, x + 11, cy)); break;
                case 2: g.fillRect(x + 1, cy - 6, 3, 12); g.fillRect(x + 7, cy - 6, 3, 12); break;
                case 3: g.fill(poly(x, cy - 6, x, cy + 6, x + 8, cy)); g.fillRect(x + 9, cy - 6, 2, 12); break;
                case 4: g.fillRect(x, cy - 6, 2, 12); g.fill(poly(x + 11, cy - 6, x + 11, cy + 6, x + 3, cy)); break;
                case 5: g.fill(poly(x, cy - 6, x, cy + 6, x + 6, cy));
                        g.fill(poly(x + 5, cy - 6, x + 5, cy + 6, x + 11, cy)); break;
                default: break;
            }
        }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            boolean en = isEnabled();
            Shape sh = new RoundRectangle2D.Double(0.5, 0.5, w - 1.5, h - 1.5, 12, 12);
            Color fg = Color.WHITE;
            if (tab) {
                if (selected) {
                    g.setPaint(new GradientPaint(0, 0, ACCENT, w, h, mix(ACCENT, CYAN, .55)));
                    g.fill(sh);
                } else {
                    g.setColor(alpha(Color.WHITE, hover ? 22 : 0));
                    g.fill(sh);
                    fg = hover ? TEXT : MUTED;
                }
            } else {
                Color c = base;
                if (!en) c = mix(c, BG, .55);
                else if (press) c = mix(c, Color.BLACK, .2);
                else if (hover) c = mix(c, Color.WHITE, .12);
                if (primary && en) g.setPaint(new GradientPaint(0, 0, c, w, h, mix(c, CYAN, .45)));
                else g.setColor(c);
                g.fill(sh);
                g.setColor(alpha(Color.WHITE, en ? 28 : 10));
                g.draw(sh);
                if (!en) fg = MUTED;
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

    /** Rounded surface with a title block. */
    static class Card extends JPanel {
        Card(String title, String sub, Component east) {
            super(new BorderLayout(0, 10));
            setOpaque(false);
            setBorder(new EmptyBorder(16, 18, 16, 18));
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            JPanel t = new JPanel();
            t.setOpaque(false);
            t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
            t.add(lbl(title, 13, TEXT, true));
            t.add(Box.createVerticalStrut(3));
            t.add(lbl(sub, 11, MUTED, false));
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
            aa(g);
            Shape s = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5, 20, 20);
            g.setPaint(new GradientPaint(0, 0, mix(PANEL, PANEL2, .35), 0, getHeight(), PANEL));
            g.fill(s);
            g.setColor(BORDER);
            g.draw(s);
            g.dispose();
        }
    }

    /** KPI card with an animated number. */
    static class MetricCard extends JComponent {
        String title, hint, text;
        Color color;
        int dec;
        String suffix;
        double cur, target;
        javax.swing.Timer tm;

        MetricCard(String title, Color color, int dec, String suffix, String hint) {
            this.title = title;
            this.color = color;
            this.dec = dec;
            this.suffix = suffix;
            this.hint = hint;
        }

        void set(double v) {
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

        void setText(String t, String hint) {
            if (tm != null) tm.stop();
            this.text = t;
            this.hint = hint;
            repaint();
        }

        public Dimension getPreferredSize() { return new Dimension(150, 96); }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            Shape s = new RoundRectangle2D.Double(0.5, 0.5, w - 1.5, h - 1.5, 18, 18);
            g.setPaint(new GradientPaint(0, 0, mix(PANEL, PANEL2, .5), 0, h, PANEL));
            g.fill(s);
            g.setColor(BORDER);
            g.draw(s);
            g.setColor(alpha(color, 220));
            g.fill(new RoundRectangle2D.Double(14, 18, 3, h - 36, 3, 3));

            g.setFont(font(Font.BOLD, 10));
            g.setColor(MUTED);
            g.drawString(title, 26, 26);

            String v = text != null ? text
                : String.format(Locale.US, "%." + dec + "f%s", cur, suffix);
            int size = text != null ? 22 : 27;
            g.setFont(font(Font.BOLD, size));
            while (g.getFontMetrics().stringWidth(v) > w - 40 && size > 11)
                g.setFont(font(Font.BOLD, --size));
            g.setColor(color);
            g.drawString(v, 26, 58);

            g.setFont(font(Font.PLAIN, 11));
            g.setColor(MUTED);
            g.drawString(hint, 26, h - 14);
            g.dispose();
        }
    }

    /** +/- stepper used for the time quantum. */
    static class Stepper extends JPanel {
        int value, min, max;
        JLabel lab;
        FlatButton minus, plus;

        Stepper(int v, int min, int max) {
            super(new BorderLayout(8, 0));
            this.value = v;
            this.min = min;
            this.max = max;
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            minus = new FlatButton("-", PANEL2, 0, false);
            plus = new FlatButton("+", PANEL2, 0, false);
            lab = lbl("" + v, 15, TEXT, true);
            lab.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel box = new JPanel(new BorderLayout()) {
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    aa(g);
                    g.setColor(PANEL2);
                    g.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                    g.setColor(BORDER);
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

        void set(int v) {
            value = Math.max(min, Math.min(max, v));
            lab.setText("" + value);
        }

        public void setEnabled(boolean en) {
            super.setEnabled(en);
            minus.setEnabled(en);
            plus.setEnabled(en);
            lab.setForeground(en ? TEXT : MUTED);
        }
    }

    /** Gantt chart with animation cursor, arrival markers and tooltips. */
    static class GanttPanel extends JComponent implements Scrollable {
        static final int PADL = 22, PADR = 22, MINU = 28;
        Result res;
        int cursor;
        List<Rectangle2D> rects = new ArrayList<>();
        List<Seg> rectSeg = new ArrayList<>();

        GanttPanel() { setToolTipText(""); }

        void setResult(Result r) {
            res = r;
            revalidate();
            repaint();
            scrollRectToVisible(new Rectangle(0, 0, 1, 1));
        }

        void setCursor(int c) {
            cursor = c;
            repaint();
        }

        void ensureCursorVisible() {
            if (res == null || res.total == 0) return;
            double u = (getWidth() - PADL - PADR) / (double) res.total;
            int x = (int) (PADL + cursor * u);
            scrollRectToVisible(new Rectangle(x - 80, 0, 220, getHeight()));
        }

        public String getToolTipText(MouseEvent e) {
            for (int i = 0; i < rects.size(); i++) {
                if (rects.get(i).contains(e.getPoint())) {
                    Seg s = rectSeg.get(i);
                    String n = s.p < 0 ? "CPU idle" : res.procs[s.p].id;
                    return n + ":  " + s.s + " \u2192 " + s.e + "   (" + (s.e - s.s) + " units)";
                }
            }
            return null;
        }

        public Dimension getPreferredSize() {
            int n = res == null ? 1 : res.total;
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
            aa(g);
            int W = getWidth(), H = getHeight();
            rects.clear();
            rectSeg.clear();
            if (res == null) {
                g.setColor(MUTED);
                g.setFont(font(Font.PLAIN, 13));
                String m = "Run a simulation to see the Gantt chart";
                g.drawString(m, (W - g.getFontMetrics().stringWidth(m)) / 2, H / 2);
                g.dispose();
                return;
            }
            double u = (W - PADL - PADR) / (double) res.total;
            int top = 50, bh = 62, axisY = top + bh + 12;

            // arrival markers
            Map<Integer, Integer> stack = new HashMap<>();
            for (Proc p : res.procs) {
                int k = stack.merge(p.at, 1, Integer::sum) - 1;
                double x = PADL + p.at * u;
                int y = Math.max(8, top - 14 - k * 9);
                g.setColor(PAL[p.order % PAL.length]);
                g.fill(poly((int) x - 4, y - 6, (int) x + 4, y - 6, (int) x, y + 2));
            }
            g.setFont(font(Font.PLAIN, 9));
            g.setColor(MUTED);
            g.drawString("arrivals", 4, 10);

            // blocks
            for (Seg s : res.segs) {
                double x = PADL + s.s * u, w = (s.e - s.s) * u;
                boolean idle = s.p < 0;
                Color c = idle ? new Color(64, 70, 94) : PAL[s.p % PAL.length];
                RoundRectangle2D box = new RoundRectangle2D.Double(x + 1, top, w - 2, bh, 12, 12);
                rects.add(box.getBounds2D());
                rectSeg.add(s);

                double vis = cursor >= s.e ? 1 : cursor <= s.s ? 0 : (cursor - s.s) / (double) (s.e - s.s);
                if (vis < 1) {
                    g.setColor(alpha(c, 38));
                    g.fill(box);
                    g.setColor(alpha(c, 110));
                    g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10f, new float[]{4f, 4f}, 0f));
                    g.draw(box);
                    g.setStroke(new BasicStroke(1f));
                }
                if (vis > 0) {
                    Shape old = g.getClip();
                    g.clip(new Rectangle2D.Double(x, 0, w * vis + 1, H));
                    g.setPaint(new GradientPaint(0, top, mix(c, Color.WHITE, .18), 0, top + bh, mix(c, BG, .22)));
                    g.fill(box);
                    if (idle) {
                        g.clip(box);
                        g.setColor(alpha(Color.WHITE, 20));
                        for (double sx = x - bh; sx < x + w; sx += 10)
                            g.draw(new Line2D.Double(sx, top + bh, sx + bh, top));
                    }
                    g.setClip(old);
                    old = g.getClip();
                    g.clip(new Rectangle2D.Double(x, 0, w * vis + 1, H));
                    if (w >= 30) {
                        String name = idle ? "idle" : res.procs[s.p].id;
                        g.setColor(Color.WHITE);
                        g.setFont(font(Font.BOLD, idle ? 11 : 14));
                        FontMetrics fm = g.getFontMetrics();
                        g.drawString(name, (float) (x + (w - fm.stringWidth(name)) / 2), top + bh / 2f);
                        g.setFont(font(Font.PLAIN, 10));
                        g.setColor(alpha(Color.WHITE, 190));
                        String d = (s.e - s.s) + "u";
                        fm = g.getFontMetrics();
                        g.drawString(d, (float) (x + (w - fm.stringWidth(d)) / 2), top + bh / 2f + 17);
                    }
                    g.setClip(old);
                }
            }

            // axis + ticks
            g.setColor(BORDER);
            g.drawLine(PADL, axisY, W - PADR, axisY);
            g.setFont(font(Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            double lastX = -100;
            List<Integer> times = new ArrayList<>();
            for (Seg s : res.segs) times.add(s.s);
            times.add(res.total);
            for (int tt : times) {
                double x = PADL + tt * u;
                g.setColor(BORDER);
                g.drawLine((int) x, axisY, (int) x, axisY + 5);
                if (x - lastX >= 26 || tt == res.total) {
                    g.setColor(MUTED);
                    String s = "" + tt;
                    g.drawString(s, (float) (x - fm.stringWidth(s) / 2.0), axisY + 20);
                    lastX = x;
                }
            }

            // playhead
            if (cursor >= 0 && cursor < res.total) {
                double x = PADL + cursor * u;
                g.setColor(alpha(CYAN, 40));
                g.setStroke(new BasicStroke(8f));
                g.draw(new Line2D.Double(x, top - 6, x, axisY));
                g.setColor(CYAN);
                g.setStroke(new BasicStroke(2f));
                g.draw(new Line2D.Double(x, top - 6, x, axisY + 4));
                g.fill(poly((int) x - 5, top - 14, (int) x + 5, top - 14, (int) x, top - 5));
            }
            g.dispose();
        }
    }

    /** Clock / CPU / ready-queue strip shown above the Gantt chart. */
    static class LiveView extends JComponent {
        Result res;
        int cursor;

        void setState(Result r, int c) {
            res = r;
            cursor = c;
            repaint();
        }

        public Dimension getPreferredSize() { return new Dimension(300, 82); }

        static void chip(Graphics2D g, int x, int y, int w, int h, Color c, String s) {
            g.setPaint(new GradientPaint(0, y, mix(c, Color.WHITE, .15), 0, y + h, mix(c, BG, .2)));
            g.fill(new RoundRectangle2D.Double(x, y, w, h, 10, 10));
            g.setFont(font(Font.BOLD, 12));
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
        }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int W = getWidth(), H = getHeight();
            g.setColor(alpha(Color.BLACK, 60));
            g.fill(new RoundRectangle2D.Double(0, 0, W - 1, H - 1, 16, 16));
            g.setColor(BORDER);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, W - 2, H - 2, 16, 16));
            if (res == null) {
                g.setColor(MUTED);
                g.setFont(font(Font.PLAIN, 12));
                g.drawString("Idle \u2014 add processes and press Run", 18, H / 2 + 4);
                g.dispose();
                return;
            }
            boolean done = cursor >= res.total;
            g.setFont(font(Font.BOLD, 10));
            g.setColor(MUTED);
            g.drawString("CLOCK", 18, 24);
            g.drawString("CPU", 128, 24);
            g.drawString("READY QUEUE", 232, 24);

            g.setFont(font(Font.BOLD, 24));
            g.setColor(CYAN);
            g.drawString("t = " + Math.min(cursor, res.total), 18, 58);

            String cs;
            Color cc;
            if (done) { cs = "DONE"; cc = new Color(52, 160, 110); }
            else if (res.tl[cursor] < 0) { cs = "IDLE"; cc = new Color(74, 80, 106); }
            else { cs = res.procs[res.tl[cursor]].id; cc = PAL[res.tl[cursor] % PAL.length]; }
            chip(g, 128, 34, 78, 32, cc, cs);

            int x = 232;
            int[] q = done ? new int[0] : res.snap[cursor];
            if (q.length == 0) {
                g.setFont(font(Font.ITALIC, 12));
                g.setColor(MUTED);
                g.drawString("empty", x, 56);
            }
            for (int i = 0; i < q.length; i++) {
                if (x + 46 > W - 50 && i < q.length - 1) {
                    g.setFont(font(Font.BOLD, 12));
                    g.setColor(MUTED);
                    g.drawString("+" + (q.length - i), x + 4, 56);
                    break;
                }
                chip(g, x, 34, 46, 32, PAL[q[i] % PAL.length], res.procs[q[i]].id);
                x += 52;
            }
            g.dispose();
        }
    }

    /** Horizontal grouped bar chart (Compare All). */
    static class BarChart extends JComponent {
        List<Result> rs = new ArrayList<>();

        void set(List<Result> r) {
            rs = r;
            revalidate();
            repaint();
        }

        public Dimension getPreferredSize() { return new Dimension(420, 70 + rs.size() * 36); }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int W = getWidth(), H = getHeight();
            if (rs.isEmpty()) { g.dispose(); return; }
            Color[] cols = {AMBER, CYAN, GREEN};
            String[] names = {"Waiting", "Turnaround", "Response"};
            int lx = 0;
            g.setFont(font(Font.PLAIN, 11));
            for (int i = 0; i < 3; i++) {
                g.setColor(cols[i]);
                g.fill(new RoundRectangle2D.Double(lx, 4, 10, 10, 4, 4));
                g.setColor(MUTED);
                g.drawString(names[i], lx + 15, 13);
                lx += 28 + g.getFontMetrics().stringWidth(names[i]) + 12;
            }
            double[][] v = new double[rs.size()][3];
            double max = 0.0001;
            double[] mn = {1e18, 1e18, 1e18};
            for (int i = 0; i < rs.size(); i++) {
                v[i][0] = rs.get(i).avgWT;
                v[i][1] = rs.get(i).avgTAT;
                v[i][2] = rs.get(i).avgRT;
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
                g.setFont(font(Font.BOLD, 11));
                g.setColor(TEXT);
                String nm = shortName(rs.get(i).algo);
                g.drawString(nm, labelW - g.getFontMetrics().stringWidth(nm), (float) (gy + gh / 2 + 4));
                double by = gy + (gh - (bh * 3 + 4)) / 2;
                for (int k = 0; k < 3; k++) {
                    double w = Math.max(2, v[i][k] / max * (x1 - x0));
                    boolean best = Math.abs(v[i][k] - mn[k]) < 1e-9;
                    g.setColor(alpha(PANEL2, 255));
                    g.fill(new RoundRectangle2D.Double(x0, by + k * (bh + 2), x1 - x0, bh, bh, bh));
                    g.setPaint(new GradientPaint((float) x0, 0, alpha(cols[k], 150), (float) (x0 + w), 0, cols[k]));
                    g.fill(new RoundRectangle2D.Double(x0, by + k * (bh + 2), w, bh, bh, bh));
                    if (best) {
                        g.setColor(Color.WHITE);
                        g.setStroke(new BasicStroke(1.2f));
                        g.draw(new RoundRectangle2D.Double(x0, by + k * (bh + 2), w, bh, bh, bh));
                        g.setStroke(new BasicStroke(1f));
                    }
                    g.setFont(font(Font.PLAIN, 9));
                    g.setColor(best ? TEXT : MUTED);
                    g.drawString(f2(v[i][k]), (float) (x0 + w + 5), (float) (by + k * (bh + 2) + bh - 0.5));
                }
            }
            g.dispose();
        }
    }

    /** Stacked mini Gantt strips, one per algorithm. */
    static class MiniGantts extends JComponent {
        List<Result> rs = new ArrayList<>();

        void set(List<Result> r) {
            rs = r;
            revalidate();
            repaint();
        }

        public Dimension getPreferredSize() { return new Dimension(600, rs.size() * 32 + 6); }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int W = getWidth(), labelW = 110, rowH = 32;
            int maxT = 1;
            for (Result r : rs) maxT = Math.max(maxT, r.total);
            double u = (W - labelW - 24) / (double) maxT;
            for (int i = 0; i < rs.size(); i++) {
                Result r = rs.get(i);
                int y = i * rowH + 4;
                g.setFont(font(Font.BOLD, 11));
                g.setColor(TEXT);
                g.drawString(shortName(r.algo), 0, y + 18);
                for (Seg s : r.segs) {
                    double x = labelW + s.s * u, w = (s.e - s.s) * u;
                    Color c = s.p < 0 ? new Color(64, 70, 94) : PAL[s.p % PAL.length];
                    g.setColor(c);
                    g.fill(new RoundRectangle2D.Double(x + 0.5, y + 2, Math.max(1, w - 1), 22, 6, 6));
                    if (w >= 22 && s.p >= 0) {
                        g.setFont(font(Font.BOLD, 10));
                        g.setColor(Color.WHITE);
                        String n = r.procs[s.p].id;
                        g.drawString(n, (float) (x + (w - g.getFontMetrics().stringWidth(n)) / 2), y + 17);
                    }
                }
                g.setColor(MUTED);
                g.setFont(font(Font.PLAIN, 10));
                g.drawString("" + r.total, (float) (labelW + r.total * u + 5), y + 17);
            }
            g.dispose();
        }
    }

    /** Panel that tracks viewport width, so BoxLayout content resizes with the window. */
    static class ScrollBody extends JPanel implements Scrollable {
        boolean fillHeight;
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 200; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() {
            return fillHeight && getParent() instanceof JViewport
                && getParent().getHeight() > getPreferredSize().height;
        }
    }

    static class Logo extends JComponent {
        public Dimension getPreferredSize() { return new Dimension(46, 46); }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setPaint(new GradientPaint(0, 0, ACCENT, 46, 46, CYAN));
            g.fill(new RoundRectangle2D.Double(0, 0, 45, 45, 14, 14));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(13, 13, 19, 19, 5, 5));
            for (int i = 0; i < 3; i++) {
                int o = 17 + i * 6;
                g.drawLine(o, 8, o, 13);
                g.drawLine(o, 32, o, 37);
                g.drawLine(8, o, 13, o);
                g.drawLine(32, o, 37, o);
            }
            g.fill(new RoundRectangle2D.Double(19, 19, 7, 7, 2, 2));
            g.dispose();
        }
    }

    // ====================================================== TABLE / SCROLL / COMBO STYLING

    static class CellR extends DefaultTableCellRenderer {
        Color[] cols;

        CellR(Color[] cols) { this.cols = cols; }

        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setHorizontalAlignment(CENTER);
            setBorder(new EmptyBorder(0, 6, 0, 6));
            setBackground(t.isRowSelected(row) ? new Color(64, 58, 120)
                : (row % 2 == 0 ? PANEL2 : mix(PANEL2, BG, .22)));
            if (col == 0) {
                setForeground(PAL[row % PAL.length]);
                setFont(font(Font.BOLD, 12));
            } else {
                setForeground(cols != null && cols[col] != null ? cols[col] : TEXT);
                setFont(font(Font.PLAIN, 12));
            }
            return this;
        }
    }

    static final double[] BEST = new double[7];

    static class CmpR extends CellR {
        CmpR() { super(null); }

        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            if (col == 0) {
                setHorizontalAlignment(LEFT);
                setForeground(TEXT);
            } else {
                try {
                    double d = Double.parseDouble(String.valueOf(v).replace("%", ""));
                    if (Math.abs(d - BEST[col]) < 1e-9) {
                        setForeground(GREEN);
                        setFont(font(Font.BOLD, 12));
                    }
                } catch (NumberFormatException ignored) { }
            }
            return this;
        }
    }

    static class HeaderR extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setHorizontalAlignment(col == 0 && t.getName() != null && t.getName().equals("cmp") ? LEFT : CENTER);
            setBackground(new Color(34, 42, 68));
            setForeground(CYAN);
            setFont(font(Font.BOLD, 11));
            setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(7, 6, 7, 6)));
            return this;
        }
    }

    /** Integer cell editor with validation (red border on bad input). */
    static class IntEditor extends DefaultCellEditor {
        final int min;
        final JTextField f;

        IntEditor(int min) {
            super(new JTextField());
            this.min = min;
            f = (JTextField) getComponent();
            f.setHorizontalAlignment(JTextField.CENTER);
            f.setBackground(new Color(40, 48, 78));
            f.setForeground(TEXT);
            f.setCaretColor(CYAN);
            f.setFont(font(Font.BOLD, 12));
            f.setBorder(new LineBorder(CYAN));
            setClickCountToStart(1);
        }

        public boolean stopCellEditing() {
            try {
                int v = Integer.parseInt(f.getText().trim());
                if (v < min || v > 999) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                f.setBorder(new LineBorder(ROSE, 2));
                return false;
            }
            f.setBorder(new LineBorder(CYAN));
            return super.stopCellEditing();
        }

        public Object getCellEditorValue() {
            return Integer.parseInt(f.getText().trim());
        }
    }

    static class SlimScroll extends BasicScrollBarUI {
        JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        protected JButton createDecreaseButton(int o) { return zero(); }
        protected JButton createIncreaseButton(int o) { return zero(); }

        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(alpha(Color.BLACK, 40));
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(isThumbRollover() || isDragging ? new Color(110, 122, 170) : new Color(74, 86, 125));
            g.fill(new RoundRectangle2D.Double(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8));
            g.dispose();
        }
    }

    static void styleScroll(JScrollPane s) {
        s.setBorder(new LineBorder(BORDER, 1, true));
        s.getViewport().setBackground(PANEL2);
        JScrollBar v = s.getVerticalScrollBar(), h = s.getHorizontalScrollBar();
        if (v != null) {
            v.setUI(new SlimScroll());
            v.setPreferredSize(new Dimension(11, 11));
            v.setUnitIncrement(16);
        }
        if (h != null) {
            h.setUI(new SlimScroll());
            h.setPreferredSize(new Dimension(11, 11));
            h.setUnitIncrement(24);
        }
    }

    static void styleTable(JTable t, Color[] colColors) {
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(PANEL2);
        t.setForeground(TEXT);
        t.setSelectionBackground(new Color(64, 58, 120));
        t.setSelectionForeground(Color.WHITE);
        t.setFont(font(Font.PLAIN, 12));
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setDefaultRenderer(new HeaderR());
        t.getTableHeader().setPreferredSize(new Dimension(100, 32));
        CellR r = new CellR(colColors);
        t.setDefaultRenderer(Object.class, r);
        t.setDefaultRenderer(Integer.class, r);
        t.setDefaultRenderer(String.class, r);
    }

    static class DarkCombo extends BasicComboBoxUI {
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    aa(g);
                    g.setColor(CYAN);
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    g.fill(poly(cx - 5, cy - 2, cx + 5, cy - 2, cx, cy + 4));
                    g.dispose();
                }
            };
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            return b;
        }

        public void paintCurrentValueBackground(Graphics g, Rectangle r, boolean hasFocus) {
            g.setColor(PANEL2);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        protected ComboPopup createPopup() {
            BasicComboPopup p = new BasicComboPopup(comboBox) {
                protected JScrollPane createScroller() {
                    JScrollPane sp = super.createScroller();
                    styleScroll(sp);
                    return sp;
                }
            };
            p.getList().setBackground(PANEL2);
            p.setBorder(new LineBorder(BORDER));
            return p;
        }
    }

    // ====================================================== APPLICATION STATE
    DefaultTableModel input = new DefaultTableModel(
        new Object[]{"PID", "Arrival", "Burst", "Priority"}, 0) {
        public boolean isCellEditable(int r, int c) { return c != 0; }
        public Class<?> getColumnClass(int c) { return c == 0 ? String.class : Integer.class; }
    };

    DefaultTableModel output = new DefaultTableModel(
        new Object[]{"PID", "Arrival", "Burst", "Priority", "Start", "CT",
            "TAT", "WT", "RT"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    DefaultTableModel cmpModel = new DefaultTableModel(
        new Object[]{"Algorithm", "Avg WT", "Avg TAT", "Avg RT", "CPU %", "Ctx sw", "Total"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    JTable inTable, outTable, cmpTable;
    JComboBox<String> algorithm;
    Stepper quantum;
    JCheckBox animate;
    JTextArea desc;
    JSlider speed;
    MetricCard mWT, mTAT, mRT, mUtil, mThr, mSw;
    MetricCard wWT, wTAT, wRT, wSw;
    GanttPanel gantt = new GanttPanel();
    LiveView live = new LiveView();
    BarChart chart = new BarChart();
    MiniGantts mini = new MiniGantts();
    FlatButton playBtn, tabSim, tabCmp;
    JLabel status, statusHint;
    CardLayout cards = new CardLayout();
    JPanel views = new JPanel(cards);

    Result current;
    List<Result> cmp = new ArrayList<>();
    int cursor;
    javax.swing.Timer playTimer = new javax.swing.Timer(150, e -> onTick());

    // ====================================================== UI CONSTRUCTION
    Main() {
        super("CPU Scheduling Studio");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Rectangle scr = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1320, scr.width), Math.min(880, scr.height));
        setMinimumSize(new Dimension(Math.min(1000, scr.width), Math.min(640, scr.height)));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setPaint(new GradientPaint(0, 0, BG2, getWidth(), getHeight(), BG));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(new RadialGradientPaint(new Point2D.Float(getWidth() * 0.88f, 0), 520f,
                    new float[]{0f, 1f}, new Color[]{alpha(ACCENT, 70), alpha(ACCENT, 0)}));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(new RadialGradientPaint(new Point2D.Float(0, getHeight()), 480f,
                    new float[]{0f, 1f}, new Color[]{alpha(CYAN, 34), alpha(CYAN, 0)}));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        setContentPane(root);

        views.setOpaque(false);
        views.add(simulatorView(), "sim");
        views.add(compareView(), "cmp");

        root.add(header(), BorderLayout.NORTH);
        root.add(views, BorderLayout.CENTER);
        root.add(footer(), BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(e -> runSimulation(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        showView("sim");
        loadDemo();
        runSimulation();
    }

    JPanel header() {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(18, 26, 12, 26));

        JPanel west = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        west.setOpaque(false);
        west.add(new Logo());
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(lbl("CPU SCHEDULING STUDIO", 22, TEXT, true));
        titles.add(Box.createVerticalStrut(3));
        titles.add(lbl("Simulate, animate and compare 10 scheduling algorithms", 12, MUTED, false));
        west.add(titles);
        p.add(west, BorderLayout.WEST);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        nav.setOpaque(false);
        tabSim = new FlatButton("Simulator", ACCENT, 0, false);
        tabCmp = new FlatButton("Compare All", ACCENT, 0, false);
        tabSim.tab = true;
        tabCmp.tab = true;
        tabSim.addActionListener(e -> showView("sim"));
        tabCmp.addActionListener(e -> showView("cmp"));
        nav.add(tabSim);
        nav.add(tabCmp);
        p.add(nav, BorderLayout.CENTER);

        JLabel pill = new JLabel("JAVA SWING") {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                aa(g);
                g.setColor(PANEL2);
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
                g.setColor(CYAN);
                g.fill(new Ellipse2D.Double(12, getHeight() / 2.0 - 3, 6, 6));
                g.dispose();
                super.paintComponent(g0);
            }
        };
        pill.setFont(font(Font.BOLD, 11));
        pill.setForeground(CYAN);
        pill.setBorder(new EmptyBorder(9, 28, 9, 14));
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        east.setOpaque(false);
        east.add(pill);
        p.add(east, BorderLayout.EAST);
        return p;
    }

    void showView(String name) {
        cards.show(views, name);
        tabSim.selected = name.equals("sim");
        tabCmp.selected = name.equals("cmp");
        tabSim.repaint();
        tabCmp.repaint();
    }

    JPanel footer() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(2, 28, 12, 28));
        status = lbl("\u25CF Ready", 12, CYAN, true);
        statusHint = lbl("Ctrl+Enter to run  \u2022  click a cell to edit  \u2022  hover Gantt blocks for details",
            11, MUTED, false);
        p.add(status, BorderLayout.WEST);
        p.add(statusHint, BorderLayout.EAST);
        return p;
    }

    void setStatus(String s, Color c) {
        status.setText("\u25CF " + s);
        status.setForeground(c);
    }

    // ---------------------------------------------------- simulator tab
    JScrollPane simulatorView() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(4, 26, 4, 26));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;

        JPanel metrics = new JPanel(new GridLayout(1, 6, 12, 0));
        metrics.setOpaque(false);
        mWT = new MetricCard("AVG WAITING", AMBER, 2, "", "WT = TAT \u2212 BT");
        mTAT = new MetricCard("AVG TURNAROUND", CYAN, 2, "", "TAT = CT \u2212 AT");
        mRT = new MetricCard("AVG RESPONSE", GREEN, 2, "", "RT = first run \u2212 AT");
        mUtil = new MetricCard("CPU UTILISATION", ACCENT, 1, "%", "busy time \u00F7 total");
        mThr = new MetricCard("THROUGHPUT", ROSE, 3, "", "processes / time unit");
        mSw = new MetricCard("CONTEXT SWITCHES", BLUE, 0, "", "process \u2192 process");
        for (MetricCard m : new MetricCard[]{mWT, mTAT, mRT, mUtil, mThr, mSw}) metrics.add(m);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.weightx = 1; c.weighty = 0;
        c.insets = new Insets(0, 0, 14, 0);
        root.add(metrics, c);

        // left column
        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        GridBagConstraints lc = new GridBagConstraints();
        lc.fill = GridBagConstraints.BOTH;
        lc.gridx = 0; lc.weightx = 1;
        lc.gridy = 0; lc.weighty = 0;
        lc.insets = new Insets(0, 0, 14, 0);
        left.add(configCard(), lc);
        lc.gridy = 1; lc.weighty = 1; lc.insets = new Insets(0, 0, 0, 0);
        left.add(processCard(), lc);

        // right column
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        GridBagConstraints rc = new GridBagConstraints();
        rc.fill = GridBagConstraints.BOTH;
        rc.gridx = 0; rc.weightx = 1;
        rc.gridy = 0; rc.weighty = 0;
        rc.insets = new Insets(0, 0, 14, 0);
        right.add(ganttCard(), rc);
        rc.gridy = 1; rc.weighty = 1; rc.insets = new Insets(0, 0, 0, 0);
        right.add(resultsCard(), rc);

        c.gridy = 1; c.gridwidth = 1; c.weighty = 1; c.insets = new Insets(0, 0, 0, 14);
        c.gridx = 0; c.weightx = 0.36;
        root.add(left, c);
        c.gridx = 1; c.weightx = 0.64; c.insets = new Insets(0, 0, 0, 0);
        root.add(right, c);

        ScrollBody sb = new ScrollBody();
        sb.fillHeight = true;
        sb.setOpaque(false);
        sb.setLayout(new BorderLayout());
        sb.add(root, BorderLayout.CENTER);
        JScrollPane sp = new JScrollPane(sb);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUI(new SlimScroll());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(11, 11));
        sp.getVerticalScrollBar().setUnitIncrement(24);
        return sp;
    }

    JPanel configCard() {
        Card p = new Card("CONFIGURATION", "Pick an algorithm and run the simulation", null);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(lbl("Scheduling algorithm", 12, TEXT, true));
        form.add(Box.createVerticalStrut(7));
        algorithm = new JComboBox<>(ALGOS);
        algorithm.setUI(new DarkCombo());
        algorithm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        algorithm.setPreferredSize(new Dimension(200, 38));
        algorithm.setAlignmentX(Component.LEFT_ALIGNMENT);
        algorithm.setFont(font(Font.BOLD, 13));
        algorithm.setBackground(PANEL2);
        algorithm.setForeground(TEXT);
        algorithm.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(0, 8, 0, 0)));
        algorithm.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean f) {
                super.getListCellRendererComponent(l, v, i, sel, f);
                setBorder(new EmptyBorder(7, 10, 7, 10));
                setBackground(sel ? new Color(64, 58, 120) : PANEL2);
                setForeground(TEXT);
                setFont(font(Font.PLAIN, 13));
                return this;
            }
        });
        form.add(algorithm);
        form.add(Box.createVerticalStrut(10));

        desc = new JTextArea(5, 20);
        desc.setEditable(false);
        desc.setFocusable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setOpaque(false);
        desc.setForeground(MUTED);
        desc.setFont(font(Font.PLAIN, 12));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        desc.setBorder(new EmptyBorder(0, 2, 0, 2));
        form.add(desc);
        form.add(Box.createVerticalStrut(12));

        form.add(lbl("Time quantum  (Round Robin / MLFQ base)", 12, TEXT, true));
        form.add(Box.createVerticalStrut(7));
        quantum = new Stepper(2, 1, 50);
        form.add(quantum);
        form.add(Box.createVerticalStrut(8));
        form.add(lbl("Priority: lower number = higher priority", 11, MUTED, false));
        form.add(Box.createVerticalStrut(8));

        animate = new JCheckBox("Animate the Gantt chart on run", true);
        animate.setOpaque(false);
        animate.setForeground(TEXT);
        animate.setFont(font(Font.PLAIN, 12));
        animate.setFocusPainted(false);
        animate.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(animate);

        algorithm.addActionListener(e -> {
            String a = (String) algorithm.getSelectedItem();
            desc.setText(DESC.get(a));
            quantum.setEnabled(A_RR.equals(a) || A_MLFQ.equals(a));
        });
        algorithm.setSelectedIndex(2);
        algorithm.setSelectedIndex(0);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        FlatButton run = new FlatButton("Run Simulation", ACCENT, 1, true);
        FlatButton reset = new FlatButton("Reset Results", PANEL2, 4, false);
        run.addActionListener(e -> runSimulation());
        reset.addActionListener(e -> resetResults());
        buttons.add(run);
        buttons.add(reset);

        p.add(form, BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    JPanel processCard() {
        Card p = new Card("PROCESSES", "Click a cell to edit arrival, burst and priority", null);

        inTable = new JTable(input);
        styleTable(inTable, null);
        inTable.setName("in");
        inTable.getColumnModel().getColumn(1).setCellEditor(new IntEditor(0));
        inTable.getColumnModel().getColumn(2).setCellEditor(new IntEditor(1));
        inTable.getColumnModel().getColumn(3).setCellEditor(new IntEditor(0));
        inTable.setSurrendersFocusOnKeystroke(true);
        JScrollPane sc = new JScrollPane(inTable);
        styleScroll(sc);
        sc.setPreferredSize(new Dimension(300, 230));
        p.add(sc, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 5, 6, 0));
        buttons.setOpaque(false);
        FlatButton add = new FlatButton("+ Add", ACCENT, 0, true);
        FlatButton rem = new FlatButton("Remove", PANEL2, 0, false);
        FlatButton rnd = new FlatButton("Random", PANEL2, 0, false);
        FlatButton demo = new FlatButton("Demo", PANEL2, 0, false);
        FlatButton clr = new FlatButton("Clear", PANEL2, 0, false);
        for (FlatButton b : new FlatButton[]{add, rem, rnd, demo, clr}) b.setFont(font(Font.BOLD, 11));
        add.addActionListener(e -> addProcess());
        rem.addActionListener(e -> removeProcess());
        rnd.addActionListener(e -> { randomProcesses(); runSimulation(); });
        demo.addActionListener(e -> { loadDemo(); runSimulation(); });
        clr.addActionListener(e -> clearAll());
        for (FlatButton b : new FlatButton[]{add, rem, rnd, demo, clr}) buttons.add(b);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(2, 0, 0, 0));
        south.add(buttons);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    JPanel ganttCard() {
        Card p = new Card("GANTT CHART", "Execution timeline \u2022 triangles mark arrivals", null);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(live, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(gantt);
        sc.setBorder(new LineBorder(BORDER, 1, true));
        sc.getViewport().setBackground(alpha(Color.BLACK, 0));
        sc.setBackground(PANEL);
        sc.getViewport().setBackground(new Color(17, 21, 37));
        sc.getVerticalScrollBar().setUI(new SlimScroll());
        sc.getHorizontalScrollBar().setUI(new SlimScroll());
        sc.getHorizontalScrollBar().setPreferredSize(new Dimension(11, 11));
        sc.getHorizontalScrollBar().setUnitIncrement(28);
        sc.setPreferredSize(new Dimension(300, 190));
        center.add(sc, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);

        JPanel ctl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ctl.setOpaque(false);
        playBtn = new FlatButton("Play", ACCENT, 1, true);
        FlatButton stop = new FlatButton("Stop", PANEL2, 3, false);
        FlatButton restart = new FlatButton("Restart", PANEL2, 4, false);
        FlatButton end = new FlatButton("Skip to end", PANEL2, 5, false);
        playBtn.addActionListener(e -> togglePlay());
        stop.addActionListener(e -> {
            if (current == null) return;
            pausePlay();
            cursor = Math.min(current.total, cursor + 1);
            syncView();
        });
        restart.addActionListener(e -> {
            if (current == null) return;
            pausePlay();
            cursor = 0;
            syncView();
        });
        end.addActionListener(e -> {
            if (current == null) return;
            pausePlay();
            cursor = current.total;
            syncView();
        });
        ctl.add(playBtn);
        ctl.add(stop);
        ctl.add(restart);
        ctl.add(end);
        ctl.add(Box.createHorizontalStrut(10));
        ctl.add(lbl("Speed", 11, MUTED, false));
        speed = new JSlider(1, 10, 4);
        speed.setOpaque(false);
        speed.setPreferredSize(new Dimension(120, 26));
        speed.addChangeListener(e -> playTimer.setDelay(Math.max(25, 660 / speed.getValue())));
        ctl.add(speed);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(ctl, BorderLayout.WEST);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    JPanel resultsCard() {
        FlatButton export = new FlatButton("Export CSV", PANEL2, 0, false);
        export.setFont(font(Font.BOLD, 11));
        export.addActionListener(e -> exportCsv());
        Card p = new Card("PROCESS RESULTS", "CT = completion  \u2022  TAT = CT \u2212 AT  \u2022  WT = TAT \u2212 BT  \u2022  RT = start \u2212 AT", export);

        outTable = new JTable(output);
        Color[] cc = new Color[9];
        cc[6] = CYAN;
        cc[7] = AMBER;
        cc[8] = GREEN;
        styleTable(outTable, cc);
        JScrollPane sc = new JScrollPane(outTable);
        styleScroll(sc);
        sc.setPreferredSize(new Dimension(300, 210));
        p.add(sc, BorderLayout.CENTER);
        return p;
    }

    // ---------------------------------------------------- compare tab
    JScrollPane compareView() {
        ScrollBody body = new ScrollBody();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(4, 26, 8, 26));

        JPanel win = new JPanel(new GridLayout(1, 4, 12, 0));
        win.setOpaque(false);
        wWT = new MetricCard("BEST WAITING TIME", AMBER, 2, "", "\u2014");
        wTAT = new MetricCard("BEST TURNAROUND", CYAN, 2, "", "\u2014");
        wRT = new MetricCard("BEST RESPONSE TIME", GREEN, 2, "", "\u2014");
        wSw = new MetricCard("FEWEST CONTEXT SWITCHES", BLUE, 0, "", "\u2014");
        for (MetricCard m : new MetricCard[]{wWT, wTAT, wRT, wSw}) {
            m.setText("\u2014", "Run a simulation first");
            win.add(m);
        }
        win.setAlignmentX(Component.LEFT_ALIGNMENT);
        win.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        body.add(win);
        body.add(Box.createVerticalStrut(14));

        JPanel mid = new JPanel(new GridLayout(1, 2, 14, 0));
        mid.setOpaque(false);
        mid.setAlignmentX(Component.LEFT_ALIGNMENT);
        Card chartCard = new Card("METRIC COMPARISON",
            "Average waiting, turnaround and response time \u2014 lower is better, white outline = winner", null);
        chartCard.add(chart, BorderLayout.CENTER);

        Card tableCard = new Card("LEADERBOARD", "Double-click a row to open that algorithm in the simulator", null);
        cmpTable = new JTable(cmpModel);
        cmpTable.setName("cmp");
        styleTable(cmpTable, null);
        cmpTable.setDefaultRenderer(Object.class, new CmpR());
        cmpTable.setDefaultRenderer(Integer.class, new CmpR());
        cmpTable.setDefaultRenderer(String.class, new CmpR());
        cmpTable.setRowHeight(34);
        cmpTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        cmpTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && cmpTable.getSelectedRow() >= 0) {
                    algorithm.setSelectedItem(ALGOS[cmpTable.getSelectedRow()]);
                    showView("sim");
                    runSimulation();
                }
            }
        });
        JScrollPane tsc = new JScrollPane(cmpTable);
        styleScroll(tsc);
        tableCard.add(tsc, BorderLayout.CENTER);
        mid.add(chartCard);
        mid.add(tableCard);
        mid.setPreferredSize(new Dimension(600, 440));
        mid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 440));
        body.add(mid);
        body.add(Box.createVerticalStrut(14));

        Card miniCard = new Card("TIMELINES SIDE BY SIDE",
            "Every algorithm on the same time scale \u2014 shorter strip = finishes sooner", null);
        miniCard.add(mini, BorderLayout.CENTER);
        miniCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(miniCard);

        JScrollPane sp = new JScrollPane(body);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUI(new SlimScroll());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(11, 11));
        sp.getVerticalScrollBar().setUnitIncrement(20);
        return sp;
    }

    // ====================================================== ACTIONS
    void addProcess() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < input.getRowCount(); i++) ids.add(String.valueOf(input.getValueAt(i, 0)));
        int n = 1;
        while (ids.contains("P" + n)) n++;
        input.addRow(new Object[]{"P" + n, 0, 1, 1});
        int last = input.getRowCount() - 1;
        inTable.setRowSelectionInterval(last, last);
        inTable.scrollRectToVisible(inTable.getCellRect(last, 0, true));
        setStatus("Process P" + n + " added \u2014 edit its values, then run", CYAN);
    }

    void removeProcess() {
        int r = inTable.getSelectedRow();
        if (r < 0) {
            setStatus("Select a process row to remove it", ROSE);
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        if (inTable.isEditing()) inTable.getCellEditor().cancelCellEditing();
        input.removeRow(r);
        setStatus("Process removed", CYAN);
    }

    void loadDemo() {
        if (inTable != null && inTable.isEditing()) inTable.getCellEditor().cancelCellEditing();
        input.setRowCount(0);
        int[][] d = {{0, 5, 2}, {1, 3, 1}, {2, 8, 4}, {3, 6, 3}, {4, 2, 5}, {6, 4, 2}};
        for (int i = 0; i < d.length; i++)
            input.addRow(new Object[]{"P" + (i + 1), d[i][0], d[i][1], d[i][2]});
    }

    void randomProcesses() {
        if (inTable.isEditing()) inTable.getCellEditor().cancelCellEditing();
        Random rnd = new Random();
        int n = 4 + rnd.nextInt(4);
        input.setRowCount(0);
        for (int i = 0; i < n; i++)
            input.addRow(new Object[]{"P" + (i + 1), rnd.nextInt(10), 1 + rnd.nextInt(9), 1 + rnd.nextInt(5)});
    }

    void clearAll() {
        if (inTable.isEditing()) inTable.getCellEditor().cancelCellEditing();
        input.setRowCount(0);
        resetResults();
        setStatus("All processes cleared", CYAN);
    }

    void resetResults() {
        pausePlay();
        current = null;
        cursor = 0;
        output.setRowCount(0);
        gantt.setResult(null);
        live.setState(null, 0);
        mWT.set(0); mTAT.set(0); mRT.set(0); mUtil.set(0); mThr.set(0); mSw.set(0);
        setStatus("Results reset", CYAN);
    }

    List<Proc> readInput() {
        if (inTable.isEditing() && !inTable.getCellEditor().stopCellEditing())
            throw new IllegalArgumentException("Fix the highlighted cell before running.");
        if (input.getRowCount() == 0)
            throw new IllegalArgumentException("Add at least one process first.");
        List<Proc> p = new ArrayList<>();
        for (int r = 0; r < input.getRowCount(); r++) {
            String id = String.valueOf(input.getValueAt(r, 0));
            int at = ((Number) input.getValueAt(r, 1)).intValue();
            int bt = ((Number) input.getValueAt(r, 2)).intValue();
            int pr = ((Number) input.getValueAt(r, 3)).intValue();
            if (at < 0 || pr < 0)
                throw new IllegalArgumentException("Arrival and priority must be non-negative (" + id + ").");
            if (bt < 1)
                throw new IllegalArgumentException("Burst time must be at least 1 (" + id + ").");
            p.add(new Proc(id, at, bt, pr, r));
        }
        return p;
    }

    void runSimulation() {
        try {
            List<Proc> in = readInput();
            String a = (String) algorithm.getSelectedItem();
            int q = quantum.value;
            Result r = simulate(in, a, q);
            showResults(r);
            compareAll(in, q);
            setStatus("Simulation complete \u2014 " + a
                + (A_RR.equals(a) || A_MLFQ.equals(a) ? "  (quantum " + q + ")" : ""), GREEN);
        } catch (IllegalArgumentException | ClassCastException ex) {
            String m = ex.getMessage() == null || ex instanceof ClassCastException
                ? "Invalid input." : ex.getMessage();
            setStatus(m, ROSE);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    void showResults(Result r) {
        pausePlay();
        current = r;
        output.setRowCount(0);
        for (Proc p : r.procs)
            output.addRow(new Object[]{p.id, p.at, p.bt, p.pr, p.start, p.ct, p.tat, p.wt, p.rt});
        mWT.set(r.avgWT);
        mTAT.set(r.avgTAT);
        mRT.set(r.avgRT);
        mUtil.set(r.util);
        mThr.set(r.thr);
        mSw.set(r.switches);
        gantt.setResult(r);
        cursor = animate.isSelected() ? 0 : r.total;
        syncView();
        if (animate.isSelected()) startPlay();
    }

    void compareAll(List<Proc> in, int q) {
        List<Result> rs = new ArrayList<>();
        for (String a : ALGOS) rs.add(simulate(in, a, q));
        cmp = rs;
        chart.set(rs);
        mini.set(rs);

        double[] b = {0, 1e18, 1e18, 1e18, -1, 1e18, 1e18};
        for (Result r : rs) {
            b[1] = Math.min(b[1], Math.round(r.avgWT * 100) / 100.0);
            b[2] = Math.min(b[2], Math.round(r.avgTAT * 100) / 100.0);
            b[3] = Math.min(b[3], Math.round(r.avgRT * 100) / 100.0);
            b[4] = Math.max(b[4], Math.round(r.util * 10) / 10.0);
            b[5] = Math.min(b[5], r.switches);
            b[6] = Math.min(b[6], r.total);
        }
        System.arraycopy(b, 0, BEST, 0, 7);

        cmpModel.setRowCount(0);
        for (Result r : rs)
            cmpModel.addRow(new Object[]{shortName(r.algo), f2(r.avgWT), f2(r.avgTAT), f2(r.avgRT),
                String.format(Locale.US, "%.1f%%", r.util), String.valueOf(r.switches), String.valueOf(r.total)});

        winner(wWT, rs, 0);
        winner(wTAT, rs, 1);
        winner(wRT, rs, 2);
        winner(wSw, rs, 3);
    }

    void winner(MetricCard card, List<Result> rs, int metric) {
        double best = Double.MAX_VALUE;
        double[] v = new double[rs.size()];
        for (int i = 0; i < v.length; i++) {
            Result r = rs.get(i);
            v[i] = metric == 0 ? r.avgWT : metric == 1 ? r.avgTAT : metric == 2 ? r.avgRT : r.switches;
            v[i] = Math.round(v[i] * 100) / 100.0;
            best = Math.min(best, v[i]);
        }
        List<String> names = new ArrayList<>();
        for (int i = 0; i < v.length; i++)
            if (Math.abs(v[i] - best) < 1e-9) names.add(shortName(rs.get(i).algo));
        String title = names.size() == 1 ? names.get(0)
            : names.size() == 2 ? names.get(0) + " / " + names.get(1)
            : names.get(0) + " +" + (names.size() - 1) + " tied";
        card.setText(title, (metric == 3 ? String.valueOf((int) best) : f2(best))
            + (metric == 3 ? " switches" : " time units"));
    }

    // ---------------------------------------------------- playback
    void startPlay() {
        playTimer.setDelay(Math.max(25, 660 / speed.getValue()));
        playTimer.start();
        playBtn.setText("Pause");
        playBtn.glyph = 2;
        playBtn.repaint();
    }

    void pausePlay() {
        playTimer.stop();
        if (playBtn != null) {
            playBtn.setText("Play");
            playBtn.glyph = 1;
            playBtn.repaint();
        }
    }

    void togglePlay() {
        if (current == null) return;
        if (playTimer.isRunning()) {
            pausePlay();
        } else {
            if (cursor >= current.total) cursor = 0;
            syncView();
            startPlay();
        }
    }

    void onTick() {
        if (current == null) {
            pausePlay();
            return;
        }
        if (cursor < current.total) cursor++;
        syncView();
        if (cursor >= current.total) pausePlay();
    }

    void syncView() {
        gantt.setCursor(cursor);
        live.setState(current, cursor);
        if (playTimer.isRunning()) gantt.ensureCursorVisible();
    }

    void exportCsv() {
        if (output.getRowCount() == 0) {
            setStatus("Nothing to export \u2014 run a simulation first", ROSE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("schedule_results.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter w = new PrintWriter(fc.getSelectedFile(), "UTF-8")) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < output.getColumnCount(); c++)
                sb.append(c > 0 ? "," : "").append(output.getColumnName(c));
            w.println(sb);
            for (int r = 0; r < output.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < output.getColumnCount(); c++)
                    sb.append(c > 0 ? "," : "").append(output.getValueAt(r, c));
                w.println(sb);
            }
            w.println();
            w.println("Algorithm," + current.algo);
            w.println("Average waiting," + f2(current.avgWT));
            w.println("Average turnaround," + f2(current.avgTAT));
            w.println("Average response," + f2(current.avgRT));
            w.println("CPU utilisation %," + f2(current.util));
            setStatus("Exported to " + fc.getSelectedFile().getName(), GREEN);
        } catch (IOException ex) {
            setStatus("Export failed: " + ex.getMessage(), ROSE);
        }
    }

    // ====================================================== MAIN
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            UIManager.put("ToolTip.background", new Color(36, 44, 72));
            UIManager.put("ToolTip.foreground", TEXT);
            UIManager.put("ToolTip.border", new LineBorder(BORDER));
            UIManager.put("ToolTip.font", font(Font.PLAIN, 12));
            new Main().setVisible(true);
        });
    }
}