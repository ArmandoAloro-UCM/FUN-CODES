
import tkinter as tk
from tkinter import messagebox
from typing import List

from theme import (
    BG, PANEL, CARD, BORDER, TEXT, SUBTEXT,
    ACCENT, ACCENT2, SUCCESS, WARNING, DANGER,
    FONT_TITLE, FONT_HEADING, FONT_BODY, FONT_SMALL,
    FONT_LABEL, FONT_BTN, FONT_MONO,
    WIN_W, WIN_H,
    proc_color,
)
from widgets import make_btn, make_header, hsep, make_card, make_scroll_frame
from scheduler import Process, SJFScheduler



# ANIMATION CONSTANTS  (single source of truth — used only in AnimView)

# Real ms per logical CPU tick.  Raise to slow the animation further.
TICK_MS   = 900          # one simulated ms takes 900 real ms  (very readable)

# Sub-steps per tick: each tick is split into this many frames so the
# Gantt edge and timeline blocks grow smoothly instead of jumping.
SUBSTEPS  = 12           # frames per tick

# Derived: real ms between consecutive animation frames
FRAME_MS  = max(1, TICK_MS // SUBSTEPS)

# Fixed pixel width of ONE tick unit in the Gantt chart.
# Because this never changes with the window size, the bars never stretch.
# A horizontal scrollbar appears if the chart exceeds the visible area.
PX_PER_TICK = 28         # px per CPU time unit


# BaseView — thin base class for every screen

class BaseView(tk.Frame):
    """Provides self.ctrl (AppController) and a consistent BG colour."""
    def __init__(self, master, ctrl):
        super().__init__(master, bg=BG)
        self.ctrl = ctrl

# 1.  INTRO VIEW — splash / title screen

class IntroView(BaseView):
    """
    First screen.  Draws everything on a Canvas so we can add decorative
    grid lines, concentric rings, and corner brackets.
    """

    def __init__(self, master, ctrl):
        super().__init__(master, ctrl)
        self._build()

    def _build(self):
        cv = tk.Canvas(self, bg="#f0f4ff", highlightthickness=0)
        cv.pack(fill="both", expand=True)
        self._cv = cv
        cv.bind("<Configure>", lambda e: self._draw())
        self.after(60, self._draw)

    def _draw(self):
        """Redraws all canvas elements on resize or startup."""
        cv = self._cv
        cv.delete("all")

        w  = cv.winfo_width()  or WIN_W
        h  = cv.winfo_height() or WIN_H
        cx = w // 2
        cy = h // 2
        oy = cy - 20     # optical centre (slightly above physical centre)

        # Decorative light grid
        for x in range(0, w + 1, 48):
            cv.create_line(x, 0, x, h, fill="#e2e8f5", width=1)
        for y in range(0, h + 1, 48):
            cv.create_line(0, y, w, y, fill="#e2e8f5", width=1)

        # Concentric accent rings
        for radius, colour in [(210, "#dbeafe"), (160, "#bfdbfe"), (110, "#93c5fd")]:
            cv.create_oval(cx - radius, oy - radius,
                           cx + radius, oy + radius,
                           outline=colour, width=2)

        # Corner bracket accents
        span = 60
        corners = [
            ((20, 20, 20+span, 20),           (20, 20, 20, 20+span)),
            ((w-20, 20, w-20-span, 20),       (w-20, 20, w-20, 20+span)),
            ((20, h-20, 20+span, h-20),       (20, h-20, 20, h-20-span)),
            ((w-20, h-20, w-20-span, h-20),   (w-20, h-20, w-20, h-20-span)),
        ]
        for (x1,y1,x2,y2), (x3,y3,x4,y4) in corners:
            cv.create_line(x1, y1, x2, y2, fill=ACCENT, width=3)
            cv.create_line(x3, y3, x4, y4, fill=ACCENT, width=3)

        # Five coloured process dots (purely decorative)
        for i in range(5):
            dx = cx - 100 + i * 50
            cv.create_oval(dx-10, oy-190, dx+10, oy-170,
                           fill=proc_color(i), outline="white", width=2)

        # Project title
        cv.create_text(cx, oy - 130,
                       text="SJF REPLICA PROJECT",
                       font=("Segoe UI", 30, "bold"), fill=TEXT, anchor="center")
        cv.create_text(cx, oy - 90,
                       text="Preemptive Shortest Job First  •  CPU Scheduling Simulator",
                       font=("Segoe UI", 11), fill=SUBTEXT, anchor="center")

        # Divider + credits
        cv.create_line(cx-280, oy-62, cx+280, oy-62, fill=BORDER, width=2)
        cv.create_text(cx, oy-36, text="Developed by:",
                       font=("Segoe UI", 10), fill=SUBTEXT, anchor="center")
        cv.create_text(cx, oy-10, text="LEADER — Entice",
                       font=("Segoe UI", 16, "bold"), fill=ACCENT, anchor="center")
        cv.create_text(cx, oy+20, text="Members:   Aloro   •   Muyco",
                       font=("Segoe UI", 12), fill=TEXT, anchor="center")
        cv.create_line(cx-280, oy+48, cx+280, oy+48, fill=BORDER, width=2)

        cv.create_text(cx, oy+80,
                       text="An educational simulation of the Shortest Remaining Time First algorithm.",
                       font=("Segoe UI", 9), fill=SUBTEXT, anchor="center")

        # Start / Cancel buttons embedded in the canvas
        btn_frame = tk.Frame(cv, bg="#f0f4ff")
        cv.create_window(cx, oy + 145, window=btn_frame)
        make_btn(btn_frame, "▶   START",  self.ctrl.show_count,
                 bg=ACCENT, fg="#fff", width=13).pack(side="left", padx=10)
        make_btn(btn_frame, "✕  CANCEL", self.ctrl.quit,
                 bg="#cbd5e1", fg=TEXT, width=13).pack(side="left", padx=10)

        # Footer note
        cv.create_text(cx, h - 18,
                       text="CPU Scheduling Simulator  •  Educational Tool  •  Python / tkinter",
                       font=("Segoe UI", 8), fill="#94a3b8", anchor="center")


# 2.  COUNT VIEW — choose number of processes (3–10)

class CountView(BaseView):
    """
    Spinner widget (− / count / +) plus colour-coded dot row for visual
    feedback.  Saves selection in ctrl.num_processes.
    """

    def __init__(self, master, ctrl):
        super().__init__(master, ctrl)
        self._val = tk.IntVar(value=getattr(ctrl, "num_processes", 3))
        self._build()

    def _build(self):
        make_header(self, "  ◉  HOW MANY PROCESSES?",
                    "  Minimum 3  •  Maximum 10")

        body = tk.Frame(self, bg=BG)
        body.pack(expand=True)

        tk.Label(body, text="Select the number of processes to schedule:",
                 font=("Segoe UI", 12), bg=BG, fg=TEXT).pack(pady=(36, 18))

        # [−]  count  [+]
        spin = tk.Frame(body, bg=BG)
        spin.pack()
        make_btn(spin, "−", self._dec, bg="#e2e8f0", fg=TEXT,
                 width=3, padx=10, pady=10).pack(side="left", padx=8)
        tk.Label(spin, textvariable=self._val,
                 font=("Segoe UI", 34, "bold"), bg=BG, fg=ACCENT,
                 width=3, anchor="center").pack(side="left", padx=10)
        make_btn(spin, "+", self._inc, bg=ACCENT, fg="#fff",
                 width=3, padx=10, pady=10).pack(side="left", padx=8)

        # Dot row: filled dots up to chosen count, grey beyond
        self._dot_frame = tk.Frame(body, bg=BG)
        self._dot_frame.pack(pady=18)
        self._refresh_dots()

        tk.Label(body, text="(minimum 3  •  maximum 10)",
                 font=("Segoe UI", 9), bg=BG, fg=SUBTEXT).pack()

        hsep(body, pady=20)

        row = tk.Frame(body, bg=BG)
        row.pack()
        make_btn(row, "◀  BACK", self.ctrl.show_intro,
                 bg="#e2e8f0", fg=TEXT, width=12).pack(side="left", padx=8)
        make_btn(row, "NEXT  →", self._confirm,
                 bg=ACCENT, fg="#fff", width=12).pack(side="left", padx=8)

    def _dec(self):
        if self._val.get() > 3:
            self._val.set(self._val.get() - 1)
            self._refresh_dots()

    def _inc(self):
        if self._val.get() < 10:
            self._val.set(self._val.get() + 1)
            self._refresh_dots()

    def _refresh_dots(self):
        """Redraw dot row to match current count."""
        for w in self._dot_frame.winfo_children():
            w.destroy()
        n = self._val.get()
        for i in range(10):
            colour = proc_color(i) if i < n else BORDER
            tk.Frame(self._dot_frame, bg=colour,
                     width=24, height=24).pack(side="left", padx=3)

    def _confirm(self):
        self.ctrl.num_processes = self._val.get()
        self.ctrl.show_input()

# 3.  INPUT VIEW — burst / arrival time entry form

class InputView(BaseView):
    """
    Data-entry table for n processes.
    Enter / Tab auto-advances:  burst[i] → arrival[i] → burst[i+1] → …
    Validation: burst 1–20 ms, arrival 0–20 ms.
    """

    def __init__(self, master, ctrl):
        super().__init__(master, ctrl)
        self._n = ctrl.num_processes
        self._bvars: List[tk.StringVar] = []   # burst time StringVars
        self._avars: List[tk.StringVar] = []   # arrival time StringVars
        self._bents: List[tk.Entry]     = []   # burst time Entry widgets
        self._aents: List[tk.Entry]     = []   # arrival time Entry widgets
        self._build()

    def _build(self):
        make_header(self, f"  ◉  PROCESS INPUT  ({self._n} processes)",
                    "  Burst: 1–20 ms  •  Arrival: 0–20 ms  •  Enter/Tab advances cursor")

        outer = tk.Frame(self, bg=BG)
        outer.pack(fill="both", expand=True, padx=30, pady=12)

        tbl = make_card(outer)
        tbl.pack(fill="x", pady=(0, 10))

        # Column headers
        hdr = tk.Frame(tbl, bg=PANEL, pady=7)
        hdr.pack(fill="x")
        for txt, w in [("  PROCESS", 11),
                       ("BURST TIME (ms)", 22),
                       ("ARRIVAL TIME (ms)", 22)]:
            tk.Label(hdr, text=txt, font=FONT_LABEL, bg=PANEL,
                     fg=ACCENT, width=w, anchor="center").pack(side="left", padx=6)

        # Key-validation callbacks (allow empty or integer within range)
        vcmd_b = (self.register(lambda v: self._valid(v, 1, 20)), "%P")
        vcmd_a = (self.register(lambda v: self._valid(v, 0, 20)), "%P")

        for i in range(self._n):
            pid    = f"P{i+1}"
            colour = proc_color(i)

            row = tk.Frame(tbl, bg=CARD, pady=5)
            row.pack(fill="x", padx=8)

            # Colour swatch strip
            sw = tk.Frame(row, bg=colour, width=6, height=34)
            sw.pack(side="left", padx=(2, 8))
            sw.pack_propagate(False)

            tk.Label(row, text=pid, font=("Segoe UI", 11, "bold"),
                     bg=CARD, fg=colour, width=5, anchor="center").pack(side="left")

            bv = tk.StringVar()
            self._bvars.append(bv)
            be = tk.Entry(row, textvariable=bv, font=("Segoe UI", 11),
                          bg="#f8faff", fg=TEXT, insertbackground=ACCENT,
                          relief="flat", bd=0, width=9,
                          highlightbackground=BORDER, highlightthickness=1,
                          highlightcolor=ACCENT, justify="center",
                          validate="key", validatecommand=vcmd_b)
            be.pack(side="left", padx=(50, 8), ipady=5)
            self._bents.append(be)

            av = tk.StringVar()
            self._avars.append(av)
            ae = tk.Entry(row, textvariable=av, font=("Segoe UI", 11),
                          bg="#f8faff", fg=TEXT, insertbackground=ACCENT,
                          relief="flat", bd=0, width=9,
                          highlightbackground=BORDER, highlightthickness=1,
                          highlightcolor=ACCENT, justify="center",
                          validate="key", validatecommand=vcmd_a)
            ae.pack(side="left", padx=(50, 8), ipady=5)
            self._aents.append(ae)

            if i < self._n - 1:
                tk.Frame(tbl, bg=BORDER, height=1).pack(fill="x", padx=8)

        # Auto-tab bindings: burst[i] → arrival[i] → burst[i+1]
        for i in range(self._n):
            self._bents[i].bind("<Return>",
                lambda e, j=i: self._aents[j].focus_set())
            self._bents[i].bind("<Tab>",
                lambda e, j=i: (self._aents[j].focus_set(), "break"))
            if i < self._n - 1:
                self._aents[i].bind("<Return>",
                    lambda e, j=i: self._bents[j+1].focus_set())
                self._aents[i].bind("<Tab>",
                    lambda e, j=i: (self._bents[j+1].focus_set(), "break"))

        self._bents[0].focus_set()

        tk.Label(outer,
                 text="💡  Enter or Tab moves to the next field automatically.",
                 font=("Segoe UI", 8), bg=BG, fg=SUBTEXT,
                 anchor="w").pack(fill="x", pady=(0, 8))

        row2 = tk.Frame(outer, bg=BG)
        row2.pack(pady=6)
        make_btn(row2, "◀  BACK",      self.ctrl.show_count,
                 bg="#e2e8f0", fg=TEXT, width=12).pack(side="left", padx=8)
        make_btn(row2, "⚙  COMPUTE →", self._submit,
                 bg=ACCENT, fg="#fff", width=14).pack(side="left", padx=8)
        make_btn(row2, "✕  CLEAR",      self._clear,
                 bg="#e2e8f0", fg=TEXT, width=12).pack(side="left", padx=8)

    @staticmethod
    def _valid(v: str, lo: int, hi: int) -> bool:
        """Allow empty string (mid-edit) or integer within [lo, hi]."""
        if v == "":
            return True
        try:
            return lo <= int(v) <= hi
        except ValueError:
            return False

    def _clear(self):
        """Reset all entry fields."""
        for bv, av in zip(self._bvars, self._avars):
            bv.set("")
            av.set("")
        self._bents[0].focus_set()

    def _submit(self):
        """Validate inputs, build Process list, run scheduler, go to TableView."""
        procs: List[Process] = []

        for i in range(self._n):
            b = self._bvars[i].get().strip()
            a = self._avars[i].get().strip()

            if b == "" or a == "":
                messagebox.showerror("Input Error",
                    f"P{i+1}: Both burst time and arrival time are required.")
                return
            try:
                bi, ai = int(b), int(a)
            except ValueError:
                messagebox.showerror("Input Error",
                    f"P{i+1}: Please enter valid integers.")
                return
            if not (1 <= bi <= 20):
                messagebox.showerror("Input Error",
                    f"P{i+1}: Burst time must be between 1 and 20 ms.")
                return
            if not (0 <= ai <= 20):
                messagebox.showerror("Input Error",
                    f"P{i+1}: Arrival time must be between 0 and 20 ms.")
                return

            procs.append(Process(pid=f"P{i+1}", burst=bi, arrival=ai, color=proc_color(i)))

        self.ctrl.processes = procs
        sched = SJFScheduler(procs)
        sched.run()
        self.ctrl.scheduler = sched
        self.ctrl.show_table()

# 4.  TABLE VIEW — input summary before animation

class TableView(BaseView):
    """Displays entered processes in a table so the user can review before animating."""

    def __init__(self, master, ctrl):
        super().__init__(master, ctrl)
        self._build()

    def _build(self):
        make_header(self, "  ◉  INPUT SUMMARY TABLE",
                    "  Review your processes before the animation begins")

        outer = tk.Frame(self, bg=BG)
        outer.pack(fill="both", expand=True, padx=40, pady=18)

        tk.Label(outer, text="PROCESS INPUT SUMMARY",
                 font=("Segoe UI", 11, "bold"),
                 bg=BG, fg=ACCENT2).pack(anchor="w", pady=(0, 8))

        tbl = make_card(outer)
        tbl.pack(fill="x")

        # Column headers
        hdr = tk.Frame(tbl, bg=PANEL, pady=8)
        hdr.pack(fill="x")
        for col_name, col_w in [("Process ID", 16),
                                  ("Burst Time (ms)", 22),
                                  ("Arrival Time (ms)", 22)]:
            tk.Label(hdr, text=col_name, font=FONT_LABEL,
                     bg=PANEL, fg=ACCENT,
                     width=col_w, anchor="center").pack(side="left", pady=4)

        # One data row per process
        for p in self.ctrl.processes:
            row = tk.Frame(tbl, bg=CARD)
            row.pack(fill="x")
            tk.Frame(row, bg=p.color, width=5, height=34).pack(side="left")
            for val, cw in [(p.pid, 16), (f"{p.burst} ms", 22), (f"{p.arrival} ms", 22)]:
                fg = p.color if val == p.pid else TEXT
                tk.Label(row, text=val, font=FONT_BODY, bg=CARD,
                         fg=fg, width=cw, anchor="center").pack(side="left", pady=6)
            tk.Frame(tbl, bg=BORDER, height=1).pack(fill="x")

        hsep(outer, pady=14)

        # Info box explaining what to expect on the animation screen
        info = make_card(outer)
        info.pack(fill="x", pady=(0, 20))
        for line in [
            "  ℹ   One combined animation screen is ready:",
            "      LEFT  — Gantt Chart: horizontal bar grows right, colour-coded by process.",
            "      RIGHT — Process Timeline: active processes are stacked vertically.",
            "               Each block = one active process.  Finished blocks disappear,",
            "               so the stack height changes over time.",
            "      Both sides run simultaneously with a live ms timer.",
        ]:
            tk.Label(info, text=line, font=("Segoe UI", 9), bg=CARD,
                     fg=TEXT, anchor="w", justify="left",
                     pady=3).pack(fill="x", padx=10)

        row2 = tk.Frame(outer, bg=BG)
        row2.pack()
        make_btn(row2, "◀  BACK",      self.ctrl.show_input,
                 bg="#e2e8f0", fg=TEXT, width=12).pack(side="left", padx=8)
        make_btn(row2, "▶  ANIMATE →", self.ctrl.show_anim,
                 bg=SUCCESS, fg="#fff", width=16).pack(side="left", padx=8)


# 5.  ANIM VIEW — Gantt Chart (left) + Process Timeline / Concurrency Diagram (right)

class AnimView(BaseView):
    """
    Combined animation screen.
    Left  — Gantt Chart     : fixed-width, scrollable, colour-coded segments.
    Right — Process Timeline: stacked blocks for active processes; finished
                              processes drop off so the stack height varies.
    """

    def __init__(self, master, ctrl):
        super().__init__(master, ctrl)

        #  animation runtime state 
        self._running  = False   # True while the animation loop is active
        self._aid      = None    # after()-id for the frame loop
        self._ms_aid   = None    # after()-id for the real-time ms counter
        self._ms_count = 0       # real elapsed ms (increments by 100 each call)
        self._tick     = 0       # current logical clock tick (0 … total_time)
        self._sub      = 0       # sub-step within the current tick (0 … SUBSTEPS-1)

        self._build()
        self._prepare()
  
    # UI CONSTRUCTION
  
    def _build(self):
        """Create all widgets.  Called once in __init__."""

        make_header(self,
                    "  ◉  ANIMATION — GANTT CHART  &  PROCESS TIMELINE",
                    "  Both run simultaneously  •  msec timer top-right")

        # Top info bar: status message (left) + ms timer (right)
        top = tk.Frame(self, bg=PANEL, pady=5)
        top.pack(fill="x")
        tk.Frame(top, bg=BORDER, height=1).pack(fill="x", side="bottom")

        self._status_var = tk.StringVar(value="Press  ▶ ANIMATE  to begin")
        tk.Label(top, textvariable=self._status_var,
                 font=("Segoe UI", 10, "bold"),
                 bg=PANEL, fg=WARNING, anchor="w").pack(side="left", padx=16)

        # Timer label — plain tk.Label avoids all canvas repositioning bugs
        self._timer_var = tk.StringVar(value="⏱  0 ms")
        tk.Label(top, textvariable=self._timer_var,
                 font=("Segoe UI", 11, "bold"),
                 bg="#dde4f7", fg=TEXT,
                 relief="flat", padx=10, pady=2).pack(side="right", padx=12, pady=2)

        # Main split: left = Gantt, right = Process Timeline
        split = tk.Frame(self, bg=BG)
        split.pack(fill="both", expand=True, padx=12, pady=6)

        #  LEFT: Gantt Chart
        left_card = make_card(split)
        left_card.pack(side="left", fill="both", expand=True, padx=(0, 5))
        tk.Label(left_card, text="  GANTT CHART",
                 font=FONT_LABEL, bg=CARD, fg=ACCENT,
                 anchor="w").pack(fill="x", padx=8, pady=(6, 0))

        # Canvas + horizontal scrollbar (chart width is fixed, may need scrolling)
        gantt_wrap = tk.Frame(left_card, bg=CARD)
        gantt_wrap.pack(fill="both", expand=True, padx=6, pady=(2, 2))

        self._gcv = tk.Canvas(gantt_wrap, bg="#f8faff",
                              highlightthickness=0, height=110)
        self._gscroll = tk.Scrollbar(gantt_wrap, orient="horizontal",
                                     command=self._gcv.xview)
        self._gcv.configure(xscrollcommand=self._gscroll.set)
        self._gscroll.pack(side="bottom", fill="x")
        self._gcv.pack(side="top", fill="both", expand=True)

        #  RIGHT: Process Timeline (Concurrency Diagram) 
        right_card = make_card(split)
        right_card.pack(side="right", fill="both", expand=True, padx=(5, 0))
        tk.Label(right_card, text="  PROCESS TIMELINE  (Concurrency Diagram)",
                 font=FONT_LABEL, bg=CARD, fg=ACCENT2,
                 anchor="w").pack(fill="x", padx=8, pady=(6, 0))
        self._tcv = tk.Canvas(right_card, bg="#f8faff", highlightthickness=0)
        self._tcv.pack(fill="both", expand=True, padx=6, pady=(2, 6))

        #  Legend strip 
        leg = make_card(self)
        leg.pack(fill="x", padx=12, pady=(0, 4))
        tk.Label(leg, text="  LEGEND:",
                 font=FONT_LABEL, bg=CARD,
                 fg=SUBTEXT).pack(side="left", padx=(8, 10), pady=4)
        for p in self.ctrl.processes:
            tk.Frame(leg, bg=p.color, width=14, height=14).pack(
                side="left", padx=(0, 3), pady=4)
            tk.Label(leg, text=p.pid,
                     font=FONT_LABEL, bg=CARD,
                     fg=p.color).pack(side="left", padx=(0, 10), pady=4)

        #  Control buttons 
        br = tk.Frame(self, bg=BG)
        br.pack(pady=5)
        make_btn(br, "◀  BACK",    self._back,
                 bg="#e2e8f0", fg=TEXT, width=12).pack(side="left", padx=6)
        self._anim_btn = make_btn(br, "▶  ANIMATE", self._start,
                                   bg=SUCCESS, fg="#fff", width=13)
        self._anim_btn.pack(side="left", padx=6)
        make_btn(br, "RESULTS →", self.ctrl.show_results,
                 bg=ACCENT, fg="#fff", width=13).pack(side="left", padx=6)

    # PREPARE — pre-compute tick-by-tick SRTF state

    def _prepare(self):
        procs       = self.ctrl.processes
        ids         = [p.pid for p in procs]
        burst_map   = {p.pid: p.burst   for p in procs}
        arrival_map = {p.pid: p.arrival for p in procs}
        color_map   = {p.pid: p.color   for p in procs}
        remain      = {p.pid: p.burst   for p in procs}

        n     = len(procs)
        t     = 0
        done  = 0
        max_t = sum(burst_map.values()) + max(arrival_map.values()) + 2
        tick_data = []
        finished_set = set()   # pids that have completed

        while done < n and t < max_t:
            # Processes that have arrived and still need CPU time
            ready = [p for p in ids
                     if arrival_map[p] <= t and remain[p] > 0]

            if not ready:
                # All arrived processes are done, or none have arrived yet
                active = [p for p in ids
                          if arrival_map[p] <= t and p not in finished_set]
                tick_data.append({'running': None, 'finished': None, 'active': active})
                t += 1
                continue

            # SRTF: pick the process with the least remaining burst time
            chosen   = min(ready, key=lambda p: remain[p])
            remain[chosen] -= 1

            finished = None
            if remain[chosen] == 0:
                finished = chosen
                finished_set.add(chosen)
                done += 1

            # active = arrived AND not yet finished at the START of this tick
            # (we include 'finished' in active for this tick — it was still running)
            active = [p for p in ids
                      if arrival_map[p] <= t and p not in (finished_set - {finished})]

            tick_data.append({'running': chosen, 'finished': finished, 'active': active})
            t += 1

        self._tick_data  = tick_data
        self._total_time = len(tick_data)
        self._color_map  = color_map
        self._burst_map  = burst_map
        self._pid_list   = ids

    # ANIMATION CONTROL

    def _back(self):
        """Stop animation and return to the input summary table."""
        self._stop_all()
        self.ctrl.show_table()

    def _stop_all(self):
        """Cancel any running after() loops."""
        self._running = False
        for attr in ("_aid", "_ms_aid"):
            aid = getattr(self, attr, None)
            if aid:
                try:
                    self.after_cancel(aid)
                except Exception:
                    pass
        self._aid = self._ms_aid = None

    def _start(self):
        """Start (or replay) the animation from tick 0."""
        if self._running:
            return

        self._stop_all()

        # Reset counters
        self._tick     = 0
        self._sub      = 0
        self._ms_count = 0
        self._timer_var.set("⏱  0 ms")

        # Gantt drawing state:
        #   _g_segs : closed segments  [(pid, colour, t_start, t_end), …]
        #   _g_cur  : currently growing segment  [pid, colour, t_start] or None
        self._g_segs = []
        self._g_cur  = None

        # Clear both canvases
        self._gcv.delete("all")
        self._tcv.delete("all")

        self._running = True
        self._anim_btn.config(state="disabled", text="ANIMATING…")
        self._status_var.set("Running…")

        # Start both loops
        self._ms_tick()
        self._do_frame()

    # TIMER LOOP — increments the ⏱ label every 100 real ms

    def _ms_tick(self):
        """Increment ms counter by 100 every 100 real ms, display capped at 9999."""
        if not self._running:
            return
        self._ms_count += 100
        self._timer_var.set(f"⏱  {min(self._ms_count, 9999)} ms")
        self._ms_aid = self.after(100, self._ms_tick)

    # FRAME LOOP — drives both Gantt chart and Process Timeline
 
    def _do_frame(self):

        if not self._running:
            return

        # On the first sub-step of each tick: advance logical state
        if self._sub == 0:

            if self._tick >= self._total_time:
                self._finish()
                return

            info     = self._tick_data[self._tick]
            running  = info['running']
            finished = info['finished']

            # Update Gantt segment list
            if running:
                if self._g_cur and self._g_cur[0] == running:
                    pass  # same process continues — drawing handles it
                else:
                    # Close previous segment (if any) and open a new one
                    if self._g_cur:
                        pid, col, ts = self._g_cur
                        self._g_segs.append((pid, col, ts, self._tick))
                    self._g_cur = [running, self._color_map[running], self._tick]
            else:
                # CPU idle — close open segment
                if self._g_cur:
                    pid, col, ts = self._g_cur
                    self._g_segs.append((pid, col, ts, self._tick))
                    self._g_cur = None

            # Update status label
            msg = f"t = {self._tick}"
            if running:  msg += f"   Running: {running}"
            if finished: msg += f"   ✔ {finished} finished"
            self._status_var.set(msg)

            self._tick += 1

        # Redraw both canvases on every frame
        self._draw_gantt()
        self._draw_timeline()

        self._sub = (self._sub + 1) % SUBSTEPS
        self._aid = self.after(FRAME_MS, self._do_frame)

    def _finish(self):
        """Called once all ticks are processed.  Seals final segment and updates UI."""
        self._running = False

        if self._ms_aid:
            try:
                self.after_cancel(self._ms_aid)
            except Exception:
                pass

        # Seal any still-open Gantt segment
        if self._g_cur:
            pid, col, ts = self._g_cur
            self._g_segs.append((pid, col, ts, self._total_time))
            self._g_cur = None

        # Final draw at 100% completion
        self._draw_gantt()
        self._draw_timeline()

        self._status_var.set(
            "✔  Animation complete!  →  Click  'RESULTS →'  to see solutions.")
        self._anim_btn.config(state="normal", text="⟳  REPLAY",
                               command=self._start)


    # DRAW — GANTT CHART
    def _draw_gantt(self):
  
        cv = self._gcv
        cv.update_idletasks()
        ch = max(cv.winfo_height(), 80)

        # Geometry
        MARGIN_L = 40    # px left of t=0 (room for "CPU" label)
        MARGIN_R = 20    # px right of the final tick mark
        BAR_Y0   = 18    # top of the Gantt bar
        BAR_Y1   = ch - 36   # bottom of the Gantt bar
        if BAR_Y1 <= BAR_Y0:
            BAR_Y1 = BAR_Y0 + 30

        total   = self._total_time or 1
        chart_w = MARGIN_L + total * PX_PER_TICK + MARGIN_R

        # Tell the scrollbar the full virtual width
        cv.configure(scrollregion=(0, 0, chart_w, ch))
        cv.delete("all")

        # Time axis line
        cv.create_line(MARGIN_L - 2, BAR_Y1 + 2,
                       chart_w - MARGIN_R, BAR_Y1 + 2,
                       fill=BORDER, width=1)

        # "CPU" label on the left
        cv.create_text(MARGIN_L - 4, (BAR_Y0 + BAR_Y1) // 2,
                       text="CPU", font=("Courier New", 8, "bold"),
                       fill=SUBTEXT, anchor="e")

        def _seg(pid, col, ts, te, active=False):
            """Draw one Gantt segment from ts to te (te may be a float)."""
            x0 = MARGIN_L + ts * PX_PER_TICK
            x1 = MARGIN_L + te * PX_PER_TICK
            if x1 - x0 < 1:
                x1 = x0 + 1   # always at least 1 px wide

            border = ACCENT if active else "#ffffff"
            bwidth = 2      if active else 1
            cv.create_rectangle(x0, BAR_Y0, x1, BAR_Y1,
                                 fill=col, outline=border, width=bwidth)

            # PID label (only if segment is wide enough)
            if (x1 - x0) > 18:
                fg = "#ffffff" if self._is_dark(col) else TEXT
                cv.create_text((x0 + x1) / 2, (BAR_Y0 + BAR_Y1) / 2,
                               text=pid, font=("Segoe UI", 8, "bold"), fill=fg)

            # Timestamp at the left edge of this segment
            cv.create_text(x0, BAR_Y1 + 14, text=str(int(ts)),
                           font=("Courier New", 7), fill=SUBTEXT, anchor="n")

        # Draw all completed (closed) segments
        for (pid, col, ts, te) in self._g_segs:
            _seg(pid, col, ts, te, active=False)

        # Draw the currently growing segment with smooth fractional right edge
        if self._g_cur:
            pid, col, ts = self._g_cur
            te_float = (self._tick - 1) + (self._sub / SUBSTEPS)
            _seg(pid, col, ts, te_float, active=True)

        # Draw the final end-time label once animation is complete
        if self._tick >= self._total_time and not self._g_cur:
            x_end = MARGIN_L + self._total_time * PX_PER_TICK
            cv.create_text(x_end, BAR_Y1 + 14, text=str(self._total_time),
                           font=("Courier New", 7), fill=SUBTEXT, anchor="n")

        # Auto-scroll so the growing edge stays visible
        if self._g_cur or self._tick < self._total_time:
            cur_t     = (self._tick - 1) + (self._sub / SUBSTEPS)
            edge_x    = MARGIN_L + cur_t * PX_PER_TICK + 20
            visible_w = cv.winfo_width()
            left_frac = max(0.0, (edge_x - visible_w) / max(chart_w, 1))
            cv.xview_moveto(left_frac)


    # DRAW — PROCESS TIMELINE (Concurrency / State Diagram)
    def _draw_timeline(self):
        cv = self._tcv
        cv.update_idletasks()
        cw = max(cv.winfo_width(),  80)
        ch = max(cv.winfo_height(), 40)

        cv.delete("all")

        # ── current tick state ────────────────────────────────────
        cur_t = self._tick - 1   # the tick we just processed (0-based)
        if cur_t < 0 or cur_t >= len(self._tick_data):
            # Nothing to show yet
            cv.create_text(cw // 2, ch // 2, text="—",
                           font=("Segoe UI", 12), fill=SUBTEXT, anchor="center")
            return

        info    = self._tick_data[cur_t]
        running = info['running']    # pid running this tick (or None)
        active  = info['active']     # list of active pids this tick

        #  layout constants
        PAD       = 18      # outer padding (all sides)
        AXIS_H    = 26      # height of the time-axis label area at the top
        GAP       = 4       # gap between adjacent layer rectangles
        MIN_BH    = 28      # minimum block height in pixels
        BORDER_W  = 3       # border width for the running-process highlight

        # Available height for the stack of blocks
        stack_top    = PAD + AXIS_H
        stack_bottom = ch - PAD
        stack_h      = max(1, stack_bottom - stack_top)

        #  time-axis label 
        cv.create_text(PAD, PAD + AXIS_H // 2,
                       text=f"t = {cur_t}",
                       font=("Segoe UI", 9, "bold"),
                       fill=SUBTEXT, anchor="w")

        #  idle state 
        if not active:
            cv.create_rectangle(PAD, stack_top, cw - PAD, stack_bottom,
                                 fill="#f1f5f9", outline=BORDER, width=1)
            cv.create_text(cw // 2, (stack_top + stack_bottom) // 2,
                           text="Idle / Waiting",
                           font=("Segoe UI", 10), fill=SUBTEXT, anchor="center")
            return

        #  compute block height 
        n  = len(active)
        # Total gap space between blocks
        total_gap = GAP * (n - 1)
        block_h   = max(MIN_BH, (stack_h - total_gap) // n)

        for i, pid in enumerate(reversed(active)):
            colour = self._color_map[pid]
            is_running = (pid == running)

            # Vertical position: blocks fill from bottom upward
            block_top = stack_bottom - (i + 1) * block_h - i * GAP
            block_bot = block_top + block_h

            # Background fill
            cv.create_rectangle(PAD, block_top, cw - PAD, block_bot,
                                 fill=colour,
                                 outline=ACCENT if is_running else "#ffffff",
                                 width=BORDER_W if is_running else 1)

            # PID label centred in the block
            label_fg = "#ffffff" if self._is_dark(colour) else TEXT
            cv.create_text((PAD + cw - PAD) // 2,
                           (block_top + block_bot) // 2,
                           text=pid + ("  ◀ CPU" if is_running else ""),
                           font=("Segoe UI", 9, "bold"),
                           fill=label_fg, anchor="center")

    # UTILITY HELPERS
    
    @staticmethod
    def _is_dark(hex_color: str) -> bool:
        """
        Return True if the colour is perceptually dark (ITU-R BT.601 luma).
        Used to decide whether to draw white or dark text on top of a block.
        """
        h = hex_color.lstrip("#")
        r, g, b = (int(h[i:i+2], 16) for i in (0, 2, 4))
        return (r * 0.299 + g * 0.587 + b * 0.114) < 150


# 6.  RESULTS VIEW — scrollable step-by-step solution page

class ResultsView(BaseView):
    def __init__(self, master, ctrl):
        super().__init__(master, ctrl)
        self._build()

    def _build(self):
        make_header(self, "  ◉  RESULTS & STEP-BY-STEP SOLUTIONS",
                    "  Waiting Time  •  Completion Time  •  Turnaround Time")

        outer = tk.Frame(self, bg=BG)
        outer.pack(fill="both", expand=True)
        _, inner = make_scroll_frame(outer)

        c = tk.Frame(inner, bg=BG)
        c.pack(fill="both", expand=True, padx=28, pady=10)

        sched   = self.ctrl.scheduler
        results = sched.processes

        def section(title: str):
            """Draw a coloured section heading with a horizontal rule."""
            tk.Label(c, text=title,
                     font=("Segoe UI", 11, "bold"),
                     bg=BG, fg=ACCENT2).pack(anchor="w", pady=(14, 3))
            tk.Frame(c, bg=BORDER, height=1).pack(fill="x")

        def avg_card(lines):
            """Grey card for average calculation display."""
            f = make_card(c)
            f.pack(fill="x", pady=4)
            for txt, highlight in lines:
                tk.Label(f, text=txt, font=FONT_MONO, bg=CARD,
                         fg=WARNING if highlight else SUBTEXT,
                         anchor="w", justify="left",
                         padx=14, pady=2).pack(fill="x")

        def proc_card(p, lines):
            """Coloured card with a left-border swatch for one process."""
            f = tk.Frame(c, bg=CARD,
                         highlightbackground=p.color,
                         highlightthickness=1)
            f.pack(fill="x", pady=3)
            tk.Frame(f, bg=p.color, width=5).pack(side="left", fill="y")
            inn = tk.Frame(f, bg=CARD)
            inn.pack(side="left", fill="x", expand=True)
            for txt, highlight in lines:
                tk.Label(inn, text=txt, font=FONT_MONO, bg=CARD,
                         fg=WARNING if highlight else TEXT,
                         anchor="w", justify="left",
                         padx=10, pady=1).pack(fill="x")

        # WAITING TIME 
        section("① PROCESS WAITING TIME")
        tk.Label(c, text="  Formula:  WT = TAT − BT  =  (CT − AT) − BT",
                 font=FONT_MONO, bg=BG, fg=SUBTEXT, anchor="w").pack(fill="x")
        for p in results:
            proc_card(p, [
                (f"  {p.pid}:  CT={p.completion}  AT={p.arrival}  BT={p.burst}", False),
                (f"       TAT = CT − AT = {p.completion} − {p.arrival} = {p.turnaround}", False),
                (f"       WT  = TAT − BT = {p.turnaround} − {p.burst} = {p.waiting} ms  ✓", True),
            ])
        avg_card([
            (f"  Avg WT = ({' + '.join(str(p.waiting) for p in results)}) ÷ {len(results)}", False),
            (f"         = {sum(p.waiting for p in results)} ÷ {len(results)}"
             f" = {sched.avg_waiting:.2f} ms  ✓", True),
        ])

        # COMPLETION TIME 
        section("② PROCESS COMPLETION TIME")
        tk.Label(c, text="  Formula:  CT = the time unit at which the process finishes execution",
                 font=FONT_MONO, bg=BG, fg=SUBTEXT, anchor="w").pack(fill="x")
        for p in results:
            proc_card(p, [
                (f"  {p.pid}: finishes at t = {p.completion}"
                 f"  →  CT = {p.completion} ms  ✓", True),
            ])
        avg_card([
            (f"  Avg CT = ({' + '.join(str(p.completion) for p in results)}) ÷ {len(results)}", False),
            (f"         = {sum(p.completion for p in results)} ÷ {len(results)}"
             f" = {sched.avg_completion:.2f} ms  ✓", True),
        ])

        #  TURNAROUND TIME 
        section("③ PROCESS TURNAROUND TIME")
        tk.Label(c, text="  Formula:  TAT = CT − AT",
                 font=FONT_MONO, bg=BG, fg=SUBTEXT, anchor="w").pack(fill="x")
        for p in results:
            proc_card(p, [
                (f"  {p.pid}: TAT = {p.completion} − {p.arrival}"
                 f" = {p.turnaround} ms  ✓", True),
            ])
        avg_card([
            (f"  Avg TAT = ({' + '.join(str(p.turnaround) for p in results)}) ÷ {len(results)}", False),
            (f"          = {sum(p.turnaround for p in results)} ÷ {len(results)}"
             f" = {sched.avg_turnaround:.2f} ms  ✓", True),
        ])

        #  AVERAGES SUMMARY 
        section("④ SUMMARY OF AVERAGES")
        af = make_card(c)
        af.pack(fill="x", pady=6)
        for lbl, val in [
            ("Average Waiting Time",    f"{sched.avg_waiting:.2f} ms"),
            ("Average Completion Time", f"{sched.avg_completion:.2f} ms"),
            ("Average Turnaround Time", f"{sched.avg_turnaround:.2f} ms"),
        ]:
            row = tk.Frame(af, bg=CARD)
            row.pack(fill="x", padx=14, pady=5)
            tk.Label(row, text=lbl, font=FONT_BODY,
                     bg=CARD, fg=TEXT, anchor="w").pack(side="left")
            tk.Label(row, text=val, font=("Segoe UI", 11, "bold"),
                     bg=CARD, fg=SUCCESS, anchor="e").pack(side="right")

        #  FINAL RESULTS TABLE 
        section("⑤ FINAL RESULTS TABLE")

        cols = [
            ("Process",           9),
            ("Burst\nTime",      10),
            ("Arrival\nTime",    10),
            ("Completion\nTime", 13),
            ("Waiting\nTime",    12),
            ("Turnaround\nTime", 14),
        ]

        ftbl = make_card(c)
        ftbl.pack(fill="x", pady=6)

        # Header row
        hdr_r = tk.Frame(ftbl, bg=PANEL)
        hdr_r.pack(fill="x")
        for col_name, col_w in cols:
            tk.Label(hdr_r, text=col_name, font=FONT_LABEL,
                     bg=PANEL, fg=ACCENT,
                     width=col_w, anchor="center",
                     justify="center").pack(side="left", pady=6, padx=2)

        # Data rows
        for p in results:
            dr = tk.Frame(ftbl, bg=CARD)
            dr.pack(fill="x")
            tk.Frame(dr, bg=p.color, width=4, height=30).pack(side="left")
            values = [p.pid, f"{p.burst} ms", f"{p.arrival} ms",
                      f"{p.completion} ms", f"{p.waiting} ms", f"{p.turnaround} ms"]
            for val, (_, col_w) in zip(values, cols):
                fg = p.color if val == p.pid else TEXT
                tk.Label(dr, text=val, font=FONT_BODY, bg=CARD,
                         fg=fg, width=col_w,
                         anchor="center").pack(side="left", pady=5, padx=2)
            tk.Frame(ftbl, bg=BORDER, height=1).pack(fill="x")

        # Average row (highlighted background)
        avg_r = tk.Frame(ftbl, bg="#eef2ff")
        avg_r.pack(fill="x")
        avg_values = ["AVERAGE", "—", "—",
                      f"{sched.avg_completion:.2f} ms",
                      f"{sched.avg_waiting:.2f} ms",
                      f"{sched.avg_turnaround:.2f} ms"]
        for val, (_, col_w) in zip(avg_values, cols):
            tk.Label(avg_r, text=val, font=FONT_LABEL, bg="#eef2ff",
                     fg=SUCCESS, width=col_w,
                     anchor="center").pack(side="left", pady=6, padx=2)

        # Navigation buttons
        nav = tk.Frame(c, bg=BG)
        nav.pack(pady=14)
        make_btn(nav, "◀  ANIMATION",  self.ctrl.show_anim,
                 bg="#e2e8f0", fg=TEXT, width=14).pack(side="left", padx=8)
        make_btn(nav, "⟳  NEW SESSION", self.ctrl.show_intro,
                 bg=ACCENT, fg="#fff", width=14).pack(side="left", padx=8)
        make_btn(nav, "✕  EXIT",         self.ctrl.quit,
                 bg=DANGER, fg="#fff", width=12).pack(side="left", padx=8)       
        self._draw_bars()

        # Advance sub-step; wrap around after SUBSTEPS
        self._sub = (self._sub + 1) % SUBSTEPS
        self._aid  = self.after(FRAME_MS, self._do_frame)

    def _finish(self):
  
        self._running = False

        # Stop the ms timer
        if self._ms_aid:
            try:
                self.after_cancel(self._ms_aid)
            except Exception:
                pass

        # Seal any still-open Gantt segment
        if self._g_cur:
            pid, col, ts = self._g_cur
            self._g_segs.append((pid, col, ts, self._total_time))
            self._g_cur = None

        # Final fully-extended draw
        self._draw_gantt()
        self._draw_bars()

        self._status_var.set(
            "✔  Animation complete!  →  Click  'RESULTS →'  to see solutions.")
        self._anim_btn.config(state="normal", text="⟳  REPLAY",
                               command=self._start)

    # DRAW — GANTT CHART

    def _draw_gantt(self):

        cv = self._gcv
        cv.update_idletasks()
        ch = max(cv.winfo_height(), 80)   # canvas height in pixels

        #  geometry constants 
        MARGIN_L = 40    # px left of t=0 (room for "CPU" label)
        MARGIN_R = 20    # px right of final tick mark
        BAR_Y0   = 18    # top of the Gantt bar
        BAR_Y1   = ch - 36  # bottom of the Gantt bar
        if BAR_Y1 <= BAR_Y0:
            BAR_Y1 = BAR_Y0 + 30

        # Total virtual canvas width (fixed — independent of window size)
        total     = self._total_time or 1
        chart_w   = MARGIN_L + total * PX_PER_TICK + MARGIN_R

        # Update scroll region so the scrollbar knows the full extent
        cv.configure(scrollregion=(0, 0, chart_w, ch))

        cv.delete("all")

        #  time axis line 
        cv.create_line(MARGIN_L - 2, BAR_Y1 + 2,
                       chart_w - MARGIN_R, BAR_Y1 + 2,
                       fill=BORDER, width=1)

        # "CPU" row label on the left
        cv.create_text(MARGIN_L - 4, (BAR_Y0 + BAR_Y1) // 2,
                       text="CPU", font=("Courier New", 8, "bold"),
                       fill=SUBTEXT, anchor="e")

        #  helper: draw one segment rectangle
        def _seg(pid, col, ts, te, active=False):
            x0 = MARGIN_L + ts  * PX_PER_TICK
            x1 = MARGIN_L + te  * PX_PER_TICK
            if x1 - x0 < 1:
                x1 = x0 + 1   # always at least 1 px wide

            border = ACCENT if active else "#ffffff"
            bwidth = 2      if active else 1
            cv.create_rectangle(x0, BAR_Y0, x1, BAR_Y1,
                                 fill=col, outline=border, width=bwidth)

            # PID label: only if segment is wide enough to fit text
            if (x1 - x0) > 18:
                fg = "#ffffff" if self._is_dark(col) else TEXT
                cv.create_text((x0 + x1) / 2, (BAR_Y0 + BAR_Y1) / 2,
                               text=pid,
                               font=("Segoe UI", 8, "bold"), fill=fg)

            # Timestamp at the LEFT edge of this segment (below the bar)
            cv.create_text(x0, BAR_Y1 + 14, text=str(int(ts)),
                           font=("Courier New", 7), fill=SUBTEXT, anchor="n")

        #  draw all completed (sealed) segments 
        for (pid, col, ts, te) in self._g_segs:
            _seg(pid, col, ts, te, active=False)

        #  draw the current growing segment 
        if self._g_cur:
            pid, col, ts = self._g_cur
            # Fractional right edge: the segment started at ts,
            # self._tick is already the NEXT tick (incremented),
            # so the current position is (tick - 1) + fraction through sub-step.
            te_float = (self._tick - 1) + (self._sub / SUBSTEPS)
            _seg(pid, col, ts, te_float, active=True)

        # ── final end-time timestamp (only when animation is done) ─
        if self._tick >= self._total_time and not self._g_cur:
            x_end = MARGIN_L + self._total_time * PX_PER_TICK
            cv.create_text(x_end, BAR_Y1 + 14,
                           text=str(self._total_time),
                           font=("Courier New", 7), fill=SUBTEXT, anchor="n")

        # Auto-scroll the Gantt so the growing edge is always visible
        if self._g_cur or self._tick < self._total_time:
            # Scroll to show the rightmost drawn position
            cur_t   = (self._tick - 1) + (self._sub / SUBSTEPS)
            edge_x  = MARGIN_L + cur_t * PX_PER_TICK + 20
            fraction = edge_x / max(chart_w, 1)
            # moveto() takes the left-fraction of the visible region
            visible_w  = cv.winfo_width()
            left_frac  = max(0.0, (edge_x - visible_w) / max(chart_w, 1))
            cv.xview_moveto(left_frac)

    # DRAW — BAR GRAPH (single horizontal bar per process)

    def _draw_bars(self):
    
        cv = self._bcv
        cv.update_idletasks()
        cw = max(cv.winfo_width(),  80)
        ch = max(cv.winfo_height(), 40)

        cv.delete("all")

        n = len(self._pid_list)
        if n == 0:
            return

        #  vertical layout 
        PAD_TOP    = 16     # px above the first row
        PAD_BOTTOM = 16     # px below the last row
        usable_h   = ch - PAD_TOP - PAD_BOTTOM
        row_h      = max(22, usable_h // n)    # px per process row
        bar_h      = max(14, int(row_h * 0.55))# px thickness of the bar

        #  horizontal layout 
        PID_W    = 38     # px for the PID label column on the left
        STAT_W   = 72     # px for the "ran/total" label column on the right
        PAD_H    = 10     # left/right padding inside the bar area
        bar_x0   = PID_W + PAD_H          # left edge of bar track
        bar_xmax = cw - STAT_W - PAD_H    # right edge of bar track
        bar_area = max(10, bar_xmax - bar_x0)   # total bar track width in px

        for i, pid in enumerate(self._pid_list):
            colour      = self._color_map[pid]
            burst_total = self._burst_map[pid]    # total burst time (ms)
            ticks_done  = self._bar_ticks[pid]    # ticks accumulated so far
            is_done     = pid in self._bar_done   # has this process finished?

            # Vertical centre of this row
            row_cy = PAD_TOP + i * row_h + row_h // 2
            bar_y0 = row_cy - bar_h // 2
            bar_y1 = bar_y0 + bar_h

            #  PID label (left column) 
            cv.create_text(PID_W - 4, row_cy,
                           text=pid,
                           font=("Segoe UI", 9, "bold"),
                           fill=colour, anchor="e")

            #  background track (the "empty" part of the bar) 
            # Drawn as a light rectangle spanning the full bar area
            cv.create_rectangle(bar_x0, bar_y0, bar_xmax, bar_y1,
                                 fill="#e2e8f0",   # light grey track
                                 outline=BORDER, width=1)

            #  filled portion (grows left → right) 
            # For the currently running process, add a fractional sub-step
            # advance so the right edge moves on every frame, not every tick.
            if not is_done:
                # Determine if this pid is currently the running process
                last_t   = self._tick - 1
                cur_run  = (self._tick_data[last_t]['running'] == pid
                            if 0 <= last_t < len(self._tick_data) else False)
                extra    = (self._sub / SUBSTEPS) if cur_run else 0.0
            else:
                extra = 0.0   # finished → no further growth needed

            # Fraction of burst time completed (clamped 0–1)
            fraction = min(1.0, (ticks_done + extra) / max(1, burst_total))
            fill_px  = int(fraction * bar_area)   # filled width in pixels

            if fill_px > 0:
                fx1 = bar_x0 + fill_px
                # Main filled bar — solid, flat, single colour
                cv.create_rectangle(bar_x0, bar_y0, fx1, bar_y1,
                                     fill=colour,
                                     outline="",    # no inner border
                                     width=0)

                # Thin bright leading-edge highlight (shows where growth is)
                # Only shown while the process is still running (not done)
                if not is_done and fill_px > 4:
                    tip = max(2, fill_px // 12)   # narrow bright tip
                    cv.create_rectangle(fx1 - tip, bar_y0, fx1, bar_y1,
                                         fill=self._lighten(colour, 90),
                                         outline="", width=0)

            # ── status label (right column) ───────────────────────
            if is_done:
                label_text   = f"✔ {burst_total} ms"
                label_colour = SUCCESS
            else:
                label_text   = f"{ticks_done} / {burst_total} ms"
                label_colour = colour

            cv.create_text(bar_xmax + 6, row_cy,
                           text=label_text,
                           font=("Segoe UI", 8, "bold"),
                           fill=label_colour, anchor="w")

            # ── thin row separator ────────────────────────────────
            # Drawn between rows (not after the last one)
            if i < n - 1:
                sep_y = PAD_TOP + (i + 1) * row_h
                cv.create_line(8, sep_y, cw - 8, sep_y,
                               fill="#e8edf5", width=1)

    # UTILITY HELPERS
    @staticmethod
    def _is_dark(hex_color: str) -> bool:
        """
        Return True if the colour is perceptually dark.
        Uses the ITU-R BT.601 luma formula so we can decide
        whether to put white or dark text on top.
        """
        h = hex_color.lstrip("#")
        r, g, b = (int(h[i:i+2], 16) for i in (0, 2, 4))
        return (r * 0.299 + g * 0.587 + b * 0.114) < 150

    @staticmethod
    def _lighten(hex_color: str, amount: int = 55) -> str:
        """
        Return a lighter version of hex_color.
        Each RGB channel is raised by `amount`, clamped to 255.
        Used for the leading-edge highlight on the bar tip.
        """
        h = hex_color.lstrip("#")
        r, g, b = (int(h[i:i+2], 16) for i in (0, 2, 4))
        return "#{:02x}{:02x}{:02x}".format(
            min(255, r + amount),
            min(255, g + amount),
            min(255, b + amount),
        )

class ResultsView(BaseView):

    def __init__(self, master, ctrl):
        super().__init__(master, ctrl)
        self._build()

    def _build(self):
        make_header(self, "  ◉  RESULTS & STEP-BY-STEP SOLUTIONS",
                    "  Waiting Time  •  Completion Time  •  Turnaround Time")

        # Scrollable wrapper so the content can exceed the window height
        outer = tk.Frame(self, bg=BG)
        outer.pack(fill="both", expand=True)
        _, inner = make_scroll_frame(outer)

        # All content goes inside this frame (which scrolls)
        c = tk.Frame(inner, bg=BG)
        c.pack(fill="both", expand=True, padx=28, pady=10)

        sched   = self.ctrl.scheduler
        results = sched.processes   # list of Process with filled metrics

        # ── helper: section heading ───────────────────────────────
        def section(title: str):
            tk.Label(c, text=title,
                     font=("Segoe UI", 11, "bold"),
                     bg=BG, fg=ACCENT2).pack(anchor="w", pady=(14, 3))
            tk.Frame(c, bg=BORDER, height=1).pack(fill="x")

        # ── helper: average result card ───────────────────────────
        def avg_card(lines):
            """Grey card showing the average calculation."""
            f = make_card(c)
            f.pack(fill="x", pady=4)
            for txt, highlight in lines:
                tk.Label(f, text=txt, font=FONT_MONO, bg=CARD,
                         fg=WARNING if highlight else SUBTEXT,
                         anchor="w", justify="left",
                         padx=14, pady=2).pack(fill="x")

        # ── helper: per-process result card ───────────────────────
        def proc_card(p, lines):
            """Coloured card (left border = process colour) for one process."""
            f = tk.Frame(c, bg=CARD,
                         highlightbackground=p.color,
                         highlightthickness=1)
            f.pack(fill="x", pady=3)
            # Left colour accent bar
            tk.Frame(f, bg=p.color, width=5).pack(side="left", fill="y")
            inn = tk.Frame(f, bg=CARD)
            inn.pack(side="left", fill="x", expand=True)
            for txt, highlight in lines:
                tk.Label(inn, text=txt, font=FONT_MONO, bg=CARD,
                         fg=WARNING if highlight else TEXT,
                         anchor="w", justify="left",
                         padx=10, pady=1).pack(fill="x")

        # ── ① WAITING TIME ────────────────────────────────────────
        section("① PROCESS WAITING TIME")
        tk.Label(c,
                 text="  Formula:  WT = TAT − BT  =  (CT − AT) − BT",
                 font=FONT_MONO, bg=BG, fg=SUBTEXT,
                 anchor="w").pack(fill="x")
        for p in results:
            proc_card(p, [
                (f"  {p.pid}:  CT={p.completion}  AT={p.arrival}  BT={p.burst}", False),
                (f"       TAT = CT − AT = {p.completion} − {p.arrival} = {p.turnaround}", False),
                (f"       WT  = TAT − BT = {p.turnaround} − {p.burst} = {p.waiting} ms  ✓", True),
            ])
        avg_card([
            (f"  Avg WT = ({' + '.join(str(p.waiting) for p in results)}) ÷ {len(results)}", False),
            (f"         = {sum(p.waiting for p in results)} ÷ {len(results)}"
             f" = {sched.avg_waiting:.2f} ms  ✓", True),
        ])

        # ── ② COMPLETION TIME ─────────────────────────────────────
        section("② PROCESS COMPLETION TIME")
        tk.Label(c,
                 text="  Formula:  CT = the time unit at which the process finishes execution",
                 font=FONT_MONO, bg=BG, fg=SUBTEXT,
                 anchor="w").pack(fill="x")
        for p in results:
            proc_card(p, [
                (f"  {p.pid}: finishes at t = {p.completion}"
                 f"  →  CT = {p.completion} ms  ✓", True),
            ])
        avg_card([
            (f"  Avg CT = ({' + '.join(str(p.completion) for p in results)}) ÷ {len(results)}", False),
            (f"         = {sum(p.completion for p in results)} ÷ {len(results)}"
             f" = {sched.avg_completion:.2f} ms  ✓", True),
        ])

        # ── ③ TURNAROUND TIME ─────────────────────────────────────
        section("③ PROCESS TURNAROUND TIME")
        tk.Label(c,
                 text="  Formula:  TAT = CT − AT",
                 font=FONT_MONO, bg=BG, fg=SUBTEXT,
                 anchor="w").pack(fill="x")
        for p in results:
            proc_card(p, [
                (f"  {p.pid}: TAT = {p.completion} − {p.arrival}"
                 f" = {p.turnaround} ms  ✓", True),
            ])
        avg_card([
            (f"  Avg TAT = ({' + '.join(str(p.turnaround) for p in results)}) ÷ {len(results)}", False),
            (f"          = {sum(p.turnaround for p in results)} ÷ {len(results)}"
             f" = {sched.avg_turnaround:.2f} ms  ✓", True),
        ])

        # ── ④ AVERAGES SUMMARY ────────────────────────────────────
        section("④ SUMMARY OF AVERAGES")
        af = make_card(c)
        af.pack(fill="x", pady=6)
        for lbl, val in [
            ("Average Waiting Time",    f"{sched.avg_waiting:.2f} ms"),
            ("Average Completion Time", f"{sched.avg_completion:.2f} ms"),
            ("Average Turnaround Time", f"{sched.avg_turnaround:.2f} ms"),
        ]:
            row = tk.Frame(af, bg=CARD)
            row.pack(fill="x", padx=14, pady=5)
            tk.Label(row, text=lbl,  font=FONT_BODY,
                     bg=CARD, fg=TEXT, anchor="w").pack(side="left")
            tk.Label(row, text=val,  font=("Segoe UI", 11, "bold"),
                     bg=CARD, fg=SUCCESS, anchor="e").pack(side="right")

        # ── ⑤ FINAL RESULTS TABLE ─────────────────────────────────
        section("⑤ FINAL RESULTS TABLE")

        # Column definitions: (header text, column width in chars)
        cols = [
            ("Process",         9),
            ("Burst\nTime",    10),
            ("Arrival\nTime",  10),
            ("Completion\nTime", 13),
            ("Waiting\nTime",  12),
            ("Turnaround\nTime", 14),
        ]

        ftbl = make_card(c)
        ftbl.pack(fill="x", pady=6)

        # Header row
        hdr_r = tk.Frame(ftbl, bg=PANEL)
        hdr_r.pack(fill="x")
        for col_name, col_w in cols:
            tk.Label(hdr_r, text=col_name, font=FONT_LABEL,
                     bg=PANEL, fg=ACCENT,
                     width=col_w, anchor="center",
                     justify="center").pack(side="left", pady=6, padx=2)

        # Data rows
        for p in results:
            dr = tk.Frame(ftbl, bg=CARD)
            dr.pack(fill="x")
            # Left colour swatch
            tk.Frame(dr, bg=p.color, width=4, height=30).pack(side="left")
            # Data cells
            values = [
                p.pid,
                f"{p.burst} ms",
                f"{p.arrival} ms",
                f"{p.completion} ms",
                f"{p.waiting} ms",
                f"{p.turnaround} ms",
            ]
            for val, (_, col_w) in zip(values, cols):
                fg = p.color if val == p.pid else TEXT
                tk.Label(dr, text=val, font=FONT_BODY, bg=CARD,
                         fg=fg, width=col_w,
                         anchor="center").pack(side="left", pady=5, padx=2)
            tk.Frame(ftbl, bg=BORDER, height=1).pack(fill="x")

        # Average row (highlighted background)
        avg_r = tk.Frame(ftbl, bg="#eef2ff")
        avg_r.pack(fill="x")
        avg_values = [
            "AVERAGE", "—", "—",
            f"{sched.avg_completion:.2f} ms",
            f"{sched.avg_waiting:.2f} ms",
            f"{sched.avg_turnaround:.2f} ms",
        ]
        for val, (_, col_w) in zip(avg_values, cols):
            tk.Label(avg_r, text=val, font=FONT_LABEL, bg="#eef2ff",
                     fg=SUCCESS, width=col_w,
                     anchor="center").pack(side="left", pady=6, padx=2)

        # ── navigation ────────────────────────────────────────────
        nav = tk.Frame(c, bg=BG)
        nav.pack(pady=14)
        make_btn(nav, "◀  ANIMATION",  self.ctrl.show_anim,
                 bg="#e2e8f0", fg=TEXT, width=14).pack(side="left", padx=8)
        make_btn(nav, "⟳  NEW SESSION", self.ctrl.show_intro,
                 bg=ACCENT, fg="#fff", width=14).pack(side="left", padx=8)
        make_btn(nav, "✕  EXIT",         self.ctrl.quit,
                 bg=DANGER, fg="#fff", width=12).pack(side="left", padx=8)