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

public final class Str extends Type {
  private static Str instance = new Str();

  // private constructor to prevent instantiation
  private Str() {}

  public static Str getInstance() { return instance; }

  public boolean matches(Type type) {
    return (type instanceof Any) || (type instanceof Str);
  }

  public Type.Code getCode() { return Type.Code.STR; }
  public char getChar() { return 's'; }

  public boolean isFixedWidth() { return false; }
  public int dataWidth() { return 4; }

  public String toString() { return "s"; }
  public String pretty() { return "string"; }
}
