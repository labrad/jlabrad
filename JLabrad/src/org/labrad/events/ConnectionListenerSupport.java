/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad.events;

import org.labrad.*;
import org.labrad.events.ConnectionEvent;

/**
 *
 * @author maffoo
 */
public class ConnectionListenerSupport extends ListenerSupport<ConnectionListener> {
    public ConnectionListenerSupport(Object source) {
        super(source);
    }
    public void fireConnected() {
        for (ConnectionListener listener : listeners) {
            listener.connected(new ConnectionEvent(source));
        }
    }
    public void fireDisconnected() {
        for (ConnectionListener listener : listeners) {
            listener.disconnected(new ConnectionEvent(source));
        }
    }

}
