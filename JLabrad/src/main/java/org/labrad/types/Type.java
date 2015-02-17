/*
 * Copyright 2008 Matthew Neeley
 * 
 * This file is part of JLabrad.
 *
 * JLabrad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JLabrad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JLabrad.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.labrad.types;

import java.util.ArrayList;

public abstract class Type {

  public enum Code {
    ANY("ANY"),
    BOOL("BOOL"),
    CLUSTER("CLUSTER"),
    COMPLEX("COMPLEX"),
    EMPTY("EMPTY"),
    ERROR("ERROR"),
    INT("INT"),
    LIST("LIST"),
    STR("STR"),
    TIME("TIME"),
    VALUE("VALUE"),
    WORD("WORD");

    String name;

    Code(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }


  /**
   * Encapsulates a cache of type objects.  A LinkedHashMap is used with a finite
   * size to prevent the cache from growing without bounds.
   */
  /*
	@SuppressWarnings("serial")
	private static class Cache extends LinkedHashMap<String, Type> {
		private static final int CACHE_SIZE = 100;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Type> eldest) {
			return size() > CACHE_SIZE;
		}
	}
   */

  /** A cache of parsed type objects. */
  //private static Cache cache = new Cache();

  // types used in parsing and unparsing packets

  /** Packet header type: "w{ctxtHigh} w{ctxtLow} i{request} w{src/tgt} w{dataLen}" */
  public static final Type HEADER_TYPE = fromTag("wwiww");

  /** Packet type: "w{ctxtHigh} w{ctxtLow} i{request} w{src/tgt} s{data}" */
  public static final Type PACKET_TYPE = fromTag("wwiws");

  /** Record type: "w{ID} s{typeTag} s{data}" */
  public static final Type RECORD_TYPE = fromTag("wss");


  /**
   * Encapsulates a string and a position within the
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

    @SuppressWarnings("unused")
    String peek() { return peek(1); }
    String peek(int i) { return s.substring(0, i); }
    char peekChar() { return s.charAt(0); }

    @SuppressWarnings("unused")
    void skip() { skip(1); }
    void skip(int i) { s = s.substring(i); }
    void skipWhitespace() { s = s.replaceFirst("[,\\s]*", ""); }

    int length() { return s.length(); }

    @Override
    public String toString() { return s; }
  }

  /**
   * Get a type object from a tag string.
   * @param tag
   * @return
   */
  public static Type fromTag(String tag) {
    tag = stripComments(tag);
    java.util.List<Type> subtypes = new ArrayList<Type>();
    Buffer tb = new Buffer(tag);
    while (tb.length() > 0) {
      subtypes.add(parseSingleType(tb));
    }
    switch (subtypes.size()) {
      case 0: return Empty.getInstance();
      case 1: return subtypes.get(0);
      default: return Cluster.of(subtypes);
    }
  }

  /**
   * Remove any comments from the type tag.  This includes anything after a colon,
   * as well as anything embedded between curly brackets: {}.
   * @param tag
   * @return
   */
  private static String stripComments(String tag) {
    tag = tag.split(":")[0]; // strip off any trailing comments
    tag = tag.replaceAll("\\{[^\\{\\}]*\\}", ""); // remove anything in brackets
    return tag;
  }

  /**
   * Parse a single type from a buffer that may contain a cluster of types.
   * @param tb
   * @return
   */
  private static Type parseSingleType(Buffer tb) {
    tb.skipWhitespace();
    if (tb.length() == 0) {
      return Empty.getInstance();
    }
    Type t;
    switch (tb.getChar()) {
      case '_': t = Empty.getInstance(); break;
      case '?': t = Any.getInstance(); break;
      case 'b': t = Bool.getInstance(); break;
      case 'i': t = Int.getInstance(); break;
      case 'w': t = Word.getInstance(); break;
      case 's': t = Str.getInstance(); break;
      case 't': t = Time.getInstance(); break;
      case 'v': t = Value.of(parseUnits(tb)); break;
      case 'c': t = Complex.of(parseUnits(tb)); break;
      case '(': t = parseCluster(tb); break;
      case '*': t = parseList(tb); break;
      case 'E': t = parseError(tb); break;
      default: throw new RuntimeException("Unknown character in type tag.");
    }
    tb.skipWhitespace();
    return t;
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
    tb.skipWhitespace();
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
    tb.skipWhitespace();
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
    tb.skipWhitespace();
    StringBuffer units = new StringBuffer();
    char c;
    if ((tb.length() == 0) || (tb.peekChar() != '[')) {
      return null;
    }
    tb.getChar(); // drop '['
    while (tb.length() > 0) {
      c = tb.getChar();
      if (c == ']') {
        return units.toString();
      }
      units.append(c);
    }
    throw new RuntimeException("No closing ] found.");
  }


  // instance methods on type objects

  public boolean matches(String tag) {
    return matches(Type.fromTag(tag));
  }

  public abstract boolean matches(Type type);

  /**
   * Returns a verbose string describing this type object.
   */
  public abstract String pretty();

  /**
   * Returns a compact string describing this type object.
   * This string representation is used in flattening and is also
   * suitable for passing to the Data constructor to create a new
   * LabRAD data object.
   */
  public abstract String toString();

  /**
   * Returns a one-character code for this type object.
   * @return
   */
  public char getChar() { return 0; }

  public Code getCode() {
    throw new RuntimeException("Not implemented.");
  }

  /**
   * Indicates whether this type object represents data whose byte length is fixed.
   * @return
   */
  public boolean isFixedWidth() { return true; }

  /**
   * Gives the width of the byte string for data of this type.  Note that if this is
   * not a fixed-width type, then some of the bytes here are pointers to variable-length
   * sections.
   * @return
   */
  public int dataWidth() { return 0; }

  /**
   * Gets the subtype of this type at the specified index.  Note that this is only
   * valid for types that have subtypes, such as lists and clusters.
   * @param index
   * @return
   */
  public Type getSubtype(int index) {
    throw new RuntimeException("Not implemented.");
  }

  public int getOffset(int index) {
    throw new RuntimeException("Not implemented.");
  }

  public int size() {
    throw new RuntimeException("Not implemented.");
  }

  public String getUnits() {
    throw new RuntimeException("Not implemented.");
  }

  public int getDepth() {
    throw new RuntimeException("Not implemented.");
  }

  public static void main(String[] args) {
    String[] tests = {
        // basic types
        "b", "i", "w", "is", "*(is)", "v", "v[]", "v[m/s]",
        "", "?", "*?", "*2?", "(s?)",
        // comments
        "s: this has a trailing comment",
        "ss{embedded comment}is",
        "ss{embedded comment}is: trailing comment",
        // whitespace and commas
        "s,s", "s, s", "* 3 v[m]"};
    for (String s : tests) {
      Type t = fromTag(s);
      System.out.println("original: " + s);
      System.out.println("parsed: " + t.toString());
      System.out.println("pretty: " + t.pretty());
      System.out.println("");
    }

    // check pairs that should match
    String[] matchTests = {
        "", "",
        "", "?",
        "?", "?",
        "v[s]", "v",
        "v[s]", "v[s]",
        "*s", "*s",
        "*s", "*?"};
    for (int i = 0; i < matchTests.length; i += 2) {
      Type t1 = Type.fromTag(matchTests[i]);
      Type t2 = Type.fromTag(matchTests[i+1]);
      assert t1.matches(t2);
      System.out.println("'" + t1 + "' matches '" + t2 + "'");
    }

    // check pairs that should not match
    String[] notMatchTests = {
        "", "i",
        "?", "s",
        "v[s]", "v[m]",
        "(ss)", "(si)",
        "*s", "*2s"};
    for (int i = 0; i < notMatchTests.length; i += 2) {
      Type t1 = Type.fromTag(matchTests[i]);
      Type t2 = Type.fromTag(matchTests[i+1]);
      assert !t1.matches(t2);
      System.out.println("'" + t1 + "' does not match '" + t2 + "'");
    }
  }
}
