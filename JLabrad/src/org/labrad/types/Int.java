package org.labrad.types;

public class Int extends Type {
    private static Int instance = new Int();

    // private constructor to prevent instantiation
    private Int() {}

    public static Int getInstance() { return instance; }

    public char getCode() { return 'i'; }

    public boolean isFixedWidth() { return true; }
    public int dataWidth() { return 4; }

    public String toString() { return "i"; }
    public String pretty() { return "int"; }
}
