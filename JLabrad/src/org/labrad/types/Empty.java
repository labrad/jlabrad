package org.labrad.types;

public final class Empty extends Type {
    private static Empty instance = new Empty();

    // private constructor to prevent instantiation
    private Empty() {}

    public static Empty getInstance() { return instance; }

    public Type.Code getCode() { return Type.Code.EMPTY; }
    public char getChar() { return '_'; }

    public String toString() { return ""; }
    public String pretty() { return "empty"; }
}
