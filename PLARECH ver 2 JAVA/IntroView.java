// IntroView.java — splash / title screen
// Draws decorative grid lines, concentric rings, corner brackets, and
// process dots on a custom-painted panel before showing the START button.

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class IntroView extends BaseView {

    public IntroView(AppController ctrl) {
        super(ctrl);
        setLayout(new BorderLayout());
        add(buildCanvas(), BorderLayout.CENTER);
    }

    /** Returns the custom-painted splash canvas panel. */
    private JPanel buildCanvas() {
        return new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w  = getWidth();
                int h  = getHeight();
                int cx = w / 2;
                int cy = h / 2;
                int oy = cy - 20;   // optical centre — slightly above physical centre

                // Decorative light grid
                g2.setColor(Color.decode("#e2e8f5"));
                g2.setStroke(new BasicStroke(1));
                for (int x = 0; x <= w; x += 48)
                    g2.drawLine(x, 0, x, h);
                for (int y = 0; y <= h; y += 48)
                    g2.drawLine(0, y, w, y);

                // Concentric accent rings
                int[][] rings = { {210, 0xdbeafe}, {160, 0xbfdbfe}, {110, 0x93c5fd} };
                for (int[] ring : rings) {
                    int r   = ring[0];
                    Color rc = new Color(ring[1]);
                    g2.setColor(rc);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawOval(cx - r, oy - r, r * 2, r * 2);
                }

                // Corner bracket accents
                int span = 60;
                int[][] corners = {
                    {20, 20},
                    {w - 20, 20},
                    {20, h - 20},
                    {w - 20, h - 20}
                };
                int[][] hDirs = { {1, 0}, {-1, 0}, {1, 0}, {-1, 0} };
                int[][] vDirs = { {0, 1}, {0, 1}, {0, -1}, {0, -1} };

                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(3));
                for (int i = 0; i < 4; i++) {
                    int bx = corners[i][0];
                    int by = corners[i][1];
                    g2.drawLine(bx, by, bx + hDirs[i][0] * span, by + hDirs[i][1] * span);
                    g2.drawLine(bx, by, bx + vDirs[i][0] * span, by + vDirs[i][1] * span);
                }

                // Five coloured process dots (purely decorative)
                for (int i = 0; i < 5; i++) {
                    int dx = cx - 100 + i * 50;
                    g2.setColor(Theme.procColor(i));
                    g2.fillOval(dx - 10, oy - 190, 20, 20);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawOval(dx - 10, oy - 190, 20, 20);
                }

                // Project title
                drawCenteredText(g2, "SJF REPLICA PROJECT",
                    new Font("Segoe UI", Font.BOLD, 30), Theme.TEXT, cx, oy - 130);
                drawCenteredText(g2, "Preemptive Shortest Job First  \u2022  CPU Scheduling Simulator",
                    new Font("Segoe UI", Font.PLAIN, 11), Theme.SUBTEXT, cx, oy - 90);

                // Divider line above credits
                g2.setColor(Theme.BORDER);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(cx - 280, oy - 62, cx + 280, oy - 62);

                // Credits
                drawCenteredText(g2, "Developed by:",
                    new Font("Segoe UI", Font.PLAIN, 10), Theme.SUBTEXT, cx, oy - 36);
                drawCenteredText(g2, "LEADER \u2014 Entice",
                    new Font("Segoe UI", Font.BOLD, 16), Theme.ACCENT, cx, oy - 10);
                drawCenteredText(g2, "Members:   Aloro   \u2022   Muyco",
                    new Font("Segoe UI", Font.PLAIN, 12), Theme.TEXT, cx, oy + 20);

                // Divider line below credits
                g2.setColor(Theme.BORDER);
                g2.drawLine(cx - 280, oy + 48, cx + 280, oy + 48);

                // Description text
                drawCenteredText(g2,
                    "An educational simulation of the Shortest Remaining Time First algorithm.",
                    new Font("Segoe UI", Font.PLAIN, 9), Theme.SUBTEXT, cx, oy + 80);

                // Footer note
                drawCenteredText(g2,
                    "CPU Scheduling Simulator  \u2022  Educational Tool  \u2022  Java / Swing",
                    new Font("Segoe UI", Font.PLAIN, 8), Color.decode("#94a3b8"), cx, h - 18);
            }

            /** Utility: draws a string centered at (x, y). */
            private void drawCenteredText(Graphics2D g2, String text, Font font,
                                          Color color, int x, int y) {
                g2.setFont(font);
                g2.setColor(color);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x - fm.stringWidth(text) / 2;
                int ty = y + fm.getAscent() / 2;
                g2.drawString(text, tx, ty);
            }

            { setBackground(Color.decode("#f0f4ff")); }
        };
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Add START and CANCEL buttons overlaid on the canvas after layout is complete
        buildButtons();
    }

    /** Adds the START and CANCEL buttons using an absolute-positioned overlay. */
    private void buildButtons() {
        setLayout(new OverlayLayout(this));

        JPanel overlay = new JPanel(null) {
            { setOpaque(false); }
        };

        // Position buttons in the lower-center of the window
        int cx     = Theme.WIN_W / 2;
        int oy     = Theme.WIN_H / 2 - 20;
        int btnY   = oy + 130;
        int btnW   = 140;
        int btnH   = 38;

        JButton startBtn = Widgets.makeBtn("\u25b6   START", e -> ctrl.showCount(),
                                           Theme.ACCENT, Color.WHITE);
        startBtn.setBounds(cx - btnW - 10, btnY, btnW, btnH);
        overlay.add(startBtn);

        JButton cancelBtn = Widgets.makeBtn("\u2715  CANCEL", e -> System.exit(0),
                                            Color.decode("#cbd5e1"), Theme.TEXT);
        cancelBtn.setBounds(cx + 10, btnY, btnW, btnH);
        overlay.add(cancelBtn);

        add(overlay);
    }
}
