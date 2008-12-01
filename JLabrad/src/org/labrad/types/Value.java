package org.labrad.types;

public class Value extends Type {
    String units;

    /**
     * Factory to create a Value type in the specified units.
     * Note that units can be null to indicate no units.  This
     * is different from the empty string "", which indicates
     * a dimensionless quantity.
     * @param units
     * @return
     */
    public static Value of(String units) {
    	return new Value(units);
    }
    
    private Value(String units) { this.units = units; }

    public boolean isFixedWidth() { return true; }
    public int dataWidth() { return 8; }

    public String getUnits() { return units; }

    public char getCode() { return 'v'; }

    public String toString() {
    	return "v" + (units == null ? "" : "[" + units + "]");
    }

    public String pretty() {
    	return "value" + (units == null ? "" : "[" + units + "]");
    }
}
