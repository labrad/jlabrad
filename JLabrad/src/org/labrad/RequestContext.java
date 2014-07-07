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

package org.labrad;

import org.labrad.data.Context;
import org.labrad.data.Data;

/**
 *
 * @author maffoo
 */
public class RequestContext<T> {

  private Context context;
  private long source;
  private T data;

  public RequestContext(Context context, long source, T data) {
    this.context = context;
    this.source = source;
    this.data = data;
  }

  public long getSource() {
    return source;
  }

  public Context getContext() {
    return context;
  }

  public T getData() {
    return data;
  }

  public void finish(Data data) {

  }
}
