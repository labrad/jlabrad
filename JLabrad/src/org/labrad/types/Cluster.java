package org.labrad.types;

import java.util.Arrays;
import java.util.List;

public final class Cluster extends Type {

    List<Type> elementTypes;
    int width;
    boolean fixedWidth;
    int[] offsets;
    String string;
    
    /**
     * Create a new cluster from an array of element types.
     * @param elementTypes
     * @return
     */
    public static Cluster of(Type... elementTypes) {
    	return new Cluster(Arrays.asList(elementTypes));
    }

    /**
     * Create a new cluster from a list of element types.
     * @param elementTypes
     * @return
     */
    public static Cluster of(List<Type> elementTypes) {
    	return new Cluster(elementTypes);
    }
    
	private Cluster(List<Type> elementTypes) {
        this.elementTypes = elementTypes;
        
        width = 0;
        fixedWidth = true;
        string = "";
        offsets = new int[elementTypes.size()];
        
        int ofs = 0;
        
        for (int i = 0; i < elementTypes.size(); i++) {
        	Type t = elementTypes.get(i);
        	width += t.dataWidth();
        	if (!t.isFixedWidth()) {
        		fixedWidth = false;
        	}
        	string += t.toString();
        	offsets[i] = ofs;
        	ofs += t.dataWidth();
        }
        string = "(" + string + ")";
    }
    
    public boolean isFixedWidth() { return fixedWidth;}
    public int dataWidth() { return width; }

    public String toString() { return string; }

    public String pretty() {
        String s = "";
        for (Type t : elementTypes) {
            s += ", " + t.pretty();
        }
        return "cluster(" + s.substring(2) + ")";
    }

    public Type.Code getCode() { return Type.Code.CLUSTER; }
    public char getChar() { return '('; }
    public Type getSubtype(int index) { return elementTypes.get(index); }
    public int size() { return elementTypes.size(); }
    public int getOffset(int index) { return offsets[index]; }
}
