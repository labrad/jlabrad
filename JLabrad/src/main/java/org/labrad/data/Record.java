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

package org.labrad.data;


public class Record {
  private String name = null;
  private long ID;
  private Data data;
  private boolean needsLookup = false;

  public Record(String name) {
    this(name, Data.EMPTY);
  }

  public Record(String name, Data data) {
    this.name = name;
    this.data = data;
    needsLookup = true;
  }

  public Record(long ID) {
    this(ID, Data.EMPTY);
  }

  public Record(long ID, Data data) {
    this.ID = ID;
    this.data = data;
  }

  public Data getData() { return data; }
  public String getName() { return name; }
  public long getID() { return ID; }
  public void setID(long ID) {
    this.ID = ID;
    this.needsLookup = false;
  }
  public boolean needsLookup() { return needsLookup; }

  public String toString() {
    if (name != null) {
      return "Record(" + name + ", " + data.pretty() + ")";
    }
    return "Record(" + ID + ", " + data.pretty() + ")";
  }
}
