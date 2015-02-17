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

package org.labrad.events;

import java.util.EventObject;

import org.labrad.data.Context;
import org.labrad.data.Data;

/**
 *
 * @author Matthew Neeley
 */
public class MessageEvent extends EventObject {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private Context context;
  private long srcID, msgID;
  private Data data;

  public MessageEvent(Object source, Context context, long srcID, long msgID, Data data) {
    super(source);
    this.context = context;
    this.srcID = srcID;
    this.msgID = msgID;
    this.data = data;
  }

  public Context getContext() {
    return context;
  }

  public long getSourceID() {
    return srcID;
  }

  public long getMessageID() {
    return msgID;
  }

  public Data getData() {
    return data;
  }
}
