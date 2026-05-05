// Theme.java — colors, fonts, and dimensions (light background theme)

import java.awt.*;

public class Theme {

    // Background and surface colors
    public static final Color BG      = Color.decode("#f0f4ff");
    public static final Color PANEL   = Color.decode("#dde4f7");
    public static final Color CARD    = Color.decode("#ffffff");
    public static final Color BORDER  = Color.decode("#b0bce8");
    public static final Color TEXT    = Color.decode("#1a1f3c");
    public static final Color SUBTEXT = Color.decode("#5a6491");
    public static final Color ACCENT  = Color.decode("#3a5bd9");
    public static final Color ACCENT2 = Color.decode("#7c3aed");
    public static final Color SUCCESS = Color.decode("#16a34a");
    public static final Color WARNING = Color.decode("#b45309");
    public static final Color DANGER  = Color.decode("#dc2626");

    // Font definitions
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD,  11);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 10);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN,  9);
    public static final Font FONT_LABEL   = new Font("Segoe UI", Font.BOLD,   9);
    public static final Font FONT_BTN     = new Font("Segoe UI", Font.BOLD,  10);
    public static final Font FONT_MONO    = new Font("Courier New", Font.PLAIN, 9);

    // Window dimensions
    public static final int WIN_W = 980;
    public static final int WIN_H = 700;

    // Animation timing constants
    public static final int TICK_MS   = 900;   // real ms per logical CPU tick
    public static final int SUBSTEPS  = 12;    // animation frames per tick
    public static final int FRAME_MS  = Math.max(1, TICK_MS / SUBSTEPS);
    public static final int PX_PER_TICK = 28;  // fixed pixel width per time unit in Gantt chart

    // Process colors for P1–P10 (cycles if more than 10 processes)
    private static final Color[] PROC_COLORS = {
        Color.decode("#dc2626"),  // P1  Red
        Color.decode("#2563eb"),  // P2  Blue
        Color.decode("#111827"),  // P3  Black
        Color.decode("#16a34a"),  // P4  Green
        Color.decode("#ea580c"),  // P5  Orange
        Color.decode("#7c3aed"),  // P6  Violet
        Color.decode("#0891b2"),  // P7  Cyan
        Color.decode("#be185d"),  // P8  Pink
        Color.decode("#ca8a04"),  // P9  Yellow
        Color.decode("#065f46"),  // P10 Teal
    };

    /** Returns the display color for a process at the given 0-based index. */
    public static Color procColor(int index) {
        return PROC_COLORS[index % PROC_COLORS.length];
    }

    /** Converts a Color to its hex string representation (e.g. "#3a5bd9"). */
    public static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
