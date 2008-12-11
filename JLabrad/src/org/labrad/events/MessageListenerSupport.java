/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad.events;

import org.labrad.events.ListenerSupport;
import java.util.ArrayList;
import java.util.List;
import org.labrad.data.Packet;

/**
 *
 * @author maffoo
 */
public class MessageListenerSupport extends ListenerSupport<MessageListener> {
    public MessageListenerSupport(Object source) { super(source); }

    public void fireMessage(Packet packet) {
        for (MessageListener listener : listeners) {
            listener.messageReceived(new MessageEvent(source, packet));
        }
    }
}
