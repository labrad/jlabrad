package org.labrad.types;

public final class Bool extends Type {
    private static Bool instance = new Bool();

    // private constructor to prevent instantiation
    private Bool() {}
    
    public static Bool getInstance() { return instance; }

    public Type.Code getCode() { return Type.Code.BOOL; }
    public char getChar() { return 'b'; }

    public boolean isFixedWidth() { return true; }
    public int dataWidth() { return 1; }

    public String toString() { return "b"; }
    public String pretty() { return "bool"; }
}
