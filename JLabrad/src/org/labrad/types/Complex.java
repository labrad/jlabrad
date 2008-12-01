package org.labrad.types;

public final class Complex extends Type {
    String units;

    /**
     * Factory to create a complex types with the specified units.
     * Note that units can be null, indicating no units.  This is
     * different from the empty string "", which means a dimensionless
     * quantity.
     */
    public static Complex of(String units) {
    	return new Complex(units);
    }
    
    private Complex(String units) { this.units = units; }

    public boolean isFixedWidth() { return true; }

    public int dataWidth() { return 16; }

    public String getUnits() { return units; }

    public Type.Code getCode() { return Type.Code.COMPLEX; }
    public char getChar() { return 'c'; }

    public String toString() {
    	return "c" + (units == null ? "" : "[" + units + "]");
    }

    public String pretty() {
    	return "complex" + (units == null ? "" : "[" + units + "]");
    }
}
