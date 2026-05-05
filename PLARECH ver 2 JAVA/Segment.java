// Segment.java — a single colored time slice in the Gantt chart timeline

public class Segment {
    public String pid;
    public int    tStart;
    public int    tEnd;
    public String color;

    public Segment(String pid, int tStart, int tEnd, String color) {
        this.pid    = pid;
        this.tStart = tStart;
        this.tEnd   = tEnd;
        this.color  = color;
    }

    /** Duration in logical time units. */
    public int duration() {
        return tEnd - tStart;
    }
}
