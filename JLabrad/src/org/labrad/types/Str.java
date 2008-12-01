package org.labrad.types;

public class Str extends Type {
    private static Str instance = new Str();

    // private constructor to prevent instantiation
    private Str() {}
    
    public static Str getInstance() { return instance; }

    public char getCode() { return 's'; }

    public boolean isFixedWidth() { return false; }
    public int dataWidth() { return 4; }

    public String toString() { return "s"; }
    public String pretty() { return "string"; }
}
