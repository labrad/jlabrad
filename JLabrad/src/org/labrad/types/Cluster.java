package org.labrad.types;

public class Cluster extends Type {
	
	Type[] elementTypes;
	
	public Cluster(Type[] elementTypes) {
		this.elementTypes = elementTypes;
	}
	
	public boolean isFixedWidth() {
		for (Type t : elementTypes) {
			if (!t.isFixedWidth()) {
				return false;
			}
		}
		return true;
	}
	public int dataWidth() {
		int dw = 0;
		for (Type t : elementTypes) {
			dw += t.dataWidth();
		}
		return dw;
	}
	
	public String toString() {
		String s = "";
		for (Type t : elementTypes) {
			s += t.toString();
		}
		return "(" + s + ")";
	}
	public String pretty() {
		String s = "";
		for (Type t : elementTypes) {
			s += ", " + t.pretty();
		}
		return "cluster(" + s.substring(2) + ")";
	}
	public char getCode() { return '('; }
	public Type getSubtype(int index) {
		return elementTypes[index];
	}
	public int size() {
		return elementTypes.length;
	}
	public int getOffset(int index) {
		int ofs = 0;
		for (int i = 0; i < index; i++) {
			ofs += elementTypes[i].dataWidth();
		}
		return ofs;
	}
}
