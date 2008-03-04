package org.labrad.types;

import java.util.Hashtable;
import java.util.Vector;
import java.lang.RuntimeException;

// TODO: add a function to find types for generic java data

public class Type {

    private static final Type[] TYPE_ARRAY = {};
    private static Hashtable<String, Type> cache = new Hashtable<String, Type>();

    public static final Type HEADER_TYPE = parse("wwiww");
    public static final Type PACKET_TYPE = parse("wwiws");
    public static final Type RECORD_TYPE = parse("wss");

    // TODO: add comment handling

    public static class Buffer {
        String s;

        public Buffer(String s) {
            this.s = s;
        }

        char getChar() {
            String c = get();
            return c.charAt(0);
        }

        String get() {
            return get(1);
        }

        String get(int i) {
            String temp = s.substring(0, i);
            s = s.substring(i);
            return temp;
        }

        char peekChar() {
            return s.charAt(0);
        }

        String peek() {
            return peek(1);
        }

        String peek(int i) {
            return s.substring(0, i);
        }

        void skip() {
            skip(1);
        }

        void skip(int i) {
            s = s.substring(i);
        }

        int length() {
            return s.length();
        }

        public String toString() {
            return s;
        }
    }

    public Type() {
    }

    public static Type parse(String tag) {
        if (cache.containsKey(tag)) {
            return cache.get(tag);
        }
        Type type;
        Vector<Type> subtypes = new Vector<Type>();
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
                type = new Cluster(subtypes.toArray(TYPE_ARRAY));
                break;
        }
        cache.put(tag, type);
        return type;
    }

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
                return new Value(units);
            case 'c':
                units = parseUnits(tb);
                return new Complex(units);
            case '(': return parseCluster(tb);
            case '*': return parseList(tb);
            case 'E': return parseError(tb);
        }
        throw new RuntimeException("Unknown character in type tag.");
    }

    private static Type parseCluster(Buffer tb) {
        Vector<Type> subtypes = new Vector<Type>();
        while (tb.length() > 0) {
            if (tb.peekChar() == ')') {
                tb.getChar();
                return new Cluster(subtypes.toArray(TYPE_ARRAY));
            }
            subtypes.add(parseSingleType(tb));
        }
        throw new RuntimeException("No closing ) found.");
    }

    private static Type parseList(Buffer tb) {
        int depth = 0, nDigits = 0;
        while (Character.isDigit(tb.peekChar())) {
            depth = depth * 10 + Integer.parseInt(tb.get());
            nDigits += 1;
        }
        if (depth == 0) {
            if (nDigits > 0) {
                throw new RuntimeException("List depth must be non-zero.");
            }
            depth = 1;
        }
        Type elementType = parseSingleType(tb);
        return new List(elementType, depth);
    }

    private static Type parseError(Buffer tb) {
        Type payloadType = parseSingleType(tb);
        return new Error(payloadType);
    }

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

    public String pretty() {
        throw new RuntimeException("Unknown type.");
    }

    public String toString() {
        throw new RuntimeException("Unknown type.");
    }

    public char getCode() {
        return 0;
    }

    public boolean isFixedWidth() {
        return true;
    }

    public int dataWidth() {
        return 0;
    }

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
            Type t = parse(s);
            System.out.println("original: " + s);
            System.out.println("parsed: " + t.toString());
            System.out.println("pretty: " + t.pretty());
        }
    }
}
