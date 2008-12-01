package org.labrad.types;

public final class Word extends Type {
    private static Word instance = new Word();

    // private constructor to prevent instantiation
    private Word() {}
    
    public static Word getInstance() { return instance; }

    public Type.Code getCode() { return Type.Code.WORD; }
    public char getChar() { return 'w'; }

    public boolean isFixedWidth() { return true; }
    public int dataWidth() { return 4; }

    public String toString() { return "w"; }
    public String pretty() { return "word"; }
}
