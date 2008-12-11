/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * Supporting class for keeping track of a list of EventListeners.
 * @author maffoo
 */
public class ListenerSupport<T extends EventListener> {
    List<T> listeners = new ArrayList<T>();
    Object source;

    public ListenerSupport(Object source) {
        this.source = source;
    }

    void addListener(T listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    void removeListener(T listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }
}
