package org.labrad.data;

public class Context {
    private long high, low;

    public Context(long high, long low) {
        this.high = high;
        this.low = low;
    }

    public long getHigh() { return high; }
    public long getLow() { return low; }
    
    public String toString() {
        return "(" + high + "," + low + ")";
    }
}
