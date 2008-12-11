/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad.events;

import java.util.EventListener;

/**
 *
 * @author maffoo
 */
public interface ConnectionListener extends EventListener {
    public void connected(ConnectionEvent evt);
    public void disconnected(ConnectionEvent evt);
}
