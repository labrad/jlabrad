package org.labrad.types;

public class Word extends Type {
	private static Word instance = new Word();
	public static Word getInstance() { return instance; }
	public char getCode() { return 'w'; }
	public boolean isFixedWidth() { return true; }
	public int dataWidth() { return 4; }
	public String toString() { return "w"; }
	public String pretty() { return "word"; }
}
