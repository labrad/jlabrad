package org.labrad.types;

public final class Str extends Type {
    private static Str instance = new Str();

    // private constructor to prevent instantiation
    private Str() {}
    
    public static Str getInstance() { return instance; }

    public Type.Code getCode() { return Type.Code.STR; }
    public char getChar() { return 's'; }

    public boolean isFixedWidth() { return false; }
    public int dataWidth() { return 4; }

    public String toString() { return "s"; }
    public String pretty() { return "string"; }
}
