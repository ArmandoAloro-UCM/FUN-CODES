// SJFScheduler.java — Preemptive SJF (SRTF) algorithm, no GUI dependencies

import java.util.*;

public class SJFScheduler {

    private final List<Process> processes;
    private final List<Segment> timeline = new ArrayList<>();

    public SJFScheduler(List<Process> input) {
        // Work on deep copies so the originals are not modified
        this.processes = new ArrayList<>();
        for (Process p : input) {
            this.processes.add(new Process(p));
        }
    }

    /** Runs the SRTF simulation and fills in completion/waiting/turnaround for each process. */
    public void run() {
        List<String> ids        = new ArrayList<>();
        Map<String, Integer> burstMap   = new HashMap<>();
        Map<String, Integer> arrivalMap = new HashMap<>();
        Map<String, String>  colorMap   = new HashMap<>();
        Map<String, Integer> remain     = new HashMap<>();

        int totalBurst = 0;
        int maxArrival = 0;

        for (Process p : processes) {
            ids.add(p.pid);
            burstMap.put(p.pid,   p.burst);
            arrivalMap.put(p.pid, p.arrival);
            colorMap.put(p.pid,   p.color);
            remain.put(p.pid,     p.burst);
            totalBurst += p.burst;
            if (p.arrival > maxArrival) maxArrival = p.arrival;
        }

        int n     = processes.size();
        int t     = 0;
        int done  = 0;
        int maxT  = totalBurst + maxArrival + 5;

        Map<String, Integer> finish = new HashMap<>();
        List<int[]> raw = new ArrayList<>();   // [pid-index, t_start, t_end]
        String current  = null;
        int    segStart = 0;

        while (done < n && t < maxT) {
            // Collect all processes that have arrived and still need CPU time
            List<String> ready = new ArrayList<>();
            for (String pid : ids) {
                if (arrivalMap.get(pid) <= t && remain.get(pid) > 0) {
                    ready.add(pid);
                }
            }

            if (ready.isEmpty()) {
                // No process ready — seal any open segment and advance one tick
                if (current != null) {
                    raw.add(new int[]{ ids.indexOf(current), segStart, t });
                    current = null;
                }
                t++;
                continue;
            }

            // SRTF: choose the process with the shortest remaining time
            String chosen = ready.stream()
                .min(Comparator.comparingInt(remain::get))
                .orElseThrow();

            if (!chosen.equals(current)) {
                // Context switch — seal the previous segment and open a new one
                if (current != null) {
                    raw.add(new int[]{ ids.indexOf(current), segStart, t });
                }
                current  = chosen;
                segStart = t;
            }

            remain.put(chosen, remain.get(chosen) - 1);
            t++;

            if (remain.get(chosen) == 0) {
                // Process finished — seal its segment
                finish.put(chosen, t);
                raw.add(new int[]{ ids.indexOf(chosen), segStart, t });
                current  = null;
                segStart = t;
                done++;
            }
        }

        // Merge consecutive segments belonging to the same process
        List<int[]> merged = new ArrayList<>();
        for (int[] s : raw) {
            if (!merged.isEmpty()) {
                int[] last = merged.get(merged.size() - 1);
                if (last[0] == s[0] && last[2] == s[1]) {
                    last[2] = s[2];   // extend the existing segment
                    continue;
                }
            }
            merged.add(new int[]{ s[0], s[1], s[2] });
        }

        // Build Segment objects for the public timeline list
        for (int[] s : merged) {
            String pid = ids.get(s[0]);
            timeline.add(new Segment(pid, s[1], s[2], colorMap.get(pid)));
        }

        // Compute per-process metrics
        for (Process p : processes) {
            int ct = finish.get(p.pid);
            p.completion  = ct;
            p.turnaround  = ct - arrivalMap.get(p.pid);
            p.waiting     = p.turnaround - burstMap.get(p.pid);
        }
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public List<Process> getProcesses() { return processes; }
    public List<Segment> getTimeline()  { return timeline;  }

    public double avgWaiting() {
        return processes.stream().mapToInt(p -> p.waiting).average().orElse(0);
    }

    public double avgCompletion() {
        return processes.stream().mapToInt(p -> p.completion).average().orElse(0);
    }

    public double avgTurnaround() {
        return processes.stream().mapToInt(p -> p.turnaround).average().orElse(0);
    }

    public int totalTime() {
        return timeline.isEmpty() ? 0 : timeline.get(timeline.size() - 1).tEnd;
    }
}
