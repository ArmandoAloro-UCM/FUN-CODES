# scheduler.py — Preemptive SJF (SRTF) algorithm, no GUI dependencies

from dataclasses import dataclass, field
from typing import List, Dict


@dataclass
class Process:
    pid:     str
    burst:   int
    arrival: int
    color:   str = "#000000"
    # filled after run()
    completion:  int = 0
    waiting:     int = 0
    turnaround:  int = 0


@dataclass
class Segment:
    pid:     str
    t_start: int
    t_end:   int
    color:   str

    @property
    def duration(self) -> int:
        return self.t_end - self.t_start


class SJFScheduler:
    def __init__(self, processes: List[Process]):
        self.processes = [Process(p.pid, p.burst, p.arrival, p.color) for p in processes]
        self.timeline:  List[Segment] = []

    def run(self):
        procs       = self.processes
        ids         = [p.pid for p in procs]
        burst_map   = {p.pid: p.burst   for p in procs}
        arrival_map = {p.pid: p.arrival for p in procs}
        color_map   = {p.pid: p.color   for p in procs}
        remain      = {p.pid: p.burst   for p in procs}

        n, t, done  = len(procs), 0, 0
        finish: Dict[str, int] = {}
        raw: List   = []
        current     = None
        seg_start   = 0
        max_t       = sum(burst_map.values()) + max(arrival_map.values()) + 5

        while done < n and t < max_t:
            ready = [p for p in ids if arrival_map[p] <= t and remain[p] > 0]
            if not ready:
                if current is not None:
                    raw.append([current, seg_start, t]); current = None
                t += 1; continue

            chosen = min(ready, key=lambda p: remain[p])
            if chosen != current:
                if current is not None:
                    raw.append([current, seg_start, t])
                current, seg_start = chosen, t

            remain[chosen] -= 1; t += 1
            if remain[chosen] == 0:
                finish[chosen] = t
                raw.append([chosen, seg_start, t])
                current, seg_start = None, t; done += 1

        # merge consecutive same-pid
        merged = []
        for s in raw:
            if merged and merged[-1][0] == s[0] and merged[-1][2] == s[1]:
                merged[-1][2] = s[2]
            else:
                merged.append(list(s))

        self.timeline = [Segment(s[0], s[1], s[2], color_map[s[0]]) for s in merged]

        for p in procs:
            ct = finish[p.pid]
            p.completion = ct
            p.turnaround = ct - arrival_map[p.pid]
            p.waiting    = p.turnaround - burst_map[p.pid]

    @property
    def avg_waiting(self):
        return sum(p.waiting    for p in self.processes) / len(self.processes)

    @property
    def avg_completion(self):
        return sum(p.completion for p in self.processes) / len(self.processes)

    @property
    def avg_turnaround(self):
        return sum(p.turnaround for p in self.processes) / len(self.processes)

    @property
    def total_time(self):
        return self.timeline[-1].t_end if self.timeline else 0