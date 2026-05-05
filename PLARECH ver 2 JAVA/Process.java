// Process.java — data class representing a single CPU process

public class Process {
    public String pid;
    public int    burst;
    public int    arrival;
    public String color;   // hex color string, e.g. "#dc2626"

    // Filled in after SJFScheduler.run()
    public int completion  = 0;
    public int waiting     = 0;
    public int turnaround  = 0;

    public Process(String pid, int burst, int arrival, String color) {
        this.pid     = pid;
        this.burst   = burst;
        this.arrival = arrival;
        this.color   = color;
    }

    /** Copy constructor — creates a fresh Process with no computed metrics. */
    public Process(Process other) {
        this(other.pid, other.burst, other.arrival, other.color);
    }
}
