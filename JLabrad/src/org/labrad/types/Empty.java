package org.labrad.types;

public class Empty extends Type {
	private static Empty instance = new Empty();
	public static Empty getInstance() { return instance; }
	public char getCode() { return '_'; }
	public String toString() { return ""; }
	public String pretty() { return "empty"; }
}
