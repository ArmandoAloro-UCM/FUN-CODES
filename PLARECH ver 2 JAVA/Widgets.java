// Widgets.java — reusable Swing helper methods

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class Widgets {

    /**
     * Returns a darker version of the given color by subtracting `amt` from each channel.
     * Channels are clamped to a minimum of 0.
     */
    public static Color darken(Color c, int amt) {
        return new Color(
            Math.max(0, c.getRed()   - amt),
            Math.max(0, c.getGreen() - amt),
            Math.max(0, c.getBlue()  - amt)
        );
    }

    /**
     * Creates a flat, styled button with hover darkening and a hand cursor.
     *
     * @param text   button label
     * @param action ActionListener to attach
     * @param bg     background color
     * @param fg     foreground (text) color
     */
    public static JButton makeBtn(String text, ActionListener action, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                // Paint background manually so the flat look is preserved
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        btn.addActionListener(action);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(Theme.FONT_BTN);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);   // we paint manually above
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(8, 14, 8, 14));

        // Hover effect: darken background while the cursor is over the button
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(darken(bg, 22)); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(bg); }
        });

        return btn;
    }

    /**
     * Creates the standard page header bar with a title and optional subtitle.
     * The returned panel is already configured for pack/add — caller adds it to the view.
     */
    public static JPanel makeHeader(String title, String subtitle) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        bar.setBackground(Theme.PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.BORDER));

        JLabel titleLbl = new JLabel("  " + title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(Theme.TEXT);
        bar.add(titleLbl);

        if (subtitle != null && !subtitle.isEmpty()) {
            JLabel subLbl = new JLabel("  " + subtitle);
            subLbl.setFont(Theme.FONT_BODY);
            subLbl.setForeground(Theme.SUBTEXT);
            bar.add(subLbl);
        }

        return bar;
    }

    /**
     * Creates a thin horizontal separator line (1 px).
     *
     * @param color border color
     */
    public static JSeparator makeSeparator(Color color) {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(color);
        sep.setBackground(color);
        return sep;
    }

    /**
     * Creates a white card panel with a 1-px border.
     * The card uses BorderLayout by default; callers can override with setLayout().
     */
    public static JPanel makeCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.CARD);
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        return card;
    }

    /**
     * Returns True if the given hex color is perceptually dark using the
     * ITU-R BT.601 luma formula — used to decide whether overlay text should
     * be white or dark.
     *
     * @param hex color in "#rrggbb" format
     */
    public static boolean isDark(String hex) {
        Color c = Color.decode(hex);
        double luma = c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114;
        return luma < 150;
    }

    /**
     * Returns a lighter version of the given hex color.
     * Each RGB channel is raised by `amount`, clamped to 255.
     * Used for the leading-edge highlight on animated bar tips.
     *
     * @param hex    source color in "#rrggbb" format
     * @param amount amount to add to each channel
     */
    public static String lighten(String hex, int amount) {
        Color c = Color.decode(hex);
        return String.format("#%02x%02x%02x",
            Math.min(255, c.getRed()   + amount),
            Math.min(255, c.getGreen() + amount),
            Math.min(255, c.getBlue()  + amount)
        );
    }
}
