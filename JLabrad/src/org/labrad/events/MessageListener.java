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
public interface MessageListener extends EventListener {
    public void messageReceived(MessageEvent e);
}
