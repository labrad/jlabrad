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

  public void addListener(T listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  public void removeListener(T listener) {
    if (listeners.contains(listener)) {
      listeners.remove(listener);
    }
  }
}
