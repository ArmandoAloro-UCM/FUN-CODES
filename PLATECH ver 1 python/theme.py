# theme.py — colors, fonts, dimensions (light background theme)

BG      = "#f0f4ff"
PANEL   = "#dde4f7"
CARD    = "#ffffff"
BORDER  = "#b0bce8"
TEXT    = "#1a1f3c"
SUBTEXT = "#5a6491"
ACCENT  = "#3a5bd9"
ACCENT2 = "#7c3aed"
SUCCESS = "#16a34a"
WARNING = "#b45309"
DANGER  = "#dc2626"

FONT_TITLE   = ("Segoe UI", 13, "bold")
FONT_HEADING = ("Segoe UI", 11, "bold")
FONT_BODY    = ("Segoe UI", 10)
FONT_SMALL   = ("Segoe UI", 9)
FONT_LABEL   = ("Segoe UI", 9,  "bold")
FONT_BTN     = ("Segoe UI", 10, "bold")
FONT_MONO    = ("Courier New", 9)

WIN_W = 980
WIN_H = 700

GANTT_STEP_MS = 350
BAR_STEP_MS   = 500
TIMER_TICK_MS = 100

# P1-P10 process colors (extends beyond 5)
_COLORS = [
    "#dc2626",  # P1  Red
    "#2563eb",  # P2  Blue
    "#111827",  # P3  Black
    "#16a34a",  # P4  Green
    "#ea580c",  # P5  Orange
    "#7c3aed",  # P6  Violet
    "#0891b2",  # P7  Cyan
    "#be185d",  # P8  Pink
    "#ca8a04",  # P9  Yellow
    "#065f46",  # P10 Teal
]

def proc_color(index: int) -> str:
    return _COLORS[index % len(_COLORS)]