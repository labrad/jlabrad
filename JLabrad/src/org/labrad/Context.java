package org.labrad;

public class Context {
    public long high, low;

    public Context(long high, long low) {
        this.high = high;
        this.low = low;
    }

    public String toString() {
        return "(" + Long.toString(high) + "," + Long.toString(low) + ")";
    }
}
