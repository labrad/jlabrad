/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

import java.util.EventObject;
import org.labrad.data.Packet;

/**
 *
 * @author maffoo
 */
public class MessageEvent extends EventObject {
    private Packet message;

    public MessageEvent(Object source, Packet message) {
        super(source);
        this.message = message;
    }

    public Packet getMessage() {
        return message;
    }
}
