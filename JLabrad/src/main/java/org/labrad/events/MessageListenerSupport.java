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

import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Packet;
import org.labrad.data.Record;

/**
 *
 * @author Matthew Neeley
 */
public class MessageListenerSupport extends ListenerSupport<MessageListener> {
  public MessageListenerSupport(Object source) { super(source); }

  public void fireMessage(Packet packet) {
    Context ctx = packet.getContext();
    long srcID = packet.getTarget();
    for (Record r : packet.getRecords()) {
      long msgID = r.getID();
      Data data = r.getData();
      for (MessageListener listener : listeners) {
        MessageEvent evt = new MessageEvent(source, ctx, srcID, msgID, data);
        listener.messageReceived(evt);
      }
    }
  }
}
