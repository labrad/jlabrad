package org.labrad.types;

public class Bool extends Type {
    private static Bool instance = new Bool();

    public static Bool getInstance() {
        return instance;
    }

    public char getCode() {
        return 'b';
    }

    public boolean isFixedWidth() {
        return true;
    }

    public int dataWidth() {
        return 1;
    }

    public String toString() {
        return "b";
    }

    public String pretty() {
        return "bool";
    }
}
