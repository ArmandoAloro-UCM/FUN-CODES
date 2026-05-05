// TableView.java — input summary table
// Displays entered processes in a table so the user can review them
// before the animation begins.

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

public class TableView extends BaseView {

    public TableView(AppController ctrl) {
        super(ctrl);
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // Header
        add(Widgets.makeHeader(
            "\u25c9  INPUT SUMMARY TABLE",
            "  Review your processes before the animation begins"),
            BorderLayout.NORTH);

        // Outer padding
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Theme.BG);
        outer.setBorder(BorderFactory.createEmptyBorder(18, 40, 18, 40));

        // Section title
        JLabel sectionLbl = new JLabel("PROCESS INPUT SUMMARY");
        sectionLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sectionLbl.setForeground(Theme.ACCENT2);
        sectionLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        outer.add(sectionLbl, BorderLayout.NORTH);

        // Table card
        JPanel tbl = Widgets.makeCard();
        tbl.setLayout(new BoxLayout(tbl, BoxLayout.Y_AXIS));

        // Column headers
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        hdr.setBackground(Theme.PANEL);
        for (String[] col : new String[][]{
                {"Process ID", "16"},
                {"Burst Time (ms)", "22"},
                {"Arrival Time (ms)", "22"} }) {
            JLabel lbl = new JLabel(col[0], SwingConstants.CENTER);
            lbl.setFont(Theme.FONT_LABEL);
            lbl.setForeground(Theme.ACCENT);
            lbl.setPreferredSize(new Dimension(Integer.parseInt(col[1]) * 7, 24));
            hdr.add(lbl);
        }
        tbl.add(hdr);

        // One data row per process
        List<Process> procs = ctrl.processes;
        for (Process p : procs) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
            row.setBackground(Theme.CARD);

            // Left color swatch
            JPanel swatch = new JPanel();
            swatch.setBackground(Color.decode(p.color));
            swatch.setPreferredSize(new Dimension(5, 34));
            row.add(swatch);

            // Data cells: PID, burst, arrival
            String[] vals = { p.pid, p.burst + " ms", p.arrival + " ms" };
            int[] widths   = { 16, 22, 22 };
            for (int i = 0; i < vals.length; i++) {
                Color fg = vals[i].equals(p.pid) ? Color.decode(p.color) : Theme.TEXT;
                JLabel cell = new JLabel(vals[i], SwingConstants.CENTER);
                cell.setFont(Theme.FONT_BODY);
                cell.setForeground(fg);
                cell.setPreferredSize(new Dimension(widths[i] * 7, 24));
                row.add(cell);
            }
            tbl.add(row);

            // Thin separator
            JPanel sep = new JPanel();
            sep.setBackground(Theme.BORDER);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            tbl.add(sep);
        }

        outer.add(tbl, BorderLayout.CENTER);

        // Info card explaining what the animation will show
        JPanel info = Widgets.makeCard();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        String[] infoLines = {
            "  \u2139   One combined animation screen is ready:",
            "      LEFT  \u2014 Gantt Chart: horizontal bar grows right, colour-coded by process.",
            "      RIGHT \u2014 Process Timeline: active processes are stacked vertically.",
            "               Each block = one active process.  Finished blocks disappear,",
            "               so the stack height changes over time.",
            "      Both sides run simultaneously with a live ms timer.",
        };
        for (String line : infoLines) {
            JLabel lbl = new JLabel(line);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            lbl.setForeground(Theme.TEXT);
            info.add(lbl);
        }

        // Navigation buttons
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        nav.setBackground(Theme.BG);
        nav.add(Widgets.makeBtn("\u25c4  BACK", e -> ctrl.showInput(),
                                Color.decode("#e2e8f0"), Theme.TEXT));
        nav.add(Widgets.makeBtn("\u25b6  ANIMATE \u2192", e -> ctrl.showAnim(),
                                Theme.SUCCESS, Color.WHITE));

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(Theme.BG);
        south.add(info, BorderLayout.NORTH);
        south.add(nav,  BorderLayout.SOUTH);
        south.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        outer.add(south, BorderLayout.SOUTH);

        add(outer, BorderLayout.CENTER);
    }
}
