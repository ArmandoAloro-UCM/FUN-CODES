// InputView.java — burst / arrival time entry form
// Renders one row per process with color swatches and numeric fields.
// Tab / Enter auto-advances:  burst[i] → arrival[i] → burst[i+1] → …
// Validation:  burst 1–20 ms,  arrival 0–20 ms.

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class InputView extends BaseView {

    private final int n;
    private final JTextField[] burstFields;
    private final JTextField[] arrivalFields;

    public InputView(AppController ctrl) {
        super(ctrl);
        this.n             = ctrl.numProcesses;
        this.burstFields   = new JTextField[n];
        this.arrivalFields = new JTextField[n];
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // Header
        add(Widgets.makeHeader(
            "\u25c9  PROCESS INPUT  (" + n + " processes)",
            "  Burst: 1\u201320 ms  \u2022  Arrival: 0\u201320 ms  \u2022  Enter/Tab advances cursor"),
            BorderLayout.NORTH);

        // Outer padding wrapper
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Theme.BG);
        outer.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));

        // Table card
        JPanel tbl = Widgets.makeCard();
        tbl.setLayout(new BoxLayout(tbl, BoxLayout.Y_AXIS));

        // Column headers
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 7));
        hdr.setBackground(Theme.PANEL);
        for (String[] col : new String[][]{
                {"  PROCESS", "11"},
                {"BURST TIME (ms)", "22"},
                {"ARRIVAL TIME (ms)", "22"} }) {
            JLabel lbl = new JLabel(col[0], SwingConstants.CENTER);
            lbl.setFont(Theme.FONT_LABEL);
            lbl.setForeground(Theme.ACCENT);
            lbl.setPreferredSize(new Dimension(Integer.parseInt(col[1]) * 7, 24));
            hdr.add(lbl);
        }
        tbl.add(hdr);

        // One data row per process
        for (int i = 0; i < n; i++) {
            Color colour = Theme.procColor(i);
            String pid   = "P" + (i + 1);

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
            row.setBackground(Theme.CARD);

            // Left color swatch strip
            JPanel swatch = new JPanel();
            swatch.setBackground(colour);
            swatch.setPreferredSize(new Dimension(6, 34));
            row.add(swatch);

            // PID label
            JLabel pidLbl = new JLabel(pid, SwingConstants.CENTER);
            pidLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            pidLbl.setForeground(colour);
            pidLbl.setPreferredSize(new Dimension(35, 24));
            row.add(pidLbl);

            // Burst time field
            JTextField bField = makeField();
            burstFields[i] = bField;
            row.add(Box.createHorizontalStrut(40));
            row.add(bField);

            // Arrival time field
            JTextField aField = makeField();
            arrivalFields[i] = aField;
            row.add(Box.createHorizontalStrut(40));
            row.add(aField);

            tbl.add(row);

            // Thin separator below each row (except the last)
            if (i < n - 1) {
                JPanel sep = new JPanel();
                sep.setBackground(Theme.BORDER);
                sep.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                tbl.add(sep);
            }
        }

        // Auto-advance bindings: burst[i] → arrival[i] → burst[i+1]
        for (int i = 0; i < n; i++) {
            final int idx = i;
            burstFields[i].addActionListener(e -> arrivalFields[idx].requestFocus());
            burstFields[i].addKeyListener(new TabAdvance(() -> arrivalFields[idx].requestFocus()));

            if (i < n - 1) {
                arrivalFields[i].addActionListener(e -> burstFields[idx + 1].requestFocus());
                arrivalFields[i].addKeyListener(new TabAdvance(() -> burstFields[idx + 1].requestFocus()));
            }
        }
        burstFields[0].requestFocusInWindow();

        outer.add(tbl, BorderLayout.NORTH);

        // Hint text
        JLabel hint = new JLabel(
            "\ud83d\udca1  Enter or Tab moves to the next field automatically.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        hint.setForeground(Theme.SUBTEXT);
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        outer.add(hint, BorderLayout.CENTER);

        // Navigation / action buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        btnRow.setBackground(Theme.BG);
        btnRow.add(Widgets.makeBtn("\u25c4  BACK", e -> ctrl.showCount(),
                                   Color.decode("#e2e8f0"), Theme.TEXT));
        btnRow.add(Widgets.makeBtn("\u2699  COMPUTE \u2192", e -> submit(),
                                   Theme.ACCENT, Color.WHITE));
        btnRow.add(Widgets.makeBtn("\u2715  CLEAR", e -> clearAll(),
                                   Color.decode("#e2e8f0"), Theme.TEXT));
        outer.add(btnRow, BorderLayout.SOUTH);

        add(outer, BorderLayout.CENTER);
    }

    /** Creates a styled numeric text field. */
    private JTextField makeField() {
        JTextField tf = new JTextField(6);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tf.setForeground(Theme.TEXT);
        tf.setBackground(Color.decode("#f8faff"));
        tf.setHorizontalAlignment(JTextField.CENTER);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1),
            BorderFactory.createEmptyBorder(5, 4, 5, 4)
        ));
        return tf;
    }

    /** Clears all entry fields and returns focus to the first burst field. */
    private void clearAll() {
        for (int i = 0; i < n; i++) {
            burstFields[i].setText("");
            arrivalFields[i].setText("");
        }
        burstFields[0].requestFocus();
    }

    /**
     * Validates inputs, builds the Process list, runs the scheduler,
     * then navigates to the input summary table view.
     */
    private void submit() {
        List<Process> procs = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            String b = burstFields[i].getText().trim();
            String a = arrivalFields[i].getText().trim();

            if (b.isEmpty() || a.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "P" + (i + 1) + ": Both burst time and arrival time are required.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int bi, ai;
            try {
                bi = Integer.parseInt(b);
                ai = Integer.parseInt(a);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "P" + (i + 1) + ": Please enter valid integers.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (bi < 1 || bi > 20) {
                JOptionPane.showMessageDialog(this,
                    "P" + (i + 1) + ": Burst time must be between 1 and 20 ms.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (ai < 0 || ai > 20) {
                JOptionPane.showMessageDialog(this,
                    "P" + (i + 1) + ": Arrival time must be between 0 and 20 ms.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            procs.add(new Process(
                "P" + (i + 1), bi, ai,
                Theme.toHex(Theme.procColor(i))
            ));
        }

        ctrl.processes = procs;
        SJFScheduler sched = new SJFScheduler(procs);
        sched.run();
        ctrl.scheduler = sched;
        ctrl.showTable();
    }

    /** KeyListener that fires a Runnable on Tab key press. */
    private static class TabAdvance extends KeyAdapter {
        private final Runnable action;
        TabAdvance(Runnable action) { this.action = action; }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                action.run();
                e.consume();
            }
        }
    }
}
