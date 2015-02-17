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

import java.io.Serializable;

public class Context implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private final long high, low;

  public Context(long high, long low) {
    this.high = high;
    this.low = low;
  }

  public long getHigh() { return high; }
  public long getLow() { return low; }

  public String toString() {
    return "(" + high + "," + low + ")";
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Context))
      return false;
    Context other = (Context) obj;
    if (high != other.high)
      return false;
    if (low != other.low)
      return false;
    return true;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (high ^ (high >>> 32));
    result = prime * result + (int) (low ^ (low >>> 32));
    return result;
  }
}
