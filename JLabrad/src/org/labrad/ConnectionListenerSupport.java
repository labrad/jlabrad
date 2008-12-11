/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

/**
 *
 * @author maffoo
 */
public class ConnectionListenerSupport extends ListenerSupport<ConnectionListener> {
    public ConnectionListenerSupport(Object source) {
        super(source);
    }
    void fireConnected() {
        for (ConnectionListener listener : listeners) {
            listener.connected(new ConnectionEvent(source));
        }
    }
    void fireDisconnected() {
        for (ConnectionListener listener : listeners) {
            listener.disconnected(new ConnectionEvent(source));
        }
    }

}
