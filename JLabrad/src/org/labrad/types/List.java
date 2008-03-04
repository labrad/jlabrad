package org.labrad.types;

public class List extends Type {
    Type elementType;

    int depth;

    /**
     * Constructor for lists of any depth. Lists are multidimensional,
     * rectangular, and homogeneous (all elements have the same type).
     * 
     * @param elementType
     *            The LabRAD type of each element of the list.
     * @param depth
     *            The depth (number of dimensions) of the list.
     */
    public List(Type elementType, int depth) {
        this.elementType = elementType;
        this.depth = depth;
    }

    /**
     * Constructor for lists of depth 1.
     * 
     * @param elementType
     *            The LabRAD type of each element of the list.
     */
    public List(Type elementType) {
        this.elementType = elementType;
        this.depth = 1;
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

    public char getCode() {
        return '*';
    }

    public int getDepth() {
        return depth;
    }

    public boolean isFixedWidth() {
        return false;
    }

    public int dataWidth() {
        return 4 * depth + 4;
    }

    public Type getSubtype(int index) {
        return elementType;
    }

    public int getOffset(int index) {
        return index * elementType.dataWidth();
    }
}
