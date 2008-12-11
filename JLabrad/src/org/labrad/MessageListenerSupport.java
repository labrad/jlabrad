/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

import java.util.ArrayList;
import java.util.List;
import org.labrad.data.Packet;

/**
 *
 * @author maffoo
 */
public class MessageListenerSupport extends ListenerSupport<MessageListener> {
    public MessageListenerSupport(Object source) { super(source); }

    void fireMessage(Packet packet) {
        for (MessageListener listener : listeners) {
            listener.messageReceived(new MessageEvent(source, packet));
        }
    }
}
