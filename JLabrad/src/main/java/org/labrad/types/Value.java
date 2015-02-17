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

public final class Value extends Type {
  String units;

  /**
   * Factory to create a Value type in the specified units.
   * Note that units can be null to indicate no units.  This
   * is different from the empty string "", which indicates
   * a dimensionless quantity.
   * @param units
   * @return
   */
  public static Value of(String units) {
    return new Value(units);
  }

  private Value(String units) { this.units = units; }

  public boolean isFixedWidth() { return true; }
  public int dataWidth() { return 8; }

  public String getUnits() { return units; }

  public boolean matches(Type type) {
    return (type instanceof Any) ||
    (type instanceof Value &&
        (type.getUnits() == null || type.getUnits().equals(getUnits())));
  }

  public Type.Code getCode() { return Type.Code.VALUE; }
  public char getChar() { return 'v'; }

  public String toString() {
    return "v" + (units == null ? "" : "[" + units + "]");
  }

  public String pretty() {
    return "value" + (units == null ? "" : "[" + units + "]");
  }
}
