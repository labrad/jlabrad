package org.labrad.types;

public final class Any extends Type {
    private static Any instance = new Any();

    // private constructor to prevent instantiation
    private Any() { }
    
    public static Any getInstance() { return instance; }

    public char getCode() { return '?'; }

    public String toString() { return "?"; }
    public String pretty() { return "any"; }
}
