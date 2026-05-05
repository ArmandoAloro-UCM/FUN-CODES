// AnimView.java — combined animation screen
// Left  — Gantt Chart     : fixed-width, scrollable, colour-coded segments.
// Right — Process Timeline: stacked blocks for active processes; finished
//                           processes drop off so the stack height varies.
// Both sides are driven by a shared frame loop using javax.swing.Timer.

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class AnimView extends BaseView {

    // ── Animation state ───────────────────────────────────────────────────────
    private boolean running   = false;
    private int     tick      = 0;     // current logical clock tick (0 … totalTime)
    private int     sub       = 0;     // sub-step within the current tick
    private int     msCount   = 0;     // real elapsed ms shown in the timer label

    // Pre-computed tick-by-tick state (filled in by prepare())
    private List<Map<String, Object>> tickData;
    private int                       totalTime;
    private Map<String, String>       colorMap;
    private Map<String, Integer>      burstMap;
    private List<String>              pidList;

    // Gantt drawing state
    private final List<Object[]>      gSegs = new ArrayList<>(); // closed segments
    private Object[]                  gCur  = null;              // [pid, color, t_start]

    // Swing timers
    private javax.swing.Timer frameTimer;
    private javax.swing.Timer msTimer;

    // UI components
    private JLabel  statusLabel;
    private JLabel  timerLabel;
    private JButton animBtn;
    private GanttCanvas  ganttCanvas;
    private TimelineCanvas timelineCanvas;

    public AnimView(AppController ctrl) {
        super(ctrl);
        buildUI();
        prepare();
    }

    // ── UI Construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));

        // Header
        add(Widgets.makeHeader(
            "\u25c9  ANIMATION \u2014 GANTT CHART  &  PROCESS TIMELINE",
            "  Both run simultaneously  \u2022  msec timer top-right"),
            BorderLayout.NORTH);

        // Top info / status bar
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.PANEL);
        top.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        statusLabel = new JLabel("Press  \u25b6 ANIMATE  to begin");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusLabel.setForeground(Theme.WARNING);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 16, 5, 8));
        top.add(statusLabel, BorderLayout.WEST);

        timerLabel = new JLabel("\u23f1  0 ms");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        timerLabel.setForeground(Theme.TEXT);
        timerLabel.setBackground(Color.decode("#dde4f7"));
        timerLabel.setOpaque(true);
        timerLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(2, 10, 2, 10)
        ));
        top.add(timerLabel, BorderLayout.EAST);
        add(top, BorderLayout.AFTER_LAST_LINE); // will be replaced below

        // Re-add in correct order using a combined north panel
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(Widgets.makeHeader(
            "\u25c9  ANIMATION \u2014 GANTT CHART  &  PROCESS TIMELINE",
            "  Both run simultaneously  \u2022  msec timer top-right"), BorderLayout.NORTH);
        northPanel.add(top, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // Main split: LEFT = Gantt, RIGHT = Process Timeline
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(Theme.WIN_W / 2);
        split.setResizeWeight(0.5);
        split.setBorder(BorderFactory.createEmptyBorder(6, 12, 0, 12));
        split.setBackground(Theme.BG);

        // LEFT — Gantt chart card
        JPanel leftCard = Widgets.makeCard();
        leftCard.setLayout(new BorderLayout());
        JLabel ganttTitle = new JLabel("  GANTT CHART");
        ganttTitle.setFont(Theme.FONT_LABEL);
        ganttTitle.setForeground(Theme.ACCENT);
        leftCard.add(ganttTitle, BorderLayout.NORTH);

        ganttCanvas = new GanttCanvas();
        JScrollPane ganttScroll = new JScrollPane(ganttCanvas,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        ganttScroll.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        leftCard.add(ganttScroll, BorderLayout.CENTER);
        split.setLeftComponent(leftCard);

        // RIGHT — Process Timeline card
        JPanel rightCard = Widgets.makeCard();
        rightCard.setLayout(new BorderLayout());
        JLabel timelineTitle = new JLabel("  PROCESS TIMELINE  (Concurrency Diagram)");
        timelineTitle.setFont(Theme.FONT_LABEL);
        timelineTitle.setForeground(Theme.ACCENT2);
        rightCard.add(timelineTitle, BorderLayout.NORTH);

        timelineCanvas = new TimelineCanvas();
        rightCard.add(timelineCanvas, BorderLayout.CENTER);
        split.setRightComponent(rightCard);

        add(split, BorderLayout.CENTER);

        // Legend strip
        JPanel leg = Widgets.makeCard();
        leg.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 4));
        leg.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        JLabel legTitle = new JLabel("  LEGEND:");
        legTitle.setFont(Theme.FONT_LABEL);
        legTitle.setForeground(Theme.SUBTEXT);
        leg.add(legTitle);

        for (Process p : ctrl.processes) {
            JPanel dot = new JPanel();
            dot.setBackground(Color.decode(p.color));
            dot.setPreferredSize(new Dimension(14, 14));
            dot.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 3));
            leg.add(dot);

            JLabel pidLbl = new JLabel(p.pid);
            pidLbl.setFont(Theme.FONT_LABEL);
            pidLbl.setForeground(Color.decode(p.color));
            leg.add(pidLbl);
        }

        // Control buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 5));
        btnRow.setBackground(Theme.BG);

        btnRow.add(Widgets.makeBtn("\u25c4  BACK", e -> goBack(),
                                   Color.decode("#e2e8f0"), Theme.TEXT));
        animBtn = Widgets.makeBtn("\u25b6  ANIMATE", e -> startAnimation(),
                                  Theme.SUCCESS, Color.WHITE);
        btnRow.add(animBtn);
        btnRow.add(Widgets.makeBtn("RESULTS \u2192", e -> ctrl.showResults(),
                                   Theme.ACCENT, Color.WHITE));

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(Theme.BG);
        south.add(leg,    BorderLayout.NORTH);
        south.add(btnRow, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    // ── Data Preparation ──────────────────────────────────────────────────────

    /**
     * Pre-computes the tick-by-tick SRTF execution state so the animation
     * loop only needs to index into a list rather than re-running the algorithm.
     */
    private void prepare() {
        List<Process> procs = ctrl.processes;
        pidList  = new ArrayList<>();
        colorMap = new LinkedHashMap<>();
        burstMap = new LinkedHashMap<>();
        Map<String, Integer> arrivalMap = new LinkedHashMap<>();
        Map<String, Integer> remain     = new LinkedHashMap<>();

        int sumBurst  = 0;
        int maxArriv  = 0;
        for (Process p : procs) {
            pidList.add(p.pid);
            colorMap.put(p.pid,   p.color);
            burstMap.put(p.pid,   p.burst);
            arrivalMap.put(p.pid, p.arrival);
            remain.put(p.pid,     p.burst);
            sumBurst += p.burst;
            if (p.arrival > maxArriv) maxArriv = p.arrival;
        }

        int n     = procs.size();
        int t     = 0;
        int done  = 0;
        int maxT  = sumBurst + maxArriv + 2;
        tickData  = new ArrayList<>();
        Set<String> finishedSet = new HashSet<>();

        while (done < n && t < maxT) {
            List<String> ready = new ArrayList<>();
            for (String pid : pidList) {
                if (arrivalMap.get(pid) <= t && remain.get(pid) > 0) {
                    ready.add(pid);
                }
            }

            if (ready.isEmpty()) {
                List<String> active = new ArrayList<>();
                for (String pid : pidList) {
                    if (arrivalMap.get(pid) <= t && !finishedSet.contains(pid)) {
                        active.add(pid);
                    }
                }
                Map<String, Object> entry = new HashMap<>();
                entry.put("running",  null);
                entry.put("finished", null);
                entry.put("active",   active);
                tickData.add(entry);
                t++;
                continue;
            }

            // SRTF selection
            String chosen = ready.stream()
                .min(Comparator.comparingInt(remain::get))
                .orElseThrow();
            remain.put(chosen, remain.get(chosen) - 1);

            String finished = null;
            if (remain.get(chosen) == 0) {
                finished = chosen;
                finishedSet.add(chosen);
                done++;
            }

            List<String> active = new ArrayList<>();
            for (String pid : pidList) {
                if (arrivalMap.get(pid) <= t) {
                    Set<String> excl = new HashSet<>(finishedSet);
                    if (finished != null) excl.remove(finished);
                    if (!excl.contains(pid)) active.add(pid);
                }
            }

            Map<String, Object> entry = new HashMap<>();
            entry.put("running",  chosen);
            entry.put("finished", finished);
            entry.put("active",   active);
            tickData.add(entry);
            t++;
        }

        totalTime = tickData.size();
        ganttCanvas.setPreferredSize(new Dimension(
            40 + totalTime * Theme.PX_PER_TICK + 20, 110));
    }

    // ── Animation Control ─────────────────────────────────────────────────────

    /** Stops the animation and returns to the input summary table. */
    private void goBack() {
        stopAll();
        ctrl.showTable();
    }

    /** Cancels all running timers. */
    private void stopAll() {
        running = false;
        if (frameTimer != null) frameTimer.stop();
        if (msTimer    != null) msTimer.stop();
    }

    /** Starts (or replays) the animation from tick 0. */
    private void startAnimation() {
        if (running) return;
        stopAll();

        tick     = 0;
        sub      = 0;
        msCount  = 0;
        gSegs.clear();
        gCur     = null;
        timerLabel.setText("\u23f1  0 ms");
        ganttCanvas.repaint();
        timelineCanvas.repaint();

        running = true;
        animBtn.setEnabled(false);
        animBtn.setText("ANIMATING\u2026");
        statusLabel.setText("Running\u2026");

        // Real-time ms counter — increments by 100 every 100 real ms
        msTimer = new javax.swing.Timer(100, e -> {
            msCount += 100;
            timerLabel.setText("\u23f1  " + Math.min(msCount, 9999) + " ms");
        });
        msTimer.start();

        // Frame loop
        frameTimer = new javax.swing.Timer(Theme.FRAME_MS, e -> doFrame());
        frameTimer.start();
    }

    // ── Frame Loop ────────────────────────────────────────────────────────────

    /** Called every FRAME_MS by the frameTimer. Advances animation state and repaints. */
    private void doFrame() {
        if (!running) return;

        // On the first sub-step of each tick: advance logical state
        if (sub == 0) {
            if (tick >= totalTime) {
                finish();
                return;
            }

            Map<String, Object> info    = tickData.get(tick);
            String              running_pid = (String) info.get("running");
            String              finished    = (String) info.get("finished");

            // Update Gantt segment list
            if (running_pid != null) {
                if (gCur != null && gCur[0].equals(running_pid)) {
                    // Same process continues — drawing will handle it
                } else {
                    if (gCur != null) {
                        gSegs.add(new Object[]{ gCur[0], gCur[1], (int) gCur[2], tick });
                    }
                    gCur = new Object[]{ running_pid, colorMap.get(running_pid), tick };
                }
            } else {
                // CPU idle — seal any open segment
                if (gCur != null) {
                    gSegs.add(new Object[]{ gCur[0], gCur[1], (int) gCur[2], tick });
                    gCur = null;
                }
            }

            // Update status label
            StringBuilder msg = new StringBuilder("t = " + tick);
            if (running_pid != null) msg.append("   Running: ").append(running_pid);
            if (finished    != null) msg.append("   \u2714 ").append(finished).append(" finished");
            statusLabel.setText(msg.toString());

            tick++;
        }

        // Repaint both canvases on every frame
        ganttCanvas.repaint();
        timelineCanvas.repaint();

        sub = (sub + 1) % Theme.SUBSTEPS;
    }

    /** Called once all ticks are processed — seals the final segment and updates UI. */
    private void finish() {
        running = false;
        if (msTimer    != null) msTimer.stop();

        if (gCur != null) {
            gSegs.add(new Object[]{ gCur[0], gCur[1], (int) gCur[2], totalTime });
            gCur = null;
        }

        ganttCanvas.repaint();
        timelineCanvas.repaint();

        statusLabel.setText(
            "\u2714  Animation complete!  \u2192  Click  'RESULTS \u2192'  to see solutions.");
        animBtn.setEnabled(true);
        animBtn.setText("\u27f3  REPLAY");
        // Remove old listeners and add a fresh one for replay
        for (ActionListener al : animBtn.getActionListeners()) {
            animBtn.removeActionListener(al);
        }
        animBtn.addActionListener(e -> startAnimation());
    }

    // ── Inner Canvas: Gantt Chart ─────────────────────────────────────────────

    private class GanttCanvas extends JPanel {

        GanttCanvas() {
            setBackground(Color.decode("#f8faff"));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int ch = Math.max(getHeight(), 80);
            int MARGIN_L = 40;
            int MARGIN_R = 20;
            int BAR_Y0   = 18;
            int BAR_Y1   = Math.max(BAR_Y0 + 30, ch - 36);

            int total   = Math.max(totalTime, 1);
            int chartW  = MARGIN_L + total * Theme.PX_PER_TICK + MARGIN_R;
            setPreferredSize(new Dimension(chartW, 110));

            // Time axis line
            g2.setColor(Theme.BORDER);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(MARGIN_L - 2, BAR_Y1 + 2, chartW - MARGIN_R, BAR_Y1 + 2);

            // "CPU" label on the left
            g2.setFont(new Font("Courier New", Font.BOLD, 8));
            g2.setColor(Theme.SUBTEXT);
            g2.drawString("CPU", 2, (BAR_Y0 + BAR_Y1) / 2 + 4);

            // Draw all completed (sealed) segments
            for (Object[] s : gSegs) {
                drawSeg(g2, (String) s[0], (String) s[1],
                        (int) s[2], (double)(int) s[3], BAR_Y0, BAR_Y1, false);
            }

            // Draw the currently growing segment with fractional right edge
            if (gCur != null) {
                double teFrac = (tick - 1) + ((double) sub / Theme.SUBSTEPS);
                drawSeg(g2, (String) gCur[0], (String) gCur[1],
                        (int) gCur[2], teFrac, BAR_Y0, BAR_Y1, true);
            }

            // Final end-time timestamp once animation is complete
            if (tick >= totalTime && gCur == null) {
                int xEnd = MARGIN_L + totalTime * Theme.PX_PER_TICK;
                g2.setFont(new Font("Courier New", Font.PLAIN, 7));
                g2.setColor(Theme.SUBTEXT);
                g2.drawString(String.valueOf(totalTime), xEnd, BAR_Y1 + 22);
            }
        }

        /** Draws one Gantt bar segment from ts to te (te may be fractional). */
        private void drawSeg(Graphics2D g2, String pid, String color,
                             int ts, double te, int barY0, int barY1, boolean active) {
            int MARGIN_L = 40;
            int x0 = MARGIN_L + (int)(ts * Theme.PX_PER_TICK);
            int x1 = MARGIN_L + (int)(te * Theme.PX_PER_TICK);
            if (x1 - x0 < 1) x1 = x0 + 1;

            g2.setColor(Color.decode(color));
            g2.fillRect(x0, barY0, x1 - x0, barY1 - barY0);

            Color border = active ? Theme.ACCENT : Color.WHITE;
            int   bw     = active ? 2 : 1;
            g2.setColor(border);
            g2.setStroke(new BasicStroke(bw));
            g2.drawRect(x0, barY0, x1 - x0, barY1 - barY0);

            // PID label — only if the segment is wide enough
            if (x1 - x0 > 18) {
                Color fg = Widgets.isDark(color) ? Color.WHITE : Theme.TEXT;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (x0 + x1) / 2 - fm.stringWidth(pid) / 2;
                int ty = (barY0 + barY1) / 2 + fm.getAscent() / 2 - 2;
                g2.drawString(pid, tx, ty);
            }

            // Timestamp at the left edge
            g2.setFont(new Font("Courier New", Font.PLAIN, 7));
            g2.setColor(Theme.SUBTEXT);
            g2.drawString(String.valueOf(ts), x0, barY1 + 22);
        }
    }

    // ── Inner Canvas: Process Timeline ────────────────────────────────────────

    private class TimelineCanvas extends JPanel {

        TimelineCanvas() {
            setBackground(Color.decode("#f8faff"));
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int cw = Math.max(getWidth(),  80);
            int ch = Math.max(getHeight(), 40);

            int curT = tick - 1;
            if (curT < 0 || curT >= tickData.size()) {
                // Nothing to show yet
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(Theme.SUBTEXT);
                g2.drawString("\u2014", cw / 2 - 4, ch / 2);
                return;
            }

            Map<String, Object> info    = tickData.get(curT);
            String              runPid  = (String) info.get("running");
            List<String>        active  = (List<String>) info.get("active");

            int PAD      = 18;
            int AXIS_H   = 26;
            int GAP      = 4;
            int MIN_BH   = 28;
            int BORDER_W = 3;

            int stackTop    = PAD + AXIS_H;
            int stackBottom = ch - PAD;
            int stackH      = Math.max(1, stackBottom - stackTop);

            // Time-axis label
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(Theme.SUBTEXT);
            g2.drawString("t = " + curT, PAD, PAD + AXIS_H / 2 + 4);

            // Idle state
            if (active == null || active.isEmpty()) {
                g2.setColor(Color.decode("#f1f5f9"));
                g2.fillRect(PAD, stackTop, cw - 2 * PAD, stackBottom - stackTop);
                g2.setColor(Theme.BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.drawRect(PAD, stackTop, cw - 2 * PAD, stackBottom - stackTop);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(Theme.SUBTEXT);
                String idle = "Idle / Waiting";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(idle,
                    (cw - fm.stringWidth(idle)) / 2,
                    (stackTop + stackBottom) / 2 + fm.getAscent() / 2 - 2);
                return;
            }

            int n         = active.size();
            int totalGap  = GAP * (n - 1);
            int blockH    = Math.max(MIN_BH, (stackH - totalGap) / n);

            List<String> reversed = new ArrayList<>(active);
            Collections.reverse(reversed);

            for (int i = 0; i < reversed.size(); i++) {
                String pid       = reversed.get(i);
                String color     = colorMap.get(pid);
                boolean isRun    = pid.equals(runPid);

                int blockTop = stackBottom - (i + 1) * blockH - i * GAP;
                int blockBot = blockTop + blockH;

                g2.setColor(Color.decode(color));
                g2.fillRect(PAD, blockTop, cw - 2 * PAD, blockH);

                Color border = isRun ? Theme.ACCENT : Color.WHITE;
                g2.setColor(border);
                g2.setStroke(new BasicStroke(isRun ? BORDER_W : 1));
                g2.drawRect(PAD, blockTop, cw - 2 * PAD, blockH);

                // Centred PID label
                String label = pid + (isRun ? "  \u25c0 CPU" : "");
                Color  fg    = Widgets.isDark(color) ? Color.WHITE : Theme.TEXT;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (cw - fm.stringWidth(label)) / 2;
                int ty = (blockTop + blockBot) / 2 + fm.getAscent() / 2 - 2;
                g2.drawString(label, tx, ty);
            }
        }
    }
}
