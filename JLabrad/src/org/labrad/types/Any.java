package org.labrad.types;

public final class Any extends Type {
    private static Any instance = new Any();

    // private constructor to prevent instantiation
    private Any() { }
    
    public static Any getInstance() { return instance; }

    public Type.Code getCode() { return Type.Code.ANY; }
    public char getChar() { return '?'; }

    public String toString() { return "?"; }
    public String pretty() { return "any"; }
}
