package org.labrad.types;

public class Complex extends Type {
	String units;
	
	public Complex(String units) {
		this.units = units;
	}
	
	public boolean isFixedWidth() { return true; }
	public int dataWidth() { return 16; }
	public String getUnits() { return units; }
	
	public char getCode() { return 'c'; }
	public String toString() {
		if (units == null) {
			return "c";
		}
		return "c[" + units + "]";
	}
	public String pretty() {
		if (units == null) {
			return "complex";
		}
		return "complex[" + units + "]";
	}
}
