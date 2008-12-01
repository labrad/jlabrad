package org.labrad.types;

public class List extends Type {
    Type elementType;

    int depth;

    /**
     * Factory to create a list containing elements of the specified
     * type and with the specified depth (number of dimensions.
     * @param elementType
     * @param depth
     * @return
     */
    public static List of(Type elementType, int depth) {
    	return new List(elementType, depth);
    }
    
    /**
     * Factory to create a one-dimensional list of the specified type.
     * @param elementType
     * @return
     */
    public static List of(Type elementType) {
    	return new List(elementType, 1);
    }
    
    /**
     * Constructor for lists of any depth. Lists are multidimensional,
     * rectangular, and homogeneous (all elements have the same type).
     * 
     * @param elementType
     *            The LabRAD type of each element of the list.
     * @param depth
     *            The depth (number of dimensions) of the list.
     */
    private List(Type elementType, int depth) {
        this.elementType = elementType;
        this.depth = depth;
    }

    public String toString() {
        if (depth == 1) {
            return "*" + elementType.toString();
        }
        return "*" + Integer.toString(depth) + elementType.toString();
    }

    public String pretty() {
        if (depth == 1) {
            return "list(" + elementType.pretty() + ")";
        }
        return "list(" + elementType.pretty() + ", " + Integer.toString(depth)
                + ")";
    }

    public char getCode() { return '*'; }

    public int getDepth() { return depth; }

    public boolean isFixedWidth() { return false; }
    public int dataWidth() { return 4 * depth + 4; }

    public Type getSubtype(int index) { return elementType; }
    public int getOffset(int index) { return index * elementType.dataWidth(); }
}
