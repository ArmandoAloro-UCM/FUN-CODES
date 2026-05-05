// CountView.java — choose number of processes (3–10)
// Shows a minus / count / plus spinner and a row of colored dots that
// updates in real time.  Saves the selection to ctrl.numProcesses.

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class CountView extends BaseView {

    private int count;
    private JLabel countLabel;
    private JPanel dotPanel;

    public CountView(AppController ctrl) {
        super(ctrl);
        // Preserve previous selection if the user navigates back here
        this.count = ctrl.numProcesses;
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // Header bar
        add(Widgets.makeHeader(
            "\u25c9  HOW MANY PROCESSES?",
            "  Minimum 3  \u2022  Maximum 10"), BorderLayout.NORTH);

        // Center body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createEmptyBorder(36, 40, 20, 40));

        // Instruction label
        JLabel instrLbl = new JLabel("Select the number of processes to schedule:");
        instrLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        instrLbl.setForeground(Theme.TEXT);
        instrLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(instrLbl);
        body.add(Box.createVerticalStrut(18));

        // [−]  count  [+] spinner row
        JPanel spinner = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        spinner.setBackground(Theme.BG);

        JButton decBtn = Widgets.makeBtn("\u2212", e -> changeCount(-1),
                                         Color.decode("#e2e8f0"), Theme.TEXT);
        decBtn.setPreferredSize(new Dimension(48, 44));

        countLabel = new JLabel(String.valueOf(count), SwingConstants.CENTER);
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 34));
        countLabel.setForeground(Theme.ACCENT);
        countLabel.setPreferredSize(new Dimension(70, 50));

        JButton incBtn = Widgets.makeBtn("+", e -> changeCount(+1),
                                         Theme.ACCENT, Color.WHITE);
        incBtn.setPreferredSize(new Dimension(48, 44));

        spinner.add(decBtn);
        spinner.add(countLabel);
        spinner.add(incBtn);

        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(spinner);
        body.add(Box.createVerticalStrut(18));

        // Dot row — filled dots up to current count, grey beyond
        dotPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        dotPanel.setBackground(Theme.BG);
        dotPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshDots();
        body.add(dotPanel);
        body.add(Box.createVerticalStrut(8));

        JLabel hintLbl = new JLabel("(minimum 3  \u2022  maximum 10)");
        hintLbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        hintLbl.setForeground(Theme.SUBTEXT);
        hintLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(hintLbl);

        // Separator
        body.add(Box.createVerticalStrut(20));
        JSeparator sep = Widgets.makeSeparator(Theme.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(sep);
        body.add(Box.createVerticalStrut(20));

        // Navigation buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnRow.setBackground(Theme.BG);
        btnRow.add(Widgets.makeBtn("\u25c4  BACK", e -> ctrl.showIntro(),
                                   Color.decode("#e2e8f0"), Theme.TEXT));
        btnRow.add(Widgets.makeBtn("NEXT  \u2192", e -> confirm(),
                                   Theme.ACCENT, Color.WHITE));
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(btnRow);

        // Wrap body in a centering panel
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.BG);
        wrapper.add(body);
        add(wrapper, BorderLayout.CENTER);
    }

    /** Increments or decrements the process count within [3, 10]. */
    private void changeCount(int delta) {
        int next = count + delta;
        if (next < 3 || next > 10) return;
        count = next;
        countLabel.setText(String.valueOf(count));
        refreshDots();
    }

    /** Redraws the dot row to match the current count. */
    private void refreshDots() {
        dotPanel.removeAll();
        for (int i = 0; i < 10; i++) {
            Color c = (i < count) ? Theme.procColor(i) : Theme.BORDER;
            JPanel dot = new JPanel() {
                { setPreferredSize(new Dimension(24, 24)); setBackground(c); }
            };
            dotPanel.add(dot);
        }
        dotPanel.revalidate();
        dotPanel.repaint();
    }

    /** Saves the selection and advances to the input view. */
    private void confirm() {
        ctrl.numProcesses = count;
        ctrl.showInput();
    }
}
