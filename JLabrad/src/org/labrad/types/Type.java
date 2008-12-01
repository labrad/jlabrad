package org.labrad.types;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

// TODO: add a function to find types for generic java data

public abstract class Type {

	@SuppressWarnings("serial")
	private static class Cache extends LinkedHashMap<String, Type> {
		private static final int CACHE_SIZE = 100;
		
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Type> eldest) {
			return size() > CACHE_SIZE;
		}
	}
	
    private static Cache cache = new Cache();

    public static final Type HEADER_TYPE = fromTag("wwiww");
    public static final Type PACKET_TYPE = fromTag("wwiws");
    public static final Type RECORD_TYPE = fromTag("wss");

    // TODO: add comment handling

    /**
     * Buffer encapsulates a string and a position within the
     * string that is currently being read.  We can pick off
     * characters one by one, or 'peek' ahead at the next character
     * without removing it.
     */
    private static class Buffer {
        String s;

        public Buffer(String s) { this.s = s; }

        String get() { return get(1); }
        String get(int i) {
            String temp = s.substring(0, i);
            s = s.substring(i);
            return temp;
        }
        char getChar() { return get().charAt(0); }
        
        String peek() { return peek(1); }
        String peek(int i) { return s.substring(0, i); }
        char peekChar() { return s.charAt(0); }
        
        void skip() { skip(1); }
        void skip(int i) { s = s.substring(i); }

        int length() { return s.length(); }

        public String toString() { return s; }
    }

    /**
     * Get a type object from a tag string.
     * @param tag
     * @return
     */
    public static Type fromTag(String tag) {
    	// TODO: this caching scheme is not thread-safe
        if (cache.containsKey(tag)) {
            return cache.get(tag);
        }
        Type type;
        java.util.List<Type> subtypes = new ArrayList<Type>();
        Buffer tb = new Buffer(tag);
        while (tb.length() > 0) {
            subtypes.add(parseSingleType(tb));
        }
        switch (subtypes.size()) {
            case 0:
                type = Empty.getInstance();
                break;

            case 1:
                type = subtypes.get(0);
                break;

            default:
                type = Cluster.of(subtypes);
                break;
        }
        cache.put(tag, type);
        return type;
    }

    /**
     * Parse a single type from a buffer that may contain a cluster of types.
     * @param tb
     * @return
     */
    private static Type parseSingleType(Buffer tb) {
        String units;
        if (tb.length() == 0) {
            return Empty.getInstance();
        }
        switch (tb.getChar()) {
            case '_': return Empty.getInstance();
            case '?': return Any.getInstance();
            case 'b': return Bool.getInstance();
            case 'i': return Int.getInstance();
            case 'w': return Word.getInstance();
            case 's': return Str.getInstance();
            case 't': return Time.getInstance();
            case 'v':
                units = parseUnits(tb);
                return Value.of(units);
            case 'c':
                units = parseUnits(tb);
                return Complex.of(units);
            case '(': return parseCluster(tb);
            case '*': return parseList(tb);
            case 'E': return parseError(tb);
        }
        throw new RuntimeException("Unknown character in type tag.");
    }

    /**
     * Parse a cluster of types by repeatedly parsing single types until
     * a close parenthesis is encountered.
     * @param tb
     * @return
     */
    private static Type parseCluster(Buffer tb) {
        java.util.List<Type> subTypes = new ArrayList<Type>();
        while (tb.length() > 0) {
            if (tb.peekChar() == ')') {
                tb.getChar();
                return Cluster.of(subTypes);
            }
            subTypes.add(parseSingleType(tb));
        }
        throw new RuntimeException("No closing ) found.");
    }

    /**
     * Parse a list by parsing an optional depth indicator,
     * followed by the element type.
     * @param tb
     * @return
     */
    private static Type parseList(Buffer tb) {
        int depth = 0, nDigits = 0;
        while (Character.isDigit(tb.peekChar())) {
            depth = depth * 10 + Integer.valueOf(tb.get());
            nDigits += 1;
        }
        if (depth == 0) {
            if (nDigits > 0) {
                throw new RuntimeException("List depth must be non-zero.");
            }
            depth = 1;
        }
        Type elementType = parseSingleType(tb);
        return List.of(elementType, depth);
    }

    /**
     * Parse an error type that may contain an optional payload.
     * @param tb
     * @return
     */
    private static Type parseError(Buffer tb) {
        Type payloadType = parseSingleType(tb);
        return Error.of(payloadType);
    }

    /**
     * Parse units for value and complex types.
     * @param tb
     * @return
     */
    private static String parseUnits(Buffer tb) {
        String units = "";
        char c;
        if ((tb.length() == 0) || (tb.peekChar() != '[')) {
            return null;
        }
        tb.getChar(); // drop '['
        while (tb.length() > 0) {
            c = tb.getChar();
            if (c == ']') {
                return units;
            }
            units += c;
        }
        throw new RuntimeException("No closing ] found.");
    }

    public abstract String pretty();

    public abstract String toString();

    public char getCode() { return 0; }

    public boolean isFixedWidth() { return true; }

    public int dataWidth() { return 0; }

    public Type getSubtype(int index) {
        throw new RuntimeException("No subtypes.");
    }

    public int getOffset(int index) {
        throw new RuntimeException("No element offsets.");
    }

    public int size() {
        throw new RuntimeException("No size for this type.");
    }

    public String getUnits() {
        throw new RuntimeException("No units.");
    }

    public int getDepth() {
        throw new RuntimeException("No depth.");
    }

    public static void main(String[] args) {
        String[] tests = { "b", "i", "w", "is", "*(is)", "v", "v[]", "v[m/s]",
                "", "?", "*?", "*2?", "(s?)" };
        for (String s : tests) {
            Type t = fromTag(s);
            System.out.println("original: " + s);
            System.out.println("parsed: " + t.toString());
            System.out.println("pretty: " + t.pretty());
        }
    }
}
