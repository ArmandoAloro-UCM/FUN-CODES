// ResultsView.java — scrollable step-by-step solutions screen
// Shows Waiting Time, Completion Time, Turnaround Time with full
// per-process formulas, average calculations, and a final summary table.

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class ResultsView extends BaseView {

    private JPanel content;   // scrollable inner panel

    public ResultsView(AppController ctrl) {
        super(ctrl);
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // Header
        add(Widgets.makeHeader(
            "\u25c9  RESULTS & STEP-BY-STEP SOLUTIONS",
            "  Waiting Time  \u2022  Completion Time  \u2022  Turnaround Time"),
            BorderLayout.NORTH);

        // Scrollable content area
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));

        JScrollPane scroll = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        SJFScheduler sched   = ctrl.scheduler;
        List<Process> results = sched.getProcesses();

        // ── ① Waiting Time ────────────────────────────────────────
        addSection("\u2460 PROCESS WAITING TIME");
        addFormulaLabel("  Formula:  WT = TAT \u2212 BT  =  (CT \u2212 AT) \u2212 BT");
        for (Process p : results) {
            addProcCard(p, new String[][]{
                { "  " + p.pid + ":  CT=" + p.completion
                  + "  AT=" + p.arrival + "  BT=" + p.burst, "false" },
                { "       TAT = CT \u2212 AT = " + p.completion
                  + " \u2212 " + p.arrival + " = " + p.turnaround, "false" },
                { "       WT  = TAT \u2212 BT = " + p.turnaround
                  + " \u2212 " + p.burst + " = " + p.waiting + " ms  \u2713", "true" },
            });
        }
        addAvgCard(new String[][]{
            { "  Avg WT = (" + results.stream()
                .map(p -> String.valueOf(p.waiting)).collect(Collectors.joining(" + "))
              + ") \u00f7 " + results.size(), "false" },
            { "         = " + results.stream().mapToInt(p -> p.waiting).sum()
              + " \u00f7 " + results.size()
              + " = " + String.format("%.2f", sched.avgWaiting()) + " ms  \u2713", "true" },
        });

        // ── ② Completion Time ─────────────────────────────────────
        addSection("\u2461 PROCESS COMPLETION TIME");
        addFormulaLabel(
            "  Formula:  CT = the time unit at which the process finishes execution");
        for (Process p : results) {
            addProcCard(p, new String[][]{
                { "  " + p.pid + ": finishes at t = " + p.completion
                  + "  \u2192  CT = " + p.completion + " ms  \u2713", "true" },
            });
        }
        addAvgCard(new String[][]{
            { "  Avg CT = (" + results.stream()
                .map(p -> String.valueOf(p.completion)).collect(Collectors.joining(" + "))
              + ") \u00f7 " + results.size(), "false" },
            { "         = " + results.stream().mapToInt(p -> p.completion).sum()
              + " \u00f7 " + results.size()
              + " = " + String.format("%.2f", sched.avgCompletion()) + " ms  \u2713", "true" },
        });

        // ── ③ Turnaround Time ─────────────────────────────────────
        addSection("\u2462 PROCESS TURNAROUND TIME");
        addFormulaLabel("  Formula:  TAT = CT \u2212 AT");
        for (Process p : results) {
            addProcCard(p, new String[][]{
                { "  " + p.pid + ": TAT = " + p.completion + " \u2212 "
                  + p.arrival + " = " + p.turnaround + " ms  \u2713", "true" },
            });
        }
        addAvgCard(new String[][]{
            { "  Avg TAT = (" + results.stream()
                .map(p -> String.valueOf(p.turnaround)).collect(Collectors.joining(" + "))
              + ") \u00f7 " + results.size(), "false" },
            { "          = " + results.stream().mapToInt(p -> p.turnaround).sum()
              + " \u00f7 " + results.size()
              + " = " + String.format("%.2f", sched.avgTurnaround()) + " ms  \u2713", "true" },
        });

        // ── ④ Summary of Averages ─────────────────────────────────
        addSection("\u2463 SUMMARY OF AVERAGES");
        JPanel af = Widgets.makeCard();
        af.setLayout(new BoxLayout(af, BoxLayout.Y_AXIS));
        af.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        af.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        String[][] avgRows = {
            { "Average Waiting Time",    String.format("%.2f ms", sched.avgWaiting()) },
            { "Average Completion Time", String.format("%.2f ms", sched.avgCompletion()) },
            { "Average Turnaround Time", String.format("%.2f ms", sched.avgTurnaround()) },
        };
        for (String[] row : avgRows) {
            JPanel rowPanel = new JPanel(new BorderLayout());
            rowPanel.setBackground(Theme.CARD);
            rowPanel.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));

            JLabel lbl = new JLabel(row[0]);
            lbl.setFont(Theme.FONT_BODY);
            lbl.setForeground(Theme.TEXT);

            JLabel val = new JLabel(row[1]);
            val.setFont(new Font("Segoe UI", Font.BOLD, 11));
            val.setForeground(Theme.SUCCESS);

            rowPanel.add(lbl, BorderLayout.WEST);
            rowPanel.add(val, BorderLayout.EAST);
            af.add(rowPanel);
        }
        content.add(af);
        content.add(Box.createVerticalStrut(6));

        // ── ⑤ Final Results Table ─────────────────────────────────
        addSection("\u2464 FINAL RESULTS TABLE");

        String[] headers = { "Process", "Burst\nTime", "Arrival\nTime",
                             "Completion\nTime", "Waiting\nTime", "Turnaround\nTime" };
        int[]    colWidths = { 90, 90, 90, 120, 110, 130 };

        JPanel ftbl = Widgets.makeCard();
        ftbl.setLayout(new BoxLayout(ftbl, BoxLayout.Y_AXIS));
        ftbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2000));

        // Header row
        JPanel hdrRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 6));
        hdrRow.setBackground(Theme.PANEL);
        for (int i = 0; i < headers.length; i++) {
            JLabel h = new JLabel("<html><center>" + headers[i].replace("\n", "<br>")
                + "</center></html>", SwingConstants.CENTER);
            h.setFont(Theme.FONT_LABEL);
            h.setForeground(Theme.ACCENT);
            h.setPreferredSize(new Dimension(colWidths[i], 36));
            hdrRow.add(h);
        }
        ftbl.add(hdrRow);

        // Data rows
        for (Process p : results) {
            JPanel dr = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 5));
            dr.setBackground(Theme.CARD);

            JPanel swatch = new JPanel();
            swatch.setBackground(Color.decode(p.color));
            swatch.setPreferredSize(new Dimension(4, 30));
            dr.add(swatch);

            String[] vals = { p.pid, p.burst + " ms", p.arrival + " ms",
                              p.completion + " ms", p.waiting + " ms",
                              p.turnaround + " ms" };
            for (int i = 0; i < vals.length; i++) {
                Color fg = vals[i].equals(p.pid) ? Color.decode(p.color) : Theme.TEXT;
                JLabel cell = new JLabel(vals[i], SwingConstants.CENTER);
                cell.setFont(Theme.FONT_BODY);
                cell.setForeground(fg);
                cell.setPreferredSize(new Dimension(colWidths[i], 24));
                dr.add(cell);
            }
            ftbl.add(dr);

            JPanel sep = new JPanel();
            sep.setBackground(Theme.BORDER);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            ftbl.add(sep);
        }

        // Average summary row
        JPanel avgRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 6));
        avgRow.setBackground(Color.decode("#eef2ff"));
        String[] avgVals = { "AVERAGE", "\u2014", "\u2014",
            String.format("%.2f ms", sched.avgCompletion()),
            String.format("%.2f ms", sched.avgWaiting()),
            String.format("%.2f ms", sched.avgTurnaround()) };
        for (int i = 0; i < avgVals.length; i++) {
            JLabel cell = new JLabel(avgVals[i], SwingConstants.CENTER);
            cell.setFont(Theme.FONT_LABEL);
            cell.setForeground(Theme.SUCCESS);
            cell.setPreferredSize(new Dimension(colWidths[i], 24));
            avgRow.add(cell);
        }
        ftbl.add(avgRow);
        content.add(ftbl);
        content.add(Box.createVerticalStrut(6));

        // Navigation buttons
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        nav.setBackground(Theme.BG);
        nav.add(Widgets.makeBtn("\u25c4  ANIMATION",   e -> ctrl.showAnim(),
                                Color.decode("#e2e8f0"), Theme.TEXT));
        nav.add(Widgets.makeBtn("\u27f3  NEW SESSION", e -> ctrl.showIntro(),
                                Theme.ACCENT, Color.WHITE));
        nav.add(Widgets.makeBtn("\u2715  EXIT",        e -> System.exit(0),
                                Theme.DANGER, Color.WHITE));
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        content.add(Box.createVerticalStrut(14));
        content.add(nav);
    }

    // ── UI Builder Helpers ────────────────────────────────────────────────────

    /** Adds a bold section heading with a horizontal rule beneath it. */
    private void addSection(String title) {
        content.add(Box.createVerticalStrut(14));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(Theme.ACCENT2);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lbl);

        JSeparator sep = Widgets.makeSeparator(Theme.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        content.add(sep);
        content.add(Box.createVerticalStrut(3));
    }

    /** Adds a monospaced formula hint label. */
    private void addFormulaLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_MONO);
        lbl.setForeground(Theme.SUBTEXT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lbl);
    }

    /**
     * Adds a colored card with a left-border swatch for a single process.
     *
     * @param p     the Process whose color is used for the swatch
     * @param lines array of { text, "true"|"false" } — "true" highlights in WARNING color
     */
    private void addProcCard(Process p, String[][] lines) {
        JPanel frame = new JPanel(new BorderLayout());
        frame.setBackground(Theme.CARD);
        frame.setBorder(BorderFactory.createLineBorder(Color.decode(p.color), 1));
        frame.setMaximumSize(new Dimension(Integer.MAX_VALUE, lines.length * 22 + 10));

        // Left color accent bar
        JPanel accent = new JPanel();
        accent.setBackground(Color.decode(p.color));
        accent.setPreferredSize(new Dimension(5, 1));
        frame.add(accent, BorderLayout.WEST);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(Theme.CARD);
        inner.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

        for (String[] line : lines) {
            JLabel lbl = new JLabel(line[0]);
            lbl.setFont(Theme.FONT_MONO);
            lbl.setForeground("true".equals(line[1]) ? Theme.WARNING : Theme.TEXT);
            inner.add(lbl);
        }
        frame.add(inner, BorderLayout.CENTER);

        frame.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(frame);
        content.add(Box.createVerticalStrut(3));
    }

    /**
     * Adds a grey card showing an average calculation.
     *
     * @param lines array of { text, "true"|"false" } — "true" highlights in WARNING color
     */
    private void addAvgCard(String[][] lines) {
        JPanel card = Widgets.makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(2, 14, 2, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, lines.length * 22 + 10));

        for (String[] line : lines) {
            JLabel lbl = new JLabel(line[0]);
            lbl.setFont(Theme.FONT_MONO);
            lbl.setForeground("true".equals(line[1]) ? Theme.WARNING : Theme.SUBTEXT);
            card.add(lbl);
        }
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(card);
        content.add(Box.createVerticalStrut(4));
    }
}
