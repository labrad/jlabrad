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
    StringBuffer buf = new StringBuffer();
    for (Type t : elementTypes) {
      buf.append(", ");
      buf.append(t.pretty());
    }
    return "cluster(" + buf.toString().substring(2) + ")";
  }

  public boolean matches(Type type) {
    if (type instanceof Any) return true;
    if (!(type instanceof Cluster)) return false;
    if (type.size() != size()) return false;
    for (int i = 0; i < size(); i++) {
      if (!getSubtype(i).matches(type.getSubtype(i)))
        return false;
    }
    return true;
  }

  public Type.Code getCode() { return Type.Code.CLUSTER; }
  public char getChar() { return '('; }
  public Type getSubtype(int index) { return elementTypes.get(index); }
  public int size() { return elementTypes.size(); }
  public int getOffset(int index) { return offsets[index]; }
}
