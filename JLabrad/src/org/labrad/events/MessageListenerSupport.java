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

import org.labrad.data.Packet;

/**
 *
 * @author Matthew Neeley
 */
public class MessageListenerSupport extends ListenerSupport<MessageListener> {
    public MessageListenerSupport(Object source) { super(source); }

    public void fireMessage(Packet packet) {
        for (MessageListener listener : listeners) {
            listener.messageReceived(new MessageEvent(source, packet));
        }
    }
}
