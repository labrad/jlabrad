/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

import java.util.EventListener;

/**
 *
 * @author maffoo
 */
interface ConnectionListener extends EventListener {
    void connected(ConnectionEvent evt);
    void disconnected(ConnectionEvent evt);
}
