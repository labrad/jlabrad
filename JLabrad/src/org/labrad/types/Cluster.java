package org.labrad.types;

public class Cluster extends Type {

    Type[] elementTypes;
    int _dataWidth;
    boolean _isFixedWidth;
    int[] _offsets;
    String _toString;
    
    public Cluster(Type[] elementTypes) {
        this.elementTypes = elementTypes;
        
        _dataWidth = 0;
        _isFixedWidth = true;
        _toString = "";
        _offsets = new int[elementTypes.length];
        
        int ofs = 0;
        
        for (int i = 0; i < elementTypes.length; i++) {
        	Type t = elementTypes[i];
        	_dataWidth += t.dataWidth();
        	if (!t.isFixedWidth()) {
        		_isFixedWidth = false;
        	}
        	_toString += t.toString();
        	_offsets[i] = ofs;
        	ofs += t.dataWidth();
        }
        _toString = "(" + _toString + ")";
    }
    
    public boolean isFixedWidth() {
        return _isFixedWidth;
    }

    public int dataWidth() {
        return _dataWidth;
    }

    public String toString() {
        return _toString;
    }

    public String pretty() {
        String s = "";
        for (Type t : elementTypes) {
            s += ", " + t.pretty();
        }
        return "cluster(" + s.substring(2) + ")";
    }

    public char getCode() {
        return '(';
    }

    public Type getSubtype(int index) {
        return elementTypes[index];
    }

    public int size() {
        return elementTypes.length;
    }

    public int getOffset(int index) {
        return _offsets[index];
    }
}
