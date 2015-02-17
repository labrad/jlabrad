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

public final class Error extends Type {
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

  public boolean matches(Type type) {
    // TODO also check payload type here
    return (type instanceof Any) || (type instanceof Error);
  }

  public Type.Code getCode() { return Type.Code.ERROR; }
  public char getChar() { return 'E'; }

  public boolean isFixedWidth() { return false; }
  public int dataWidth() { return 4 + 4 + payload.dataWidth(); }

  public String toString() { return "E" + payload.toString(); }

  public String pretty() { return "error(" + payload.pretty() + ")"; }
  public Type getSubtype(int i) { return payload; }
}
