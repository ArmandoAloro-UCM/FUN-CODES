# widgets.py — reusable tkinter helpers

import tkinter as tk
from theme import (
    BG, PANEL, CARD, BORDER, TEXT, SUBTEXT,
    ACCENT, SUCCESS, DANGER,
    FONT_BTN, FONT_BODY, FONT_LABEL, FONT_SMALL,
    TIMER_TICK_MS,
)


def _dk(hex_color: str, amt: int = 22) -> str:
    h = hex_color.lstrip("#")
    r, g, b = (int(h[i:i+2], 16) for i in (0, 2, 4))
    return "#{:02x}{:02x}{:02x}".format(max(0,r-amt), max(0,g-amt), max(0,b-amt))


def make_btn(parent, text: str, cmd, bg=ACCENT, fg="#ffffff", width=13, padx=12, pady=8):
    b = tk.Button(parent, text=text, command=cmd, bg=bg, fg=fg,
                  font=FONT_BTN, relief="flat", bd=0,
                  padx=padx, pady=pady, width=width,
                  activebackground=_dk(bg), activeforeground=fg,
                  cursor="hand2")
    b.bind("<Enter>", lambda _: b.config(bg=_dk(bg)))
    b.bind("<Leave>", lambda _: b.config(bg=bg))
    return b


def make_header(parent, title: str, subtitle: str = "") -> tk.Frame:
    bar = tk.Frame(parent, bg=PANEL, pady=11)
    bar.pack(fill="x")
    tk.Label(bar, text=title, font=("Segoe UI", 13, "bold"),
             bg=PANEL, fg=TEXT).pack(side="left", padx=20)
    if subtitle:
        tk.Label(bar, text=subtitle, font=FONT_BODY,
                 bg=PANEL, fg=SUBTEXT).pack(side="left", padx=6)
    tk.Frame(bar, bg=BORDER, height=2).pack(fill="x", side="bottom")
    return bar


def hsep(parent, pady=6, color=BORDER):
    tk.Frame(parent, bg=color, height=1).pack(fill="x", pady=pady)


def make_card(parent, **kw) -> tk.Frame:
    defaults = dict(bg=CARD, highlightbackground=BORDER, highlightthickness=1)
    defaults.update(kw)
    return tk.Frame(parent, **defaults)


def make_scroll_frame(parent):
    """Returns (canvas, inner_frame) — inner_frame is scrollable."""
    canvas = tk.Canvas(parent, bg=BG, highlightthickness=0)
    vsb    = tk.Scrollbar(parent, orient="vertical", command=canvas.yview)
    canvas.configure(yscrollcommand=vsb.set)
    vsb.pack(side="right", fill="y")
    canvas.pack(side="left", fill="both", expand=True)

    inner  = tk.Frame(canvas, bg=BG)
    wid    = canvas.create_window((0, 0), window=inner, anchor="nw")

    inner.bind("<Configure>", lambda e: canvas.configure(scrollregion=canvas.bbox("all")))
    canvas.bind("<Configure>", lambda e: canvas.itemconfig(wid, width=e.width))

    def _wheel(e):
        delta = -1 if (e.delta > 0 or e.num == 4) else 1
        canvas.yview_scroll(delta, "units")
    canvas.bind_all("<MouseWheel>", _wheel)
    canvas.bind_all("<Button-4>",   _wheel)
    canvas.bind_all("<Button-5>",   _wheel)

    return canvas, inner


class MsecTimer:
    """Live ms timer shown as text on a Canvas (upper-right corner)."""

    def __init__(self, canvas: tk.Canvas, tick_ms: int = TIMER_TICK_MS):
        self._cv      = canvas
        self._tick    = tick_ms
        self._elapsed = 0
        self._running = False
        self._aid     = None

        # background pill
        self._bg = canvas.create_rectangle(0, 0, 0, 0,
                                           fill=PANEL, outline=BORDER, width=1)
        self._txt = canvas.create_text(0, 0, text="⏱  0 ms",
                                       font=("Segoe UI", 10, "bold"),
                                       fill=TEXT, anchor="ne")
        canvas.bind("<Configure>", lambda e: self._reposition())
        self.after_id_reposition = canvas.after(80, self._reposition)

    def _reposition(self):
        w = self._cv.winfo_width()
        if w < 2:
            return
        x, y = w - 10, 10
        self._cv.coords(self._txt, x, y)
        bb = self._cv.bbox(self._txt)
        if bb:
            self._cv.coords(self._bg, bb[0]-5, bb[1]-3, bb[2]+5, bb[3]+3)
            self._cv.tag_raise(self._txt)

    def _tick_fn(self):
        if not self._running:
            return
        self._elapsed += 1
        self._cv.itemconfig(self._txt, text=f"⏱  {self._elapsed * self._tick} ms")
        self._reposition()
        self._aid = self._cv.after(self._tick, self._tick_fn)

    def start(self):
        if self._running:
            return
        self._running = True
        self._aid = self._cv.after(self._tick, self._tick_fn)

    def stop(self):
        self._running = False
        if self._aid:
            try: self._cv.after_cancel(self._aid)
            except Exception: pass

    def reset(self):
        self.stop()
        self._elapsed = 0
        self._cv.itemconfig(self._txt, text="⏱  0 ms")
        self._reposition()