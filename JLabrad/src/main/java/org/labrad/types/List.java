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

public final class List extends Type {
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
    // FIXME this is a bit of a hack to get empty lists to flatten properly in some circumstances
    String elemStr = elementType.toString();
    if (elementType == Empty.getInstance()) {
      elemStr = "_";
    }
    if (depth == 1) {
      return "*" + elemStr;
    }
    return "*" + Integer.toString(depth) + elemStr;
  }

  public String pretty() {
    if (depth == 1) {
      return "list(" + elementType.pretty() + ")";
    }
    return "list(" + elementType.pretty() + ", " + Integer.toString(depth)
    + ")";
  }

  public boolean matches(Type type) {
    if (type instanceof Any) return true;
    if (!(type instanceof List)) return false;
    if (type.getDepth() != getDepth()) return false;
    return getSubtype(0).matches(type.getSubtype(0));
  }

  public Type.Code getCode() { return Type.Code.LIST; }
  public char getChar() { return '*'; }

  public int getDepth() { return depth; }

  public boolean isFixedWidth() { return false; }
  public int dataWidth() { return 4 * depth + 4; }

  public Type getSubtype(int index) { return elementType; }
  public int getOffset(int index) { return index * elementType.dataWidth(); }
}
