package org.labrad.types;

public final class Int extends Type {
    private static Int instance = new Int();

    // private constructor to prevent instantiation
    private Int() {}

    public static Int getInstance() { return instance; }

    public Type.Code getCode() { return Type.Code.INT; }
    public char getChar() { return 'i'; }

    public boolean isFixedWidth() { return true; }
    public int dataWidth() { return 4; }

    public String toString() { return "i"; }
    public String pretty() { return "int"; }
}
