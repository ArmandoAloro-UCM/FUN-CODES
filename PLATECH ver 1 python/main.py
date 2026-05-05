"""
main.py — Entry point for Preemptive SJF CPU Scheduling Simulator.
Run:  python main.py   (from inside sjf_project/)

Files:
  main.py       entry point + controller
  scheduler.py  SRTF algorithm + data classes
  theme.py      colors, fonts, constants
  widgets.py    reusable tkinter helpers
  views.py      all screen classes
"""
import os, sys, tkinter as tk

_HERE = os.path.dirname(os.path.abspath(__file__))
if _HERE not in sys.path:
    sys.path.insert(0, _HERE)

from theme import WIN_W, WIN_H, BG


class AppController(tk.Tk):
    num_processes: int  = 3
    processes:     list = []
    scheduler            = None

    def __init__(self):
        super().__init__()
        self.title("Preemptive SJF Scheduler — CPU Replica Project")
        self.configure(bg=BG)
        self.resizable(True, True)
        self._center(WIN_W, WIN_H)
        self._view = None
        self.show_intro()

    def _center(self, w, h):
        self.update_idletasks()
        sx, sy = self.winfo_screenwidth(), self.winfo_screenheight()
        self.geometry(f"{w}x{h}+{(sx-w)//2}+{(sy-h)//2}")

    def _switch(self, ViewClass):
        if self._view:
            self._view.destroy()
        v = ViewClass(self, self)
        v.pack(fill="both", expand=True)
        self._view = v

    def show_intro(self):
        from views import IntroView;   self._switch(IntroView)
    def show_count(self):
        from views import CountView;   self._switch(CountView)
    def show_input(self):
        from views import InputView;   self._switch(InputView)
    def show_table(self):
        from views import TableView;   self._switch(TableView)
    def show_anim(self):
        from views import AnimView;    self._switch(AnimView)
    def show_results(self):
        from views import ResultsView; self._switch(ResultsView)
    def quit(self):
        self.destroy()


if __name__ == "__main__":
    AppController().mainloop()