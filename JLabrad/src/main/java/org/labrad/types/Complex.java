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

public final class Complex extends Type {
  String units;

  /**
   * Factory to create a complex types with the specified units.
   * Note that units can be null, indicating no units.  This is
   * different from the empty string "", which means a dimensionless
   * quantity.
   */
  public static Complex of(String units) {
    return new Complex(units);
  }

  private Complex(String units) { this.units = units; }

  public boolean isFixedWidth() { return true; }

  public int dataWidth() { return 16; }

  public String getUnits() { return units; }

  public boolean matches(Type type) {
    return (type instanceof Any) ||
    (type instanceof Complex &&
        (type.getUnits() == null || type.getUnits().equals(getUnits())));
  }

  public Type.Code getCode() { return Type.Code.COMPLEX; }
  public char getChar() { return 'c'; }

  public String toString() {
    return "c" + (units == null ? "" : "[" + units + "]");
  }

  public String pretty() {
    return "complex" + (units == null ? "" : "[" + units + "]");
  }
}
