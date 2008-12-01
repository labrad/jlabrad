package org.labrad.types;

public class Error extends Type {
    Type payload;

    /**
     * Factory to create an error type with the specified payload.
     * @param payload
     * @return
     */
    public static Error of(Type payload) {
    	return new Error(payload);
    }
    
    private Error(Type payload) { this.payload = payload; }

    public char getCode() { return 'E'; }

    public boolean isFixedWidth() { return false; }
    public int dataWidth() { return 4 + 4 + payload.dataWidth(); }

    public String toString() { return "E" + payload.toString(); }

    public String pretty() { return "error(" + payload.pretty() + ")"; }
    public Type getSubtype(int i) { return payload; }
}
