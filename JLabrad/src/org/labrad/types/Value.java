
package org.labrad.types;

public class Value extends Type {
	String units;
	
	public Value(String units) {
		this.units = units;
	}
	
	public boolean isFixedWidth() { return true; }
	public int dataWidth() { return 8; }
	public String getUnits() { return units; }
	
	public char getCode() { return 'v'; }
	public String toString() {
		if (units == null) {
			return "v";
		}
		return "v[" + units + "]";
	}
	public String pretty() {
		if (units == null) {
			return "value";
		}
		return "value[" + units + "]";
	}
}
